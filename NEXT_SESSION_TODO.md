# Next session TODO

Context for whoever (Claude or human) picks this up: SmartPharma is deployed and working
(GitHub + Render, MySQL/backend/ml/frontend all live). The seasonal PCRS-based forecasting
redesign from the previous session is DONE and deployed (`app/pcrs.py`, `app/model.py`,
`data/pcrs_national_trend.csv` with 8 years of real HSE PCRS data, 2016-2023).

This file lists three concrete, unaddressed issues raised in review feedback on 2026-07-28.
Read each section, then just start implementing — no need to re-plan from scratch.

---

## 1. Stock quantity has no validation (caused a real production bug)

**Symptom:** production `products.current_quantity` was found corrupted — `10000045` for
Paracetamol, `-8` for Amoxicillin, `-5082` for Esomeprazole, `-40` for Bisoprolol. Patched
manually via SQL, but the root cause (no server-side guardrail) is still there.

**Where:** `backend-java/src/main/java/com/smartpharma/service/ProductService.java`,
method that applies `quantityChange` to `current_quantity` (used by stock in/out endpoints).
Currently just does `newQuantity = currentQuantity + change` with no bounds check.

**Fix:**
- Reject the update (throw a validation exception -> 400) if `newQuantity < 0`.
- Add a sane upper bound check (e.g. reject absurd single-transaction changes, or cap
  `current_quantity` at some large-but-realistic ceiling) to prevent a repeat of the
  `10000045` case.
- Check whether `reason` (SALE/RESTOCK/EXPIRED) should constrain the sign of `quantityChange`
  (SALE and EXPIRED should always decrease stock, RESTOCK should always increase it) — if that
  constraint doesn't exist yet, add it too, it would have caught this bug directly.
- Add a unit test for: negative-result rejection, and reason/sign mismatch rejection.

---

## 2. Forecast confidence score sits at ~50% for almost everything

**Symptom (user's words):** "Confidence of the forecast is near 50% like a flipping a coin."

**Root cause:** `ml-service-python/app/model.py`, bottom of `forecast()`:

```python
r2 = model.score(day_index, quantities)
confidence = CONFIDENCE_FLOOR + max(0.0, min(1.0, r2)) * (CONFIDENCE_CEILING - CONFIDENCE_FLOOR)
```

`r2` is the R² of a straight line fit against **raw daily** sales counts. Daily counts are
Poisson noise on top of a weak trend, so R² is near zero (or negative) for almost every
product regardless of how good the underlying seasonal signal actually is. `max(0.0, ...)`
then floors it, so confidence lands on exactly 50 (`CONFIDENCE_FLOOR`) most of the time — it
looks like a placeholder because it effectively is one for most products.

**Fix direction (agreed in prior session, not yet implemented):** stop scoring R² against
raw daily points. Options, pick one and implement:
- Aggregate actuals and fitted values to weekly (or monthly) totals before computing R² —
  smooths out day-level Poisson noise while still reflecting real trend/seasonal fit quality.
- Or: compute R² against the **seasonally-adjusted** series (`actual / seasonal_multiplier`)
  instead of the raw trend line, so the metric reflects whether the trend+seasonal model
  together explains the data, not just the bare trend line.
- Whichever is chosen, recheck the `CONFIDENCE_FLOOR=50` / `CONFIDENCE_CEILING=95` clamp
  still makes sense once the underlying R² distribution actually spreads out — it may need
  different bounds once it's not artificially pinned at the floor.

**Verify:** after the fix, pull `/predict` for several different products locally and confirm
confidence scores actually vary (not all clustered at 50) and correlate with how much real
seasonal/trend signal each product's synthetic history has.

---

## 3. Analytics "Transaction Breakdown" only shows SALE

**Symptom (user's words):** "In the analytics tab, under transaction breakdown all three
should be present." (SALE / RESTOCK / EXPIRED)

**Root cause:** `ml-service-python/scripts/generate_data.py` only ever inserts
`reason='SALE'` rows (see `seed_product()`). RESTOCK has only a couple of manual test rows,
EXPIRED has essentially zero rows anywhere. `AnalyticsService.buildReasonBreakdown()`
(backend-java) is fine — it just returns whatever reasons exist, so it correctly shows an
almost-100%-SALE chart because that's genuinely all the data there is.

**Fix:** extend `generate_data.py`'s `seed_product()` (or add a sibling function) to also
synthesize:
- Periodic **RESTOCK** transactions — e.g. every N days (or when a running simulated stock
  level would drop below `min_threshold`), insert a positive `quantity_change` RESTOCK row
  bringing the product back up to a reasonable level. This should roughly balance against the
  SALE depletion so the simulated stock trajectory looks plausible.
- Occasional **EXPIRED** transactions — e.g. a small write-off near a product's
  `expiry_date`, or randomly on a low probability per product, as a negative `quantity_change`
  with `reason='EXPIRED'`.
- Keep it idempotent like the existing SALE logic (skip products that already have synthetic
  RESTOCK/EXPIRED history in the window) so re-running the script is still safe.

**Re-seed required after this change (both DBs), same procedure as last time:**
1. Local MySQL: `DELETE FROM inventory_transactions WHERE reason IN ('SALE','RESTOCK','EXPIRED')`,
   then `python -m scripts.generate_data` from `ml-service-python/`.
2. Production (Render): same `DELETE` via the `smartpharma-mysql` Web Shell, then re-run the
   seeding script via the `smartpharma-ml` Web Shell (`/usr/local/bin/python3 -m scripts.generate_data`
   worked last time — plain `python`/`python3` were unreliable in that shell).
3. No redeploy needed for this one unless `generate_data.py` itself changes in a way that
   affects the Docker image (it's a one-off script, not something the running service imports
   at request time) — but push the code change to GitHub either way so it's not local-only.

**Verify:** reload `/analytics` in the browser, confirm the Transaction Breakdown donut shows
all three categories with non-trivial (not 0.1%) slices for RESTOCK and EXPIRED.

---

## Suggested order

1 (validation) is small, self-contained, backend-only — good to do first.
2 (confidence) and 3 (breakdown data) are both in the ML service; 3 requires a re-seed of
both databases regardless, so if doing both, make the model.py change and the generate_data.py
change together, then re-seed once instead of twice.
