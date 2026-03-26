import json
import re
from pathlib import Path

TSV_PATH = Path(r"C:\temp\items.tsv")
JSON_PATH = Path(r"A:\MC bingo server\jamiebingo 1.21.11 development\src\main\resources\data\jamiebingo\bingo_items.json")

CATEGORY_FIXES = {
    "bunde": "Bundle",
}

def normalize_category(cat: str) -> str:
    if not cat:
        return cat
    key = cat.strip().lower()
    return CATEGORY_FIXES.get(key, cat.strip())

def parse_constraints(extra: str):
    """
    Returns a dict of boolean constraint flags inferred from the 'extra function' column.
    """
    flags = {
        "requiresHostileMobsEnabled": False,
        "requiresHostileMobsDisabled": False,
        "requiresDaylightCycleEnabled": False,
        "requiresDaylightCycleDisabled": False,
    }
    if not extra:
        return flags

    text = extra.lower()

    # Hostile mobs constraints
    if "hostile mobs" in text:
        if "cannot appear if" in text and "disabled" in text:
            flags["requiresHostileMobsEnabled"] = True
        if "can only appear if" in text and "disabled" in text:
            flags["requiresHostileMobsDisabled"] = True
        if "are off" in text:
            if "cannot appear if" in text:
                flags["requiresHostileMobsEnabled"] = True
            if "can only appear if" in text:
                flags["requiresHostileMobsDisabled"] = True

    # Daylight cycle constraints
    if "daylight cycle" in text:
        if "cannot appear if" in text and "disabled" in text:
            flags["requiresDaylightCycleEnabled"] = True
        if "can only appear if" in text and "disabled" in text:
            flags["requiresDaylightCycleDisabled"] = True

    # Handle combined phrasing like "cannot appear if hostile mobs are off and/or if daylight cycle is disabled"
    if "cannot appear if" in text and "hostile mobs" in text and "daylight cycle" in text:
        if "disabled" in text or "are off" in text:
            flags["requiresHostileMobsEnabled"] = True
            flags["requiresDaylightCycleEnabled"] = True

    # Resolve conflicts (prefer explicit "only appear if" over "cannot appear if")
    if flags["requiresHostileMobsEnabled"] and flags["requiresHostileMobsDisabled"]:
        # Prefer the more restrictive "only appear if disabled"
        flags["requiresHostileMobsEnabled"] = False
    if flags["requiresDaylightCycleEnabled"] and flags["requiresDaylightCycleDisabled"]:
        flags["requiresDaylightCycleEnabled"] = False

    return flags


def main():
    if not TSV_PATH.exists():
        raise SystemExit(f"Missing TSV: {TSV_PATH}")
    if not JSON_PATH.exists():
        raise SystemExit(f"Missing JSON: {JSON_PATH}")

    rows = []
    for line in TSV_PATH.read_text(encoding="utf-8").splitlines():
        parts = line.split("\t")
        if len(parts) < 6:
            continue
        _, name, item_id, cat, rar, extra = parts[:6]
        name = name.strip()
        item_id = item_id.strip()
        cat = normalize_category(cat)
        rar = rar.strip()
        extra = extra.strip()
        if not item_id:
            continue
        rows.append((name, item_id, cat, rar, extra))

    data = json.loads(JSON_PATH.read_text(encoding="utf-8"))
    items = data.get("items", [])
    by_id = {it.get("id"): it for it in items if "id" in it}

    added = 0
    updated = 0
    for name, item_id, cat, rar, extra in rows:
        it = by_id.get(item_id)
        if it is None:
            it = {
                "id": item_id,
                "name": name if name else item_id,
                "category": cat if cat else "Misc",
                "rarity": rar if rar else "Common",
                "enabled": True,
            }
            items.append(it)
            by_id[item_id] = it
            added += 1
        changed = False
        if name and it.get("name") != name:
            it["name"] = name
            changed = True
        if cat and it.get("category") != cat:
            it["category"] = cat
            changed = True
        if rar and it.get("rarity") != rar:
            it["rarity"] = rar
            changed = True

        flags = parse_constraints(extra)
        for k, v in flags.items():
            if v:
                if it.get(k) != v:
                    it[k] = v
                    changed = True

        if changed:
            updated += 1

    JSON_PATH.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"added={added} updated={updated} total={len(items)}")


if __name__ == "__main__":
    main()
