#!/usr/bin/env python3
"""
Extracts release notes for a given version from CHANGELOG.md.
Used during automated builds and GitHub releases to ensure
proper, structured release notes accompany every release.
"""

import sys
import re
import os

def extract_notes(version_tag, changelog_path="CHANGELOG.md"):
    # Strip leading 'v' or 'V'
    clean_ver = version_tag.lstrip("vV").strip()
    
    if not os.path.exists(changelog_path):
        print(f"❌ Error: {changelog_path} not found!", file=sys.stderr)
        sys.exit(1)
        
    with open(changelog_path, "r", encoding="utf-8") as f:
        content = f.read()

    # Match section: ## [1.3.0.1] - YYYY-MM-DD up to the next ## [ or EOF
    pattern = rf"(?m)^##\s*\[{re.escape(clean_ver)}\]\s*-\s*(\d{{4}}-\d{{2}}-\d{{2}})?\s*\n([\s\S]*?)(?=\n##\s*\[|\Z)"
    match = re.search(pattern, content)
    
    if not match:
        print(f"❌ Error: No release notes found for version '{clean_ver}' in {changelog_path}!", file=sys.stderr)
        print(f"📝 Reminder: Please add a section '## [{clean_ver}] - YYYY-MM-DD' to {changelog_path} before building.", file=sys.stderr)
        sys.exit(1)
        
    raw_notes = match.group(2).strip()
    raw_notes = re.sub(r"\n*---\s*$", "", raw_notes).strip()
    
    if not raw_notes:
        print(f"❌ Error: Release notes for version '{clean_ver}' in {changelog_path} are empty!", file=sys.stderr)
        sys.exit(1)
        
    return raw_notes

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: extract_release_notes.py <version_tag> [changelog_path]")
        sys.exit(1)
        
    tag = sys.argv[1]
    ch_path = sys.argv[2] if len(sys.argv) > 2 else "CHANGELOG.md"
    notes = extract_notes(tag, ch_path)
    print(notes)
