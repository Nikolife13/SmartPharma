# -*- coding: utf-8 -*-
"""
Unit tests for app/pcrs.py's load_pcrs_data() - verifies the demand-weight and
seasonal-index math against a small, hand-computed fixture CSV (not the real
1,363-row dataset, so the expected numbers can be checked by hand).
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

import pytest

import app.pcrs as pcrs

FIXTURE_CSV = """# fixture data for tests
product_name,year_month,prescribing_frequency
DrugA,202401,100
DrugA,202402,200
DrugA,202501,150
DrugA,202502,300
DrugB,202401,50
DrugB,202402,50
"""


def test_load_pcrs_data_with_no_file_returns_empty_dicts(tmp_path, monkeypatch):
    monkeypatch.setattr(pcrs, "PCRS_CSV", tmp_path / "does-not-exist.csv")

    weights, seasonal = pcrs.load_pcrs_data()

    assert weights == {}
    assert seasonal == {}


def test_demand_weight_is_relative_to_the_overall_average(tmp_path, monkeypatch):
    csv_path = tmp_path / "pcrs.csv"
    csv_path.write_text(FIXTURE_CSV, encoding="utf-8")
    monkeypatch.setattr(pcrs, "PCRS_CSV", csv_path)

    weights, _ = pcrs.load_pcrs_data()

    # overall_average = mean(100,200,150,300,50,50) = 141.666...
    # drugA mean = 187.5 -> weight = 187.5 / 141.666... = 1.3235...
    # drugB mean = 50    -> weight = 50 / 141.666...    = 0.3529...
    assert weights["druga"] == pytest.approx(1.3235, abs=0.001)
    assert weights["drugb"] == pytest.approx(0.3529, abs=0.001)


def test_seasonal_index_averages_within_year_ratios_across_years(tmp_path, monkeypatch):
    csv_path = tmp_path / "pcrs.csv"
    csv_path.write_text(FIXTURE_CSV, encoding="utf-8")
    monkeypatch.setattr(pcrs, "PCRS_CSV", csv_path)

    _, seasonal = pcrs.load_pcrs_data()

    # 2024: month1=100, month2=200, year_avg=150 -> ratios 0.667, 1.333
    # 2025: month1=150, month2=300, year_avg=225 -> ratios 0.667, 1.333 (same shape)
    # Averaged across both years: month1=0.667, month2=1.333
    assert seasonal["druga"][1] == pytest.approx(0.6667, abs=0.001)
    assert seasonal["druga"][2] == pytest.approx(1.3333, abs=0.001)


def test_seasonal_index_is_neutral_when_a_drug_has_no_real_seasonal_swing(tmp_path, monkeypatch):
    csv_path = tmp_path / "pcrs.csv"
    csv_path.write_text(FIXTURE_CSV, encoding="utf-8")
    monkeypatch.setattr(pcrs, "PCRS_CSV", csv_path)

    _, seasonal = pcrs.load_pcrs_data()

    # DrugB sells exactly the same amount every month it appears -> no seasonality.
    assert seasonal["drugb"][1] == pytest.approx(1.0, abs=0.0001)
    assert seasonal["drugb"][2] == pytest.approx(1.0, abs=0.0001)


def test_comment_lines_are_ignored_when_parsing_the_csv(tmp_path, monkeypatch):
    csv_path = tmp_path / "pcrs.csv"
    csv_path.write_text(FIXTURE_CSV, encoding="utf-8")
    monkeypatch.setattr(pcrs, "PCRS_CSV", csv_path)

    weights, _ = pcrs.load_pcrs_data()

    # If the leading "#" comment line had been treated as the header, this key
    # (or any real drug name) wouldn't be present at all.
    assert "druga" in weights
    assert "drugb" in weights


