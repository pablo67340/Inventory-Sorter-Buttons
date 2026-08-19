#!/usr/bin/env python3
"""Upload every Inventory Sorter Buttons jar (root 1.19.2 + all ports) to
CurseForge as project files, with the right game version, modloader, Java
version and environment tags, plus the changelog.

Prerequisites:
  1. pip install -r requirements.txt          (just `requests`)
  2. An API token from https://authors.curseforge.com/account/api-tokens
     -> put it in the CURSEFORGE_TOKEN environment variable (or --token)
  3. Your project's numeric ID (shown in the "About Project" box on the
     project page) -> CURSEFORGE_PROJECT_ID environment variable (or
     --project-id)
  4. The jars collected in releases/<mod_version>/ (the build workflow
     already does this: invsortbuttons-<mc>-<mod_version>.jar for all 8)

Usage, from the project root:
  python scripts/upload_curseforge.py --project-id 123456
  python scripts/upload_curseforge.py --dry-run          # resolve + preview only
  python scripts/upload_curseforge.py --only 1.19.2,1.20.6
"""

import argparse
import json
import os
import re
import sys
from pathlib import Path

import requests

PROJECT_ROOT = Path(__file__).resolve().parent.parent
UPLOAD_API = "https://minecraft.curseforge.com/api"

# Every shipped version: Minecraft version tag on CurseForge + the Java the
# jar targets. All of them are Forge, client + server.
PORTS = [
    {"mc": "1.7.10",  "java": "Java 8"},
    {"mc": "1.12.2",  "java": "Java 8"},
    {"mc": "1.16.5",  "java": "Java 8"},
    {"mc": "1.19.2",  "java": "Java 17"},
    {"mc": "1.20.6",  "java": "Java 21"},
    {"mc": "1.21.11", "java": "Java 21"},
    {"mc": "26.1.2",  "java": "Java 25"},
    {"mc": "26.2",    "java": "Java 25"},
]

COMMON_TAGS = [("modloader", "Forge"),
               ("environment", "Client"),
               ("environment", "Server")]


def read_mod_version() -> str:
    for line in (PROJECT_ROOT / "gradle.properties").read_text().splitlines():
        if line.strip().startswith("mod_version="):
            return line.split("=", 1)[1].strip()
    sys.exit("mod_version not found in gradle.properties")


def fetch_version_ids(token: str) -> dict:
    """Map (category, display name) to numeric game-version IDs.

    The same display name can exist under several version types (e.g.
    "1.19.2" as a Minecraft version and under an addon category), and the
    upload API rejects IDs from the wrong type, so the type must be part of
    the lookup key.
    """
    headers = {"X-Api-Token": token}
    types_resp = requests.get(f"{UPLOAD_API}/game/version-types",
                              headers=headers, timeout=30)
    types_resp.raise_for_status()
    type_category = {}
    for vt in types_resp.json():
        name = vt["name"]
        # Minecraft version types are usually "Minecraft 1.19", but some are
        # bare version numbers like "26.2"
        if name.startswith("Minecraft") or re.fullmatch(r"\d+(\.\d+)*", name):
            type_category[vt["id"]] = "minecraft"
        elif name == "Java":
            type_category[vt["id"]] = "java"
        elif name in ("Modloader", "ModLoader"):
            type_category[vt["id"]] = "modloader"
        elif name == "Environment":
            type_category[vt["id"]] = "environment"

    resp = requests.get(f"{UPLOAD_API}/game/versions", headers=headers, timeout=30)
    resp.raise_for_status()
    ids = {}
    for entry in resp.json():
        category = type_category.get(entry["gameVersionTypeID"])
        if category is not None:
            ids[(category, entry["name"])] = entry["id"]
    return ids


def resolve_tags(tags: list, version_ids: dict) -> list:
    """tags is a list of (category, name) pairs."""
    resolved = []
    for category, name in tags:
        key = (category, name)
        if key not in version_ids:
            close = sorted(n for c, n in version_ids
                           if c == category and name.split()[0] in n)[:10]
            sys.exit(f"CurseForge has no {category} version named {name!r}.\n"
                     f"Closest available names in that category: {close}")
        resolved.append(version_ids[key])
    return resolved


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--project-id", default=os.environ.get("CURSEFORGE_PROJECT_ID"),
                        help="numeric CurseForge project id")
    parser.add_argument("--token", default=os.environ.get("CURSEFORGE_TOKEN"),
                        help="CurseForge author API token")
    parser.add_argument("--mod-version", default=None,
                        help="defaults to mod_version from gradle.properties")
    parser.add_argument("--changelog-file", default=str(PROJECT_ROOT / "CHANGELOG.md"))
    parser.add_argument("--release-type", default="release",
                        choices=["release", "beta", "alpha"])
    parser.add_argument("--only", default=None,
                        help="comma-separated MC versions to upload (default: all)")
    parser.add_argument("--dry-run", action="store_true",
                        help="resolve everything and show what would be uploaded")
    args = parser.parse_args()

    if not args.token:
        sys.exit("No API token: set CURSEFORGE_TOKEN or pass --token")
    if not args.project_id and not args.dry_run:
        sys.exit("No project id: set CURSEFORGE_PROJECT_ID or pass --project-id")

    mod_version = args.mod_version or read_mod_version()
    changelog = Path(args.changelog_file).read_text(encoding="utf-8")
    only = set(args.only.split(",")) if args.only else None

    release_dir = PROJECT_ROOT / "releases" / mod_version
    ports = [p for p in PORTS if only is None or p["mc"] in only]

    # Verify all jars exist before touching the network
    for port in ports:
        port["jar"] = release_dir / f"invsortbuttons-{port['mc']}-{mod_version}.jar"
        if not port["jar"].is_file():
            sys.exit(f"Missing jar: {port['jar']}\n"
                     f"Build and collect the release jars first.")

    print(f"Resolving CurseForge game version IDs...")
    version_ids = fetch_version_ids(args.token)

    uploaded = []
    for port in ports:
        tags = [("minecraft", port["mc"]), ("java", port["java"])] + COMMON_TAGS
        game_versions = resolve_tags(tags, version_ids)
        metadata = {
            "changelog": changelog,
            "changelogType": "markdown",
            "displayName": f"Inventory Sorter Buttons {mod_version} (MC {port['mc']})",
            "gameVersions": game_versions,
            "releaseType": args.release_type,
        }
        print(f"\n{port['jar'].name}")
        print(f"  tags: {', '.join(n for _, n in tags)}  ->  ids {game_versions}")
        if args.dry_run:
            continue

        with open(port["jar"], "rb") as fh:
            resp = requests.post(
                f"{UPLOAD_API}/projects/{args.project_id}/upload-file",
                headers={"X-Api-Token": args.token},
                data={"metadata": json.dumps(metadata)},
                files={"file": (port["jar"].name, fh, "application/java-archive")},
                timeout=300,
            )
        if resp.status_code != 200:
            sys.exit(f"  UPLOAD FAILED ({resp.status_code}): {resp.text}")
        file_id = resp.json()["id"]
        uploaded.append((port["mc"], file_id))
        print(f"  uploaded -> file id {file_id}")

    if args.dry_run:
        print("\nDry run complete - nothing uploaded.")
    else:
        print(f"\nDone: {len(uploaded)} files uploaded. "
              f"They'll appear once CurseForge approval finishes.")


if __name__ == "__main__":
    main()
