#!/usr/bin/env python3
"""Reorganize OOP and DOD dashboards to canonical comparison layout. Run from repo root."""
import json
import os
import copy

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))

# Canonical layout: id -> (title, gridPos)
LAYOUT = {
    1: ("API Throughput", {"x": 0, "y": 0, "w": 12, "h": 8}),
    2: ("API Latency", {"x": 12, "y": 0, "w": 12, "h": 8}),
    3: ("HTTP Status Codes", {"x": 0, "y": 8, "w": 12, "h": 8}),
    4: ("Total request latency", {"x": 12, "y": 8, "w": 12, "h": 8}),
    5: ("Garbage Collection", {"x": 0, "y": 16, "w": 12, "h": 8}),
    6: ("Memory allocation", {"x": 12, "y": 16, "w": 12, "h": 8}),
    7: ("Request wait time", {"x": 0, "y": 24, "w": 24, "h": 8}),
    8: ("DB / EF query duration", {"x": 0, "y": 32, "w": 12, "h": 8}),
    9: ("DB / EF query count", {"x": 12, "y": 32, "w": 12, "h": 8}),
    10: ("EF Core change tracker", {"x": 0, "y": 40, "w": 12, "h": 8}),
    11: ("Memory per operation", {"x": 12, "y": 40, "w": 12, "h": 8}),
}

def normalize_datasource(panel):
    """Set datasource to Prometheus in panel and targets."""
    for key in ("datasource",):
        if key in panel and isinstance(panel[key], dict):
            panel[key]["uid"] = "Prometheus"
            panel[key]["type"] = "prometheus"
    for t in panel.get("targets", []):
        if isinstance(t.get("datasource"), dict):
            t["datasource"]["uid"] = "Prometheus"
            t["datasource"]["type"] = "prometheus"
    return panel

def transform_oop(path):
    with open(path, "r", encoding="utf-8-sig") as f:
        dash = json.load(f)
    panels_by_id = {p["id"]: p for p in dash["panels"] if "id" in p}
    # Keep: 1, 2, 8, 15, 6, 5, 30, 9, 10, 13, 14 -> slots 1..11. Remove: 3, 4, 7, 11, 12, 16
    old_to_slot = {
        1: 1, 2: 2, 8: 3, 15: 4, 6: 5, 5: 6, 30: 7, 9: 8, 10: 9, 13: 10, 14: 11,
    }
    new_panels = []
    for old_id, slot in sorted(old_to_slot.items(), key=lambda x: x[1]):
        if old_id not in panels_by_id:
            continue
        p = copy.deepcopy(panels_by_id[old_id])
        title, pos = LAYOUT[slot]
        p["id"] = slot
        p["title"] = title
        p["gridPos"] = pos
        new_panels.append(normalize_datasource(p))
    dash["panels"] = new_panels
    with open(path, "w", encoding="utf-8") as f:
        json.dump(dash, f, indent=2)
    print(f"OOP: {path} -> {len(new_panels)} panels")

def make_text_panel(panel_id, title, grid_pos):
    return {
        "id": panel_id,
        "title": title,
        "type": "text",
        "gridPos": grid_pos,
        "options": {"content": "N/A (DOD does not expose this metric)", "mode": "markdown"},
        "datasource": {"type": "prometheus", "uid": "Prometheus"},
    }

def make_http_status_panel_dod():
    """HTTP Status Codes panel for DOD (http_requests_total_dod by status)."""
    return {
        "id": 3,
        "title": "HTTP Status Codes",
        "type": "timeseries",
        "gridPos": {"x": 0, "y": 8, "w": 12, "h": 8},
        "datasource": {"type": "prometheus", "uid": "Prometheus"},
        "fieldConfig": {
            "defaults": {
                "color": {"mode": "palette-classic"},
                "custom": {
                    "axisCenteredZero": False,
                    "axisColorMode": "text",
                    "axisLabel": "Requests/Second",
                    "axisPlacement": "auto",
                    "drawStyle": "line",
                    "fillOpacity": 20,
                    "lineInterpolation": "smooth",
                    "lineWidth": 2,
                    "showPoints": "never",
                    "spanNulls": True,
                    "stacking": {"group": "A", "mode": "none"},
                    "thresholdsStyle": {"mode": "off"},
                },
                "unit": "reqps",
            },
            "overrides": [],
        },
        "options": {
            "legend": {"calcs": ["mean", "max"], "displayMode": "table", "placement": "bottom", "showLegend": True},
            "tooltip": {"mode": "multi", "sort": "desc"},
        },
        "targets": [{
            "datasource": {"type": "prometheus", "uid": "Prometheus"},
            "expr": "sum(rate(http_requests_total_dod{job=\"comic-api-dod\"}[1m])) by (status)",
            "legendFormat": "Status {{status}}",
            "refId": "A",
        }],
    }

