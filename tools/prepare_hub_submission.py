"""Generate a review manifest from a real uploaded commit; never submit/publish."""
import argparse
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

def prepare(repository, commit):
    if not re.fullmatch(r"https://github\.com/[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+(?:\.git)?", repository):
        raise ValueError("Use the public GitHub repository HTTPS URL")
    if not re.fullmatch(r"[0-9a-fA-F]{40}", commit) or len(set(commit.lower())) == 1:
        raise ValueError("Use the real full 40-character uploaded commit hash")
    repository = repository if repository.endswith(".git") else repository + ".git"
    return f"repository={repository}\ncommit={commit.lower()}\n"

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repository", default="https://github.com/tkoh-v7/2005scape.git")
    parser.add_argument("--commit", required=True)
    args = parser.parse_args()
    text = prepare(args.repository, args.commit)
    destination = ROOT / "build/hub-submission"
    destination.mkdir(parents=True, exist_ok=True)
    (destination / "2005scape").write_text(text, encoding="utf-8")
    (destination / "pull-request.md").write_text(
        "Add 2005Scape\n\nA personal historical challenge using a fixed 22 June 2005 cutoff and bundled local data. "
        "It provides classic presentation, missed-loot totals, source review and progression. "
        "No game client, network dependency or developer launcher is bundled in the plugin JAR.\n\n"
        "Review focus: conditional menu filtering and consumed combat/content clicks; hidden item models and spell/UI widgets; "
        "temporary Interface Styles settings; account-specific state; attribution of modern rewards. "
        "Please assess these restrictions against current policy; acceptance is not assumed. "
        "In-game verification is still pending and must be completed before submitting.\n\n"
        "Bundled factual data and plugin source are provided under GPL-3.0; RuneLite BSD attribution is retained. "
        "The audit HTML is in site/index.html.\n", encoding="utf-8")
    print("Prepared manifest and review description. Nothing was submitted or published.")
