#!/usr/bin/env python3
"""Fail when a screen file or navigation route is missing from the Phase 1 inventory."""

from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
UI_ROOT = ROOT / "app/src/main/java/org/dpdns/bnai/anime/ui"
NAV_GRAPH = UI_ROOT / "navigation/AppNavGraph.kt"
INVENTORY = ROOT / "docs/audit/ANDROID_PHASE_1_TEST_INVENTORY.md"


def main() -> int:
    inventory = INVENTORY.read_text(encoding="utf-8")
    screens = sorted(p.name for p in UI_ROOT.rglob("*Screen.kt"))
    nav_source = NAV_GRAPH.read_text(encoding="utf-8")
    routes = sorted(set(re.findall(r'composable\("([^"]+)"\)', nav_source)))

    missing_screens = [name for name in screens if f"`{name}`" not in inventory]
    missing_routes = [route for route in routes if f"`{route}`" not in inventory]

    print(f"screen files: {len(screens)}")
    print(f"unique routes: {len(routes)}")
    print(f"inventory file: {INVENTORY.relative_to(ROOT)}")

    if missing_screens:
        print("MISSING SCREEN INVENTORY ENTRIES:")
        for name in missing_screens:
            print(f"  - {name}")

    if missing_routes:
        print("MISSING ROUTE INVENTORY ENTRIES:")
        for route in missing_routes:
            print(f"  - {route}")

    if missing_screens or missing_routes:
        return 1

    print("OK: every screen file and unique route is present in the inventory.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
