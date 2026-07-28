import csv
from collections import defaultdict
from pathlib import Path

DATA_DIR = Path(__file__).resolve().parent.parent / "data"
PCRS_CSV = DATA_DIR / "pcrs_national_trend.csv"


def _read_rows():
    if not PCRS_CSV.exists():
        return []
    with open(PCRS_CSV, newline="", encoding="utf-8") as f:
        data_lines = (line for line in f if not line.startswith("#"))
        return list(csv.DictReader(data_lines))


def load_pcrs_data():
    """
    Derives (demand_weights, seasonal_index) from real HSE PCRS national
    prescribing-frequency data (data/pcrs_national_trend.csv).

    demand_weights: {drug_name_lower: relative weight} - mean frequency for
    that drug across every collected month, relative to the overall average
    across all drugs. Scales how fast a product moves relative to the others.

    seasonal_index: {drug_name_lower: {month(1-12): multiplier}} - for each
    year we have data, the drug's value in a given month divided by that
    drug's own average across the months present that year, then averaged
    across all available years (2016-2023). Comparing "May to that same
    year's own average" first, rather than raw multi-year totals, keeps a
    generally-busier year (e.g. more prescribing overall by 2023 than 2016)
    from skewing which months look seasonal.

    A drug/month with no PCRS coverage is simply absent from seasonal_index -
    callers should default missing lookups to 1.0 (no adjustment), not guess.
    """
    rows = _read_rows()
    if not rows:
        return {}, {}

    by_drug = defaultdict(lambda: defaultdict(dict))  # drug -> year -> month -> freq
    all_freqs = []
    for row in rows:
        drug = row["product_name"].strip().lower()
        year_month = row["year_month"].strip()
        year, month = int(year_month[:4]), int(year_month[4:6])
        freq = int(row["prescribing_frequency"])
        by_drug[drug][year][month] = freq
        all_freqs.append(freq)

    overall_average = sum(all_freqs) / len(all_freqs)

    demand_weights = {}
    seasonal_index = {}
    for drug, years in by_drug.items():
        drug_freqs = [freq for months in years.values() for freq in months.values()]
        demand_weights[drug] = (sum(drug_freqs) / len(drug_freqs)) / overall_average

        ratios_by_month = defaultdict(list)
        for year_months in years.values():
            year_average = sum(year_months.values()) / len(year_months)
            for month, freq in year_months.items():
                ratios_by_month[month].append(freq / year_average)

        seasonal_index[drug] = {
            month: sum(ratios) / len(ratios) for month, ratios in ratios_by_month.items()
        }

    return demand_weights, seasonal_index