def transform_dod(path):
    with open(path, "r", encoding="utf-8-sig") as f:
        dash = json.load(f)
    # Map by title substring to find panels
    title_to_panel = {}
    impl_panels = []
    for p in dash["panels"]:
        t = p.get("title", "")
        if "Requests Processed in Every Batch" in t:
            impl_panels.append(("batch", p))
        elif "DOD API Latency" in t:
            title_to_panel["latency"] = p
        elif "DOD API Throughput" in t:
            title_to_panel["throughput"] = p
        elif "DOD API Garbage" in t:
            title_to_panel["gc"] = p
        elif "DOD API Memory Allocation" in t:
            title_to_panel["memory"] = p
        elif "Thread Pool" in t:
            impl_panels.append(("thread", p))
        elif "Computation Status Rate" in t:
            impl_panels.append(("comp_status", p))
        elif "Computation Latencies" in t:
            impl_panels.append(("comp_lat", p))
        elif "Request Wait Time" in t:
            title_to_panel["wait"] = p
        elif "EF Core Query Duration" in t:
            title_to_panel["db_duration"] = p
        elif "EF Core Query Count" in t:
            title_to_panel["db_count"] = p
        elif "Database Query Duration" in t:
            pass  # skip duplicate
        elif "Database Query Count" in t:
            pass  # we use EF Core Query Count
        elif "Change Tracker" in t:
            title_to_panel["tracker"] = p
        elif "Memory Allocation Per Operation" in t:
            title_to_panel["mem_op"] = p
    # Build canonical order: 1 Throughput, 2 Latency, 3 HTTP Status (new), 4 N/A (new), 5 GC, 6 Memory, 7 Wait, 8 DB duration, 9 DB count, 10 Tracker, 11 Mem op, then impl
    mapping = [
        (1, "throughput"), (2, "latency"), (5, "gc"), (6, "memory"), (7, "wait"),
        (8, "db_duration"), (9, "db_count"), (10, "tracker"), (11, "mem_op"),
    ]
    new_panels = []
    for slot, key in mapping:
        if key not in title_to_panel:
            continue
        p = copy.deepcopy(title_to_panel[key])
        title, pos = LAYOUT[slot]
        p["id"] = slot
        p["title"] = title
        p["gridPos"] = pos
        new_panels.append(normalize_datasource(p))
    # Insert HTTP Status (3) and N/A text (4) at correct positions
    new_panels.insert(2, make_http_status_panel_dod())
    new_panels.insert(3, make_text_panel(4, "Total request latency", {"x": 12, "y": 8, "w": 12, "h": 8}))
    # Sort by id to get 1,2,3,4,5,6,7,8,9,10,11
    new_panels.sort(key=lambda p: p["id"])
    # Implementation-specific at y=48
    y_impl = 48
    for i, (_, p) in enumerate(impl_panels):
        p = copy.deepcopy(p)
        p["id"] = 12 + i
        row = i // 2
        col = i % 2
        p["gridPos"] = {"x": col * 12, "y": y_impl + row * 8, "w": 12, "h": 8}
        new_panels.append(normalize_datasource(p))
    dash["panels"] = new_panels
    with open(path, "w", encoding="utf-8") as f:
        json.dump(dash, f, indent=2)
    print(f"DOD: {path} -> {len(new_panels)} panels")

def main():
    oop_path = os.path.join(SCRIPT_DIR, "comic-api-performance.json")
    dod_path = os.path.join(SCRIPT_DIR, "comic-api-dod-performance.json")
    transform_oop(oop_path)
    transform_dod(dod_path)

if __name__ == "__main__":
    main()
