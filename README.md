# 2005Scape

Personal RuneLite historical challenge with a cutoff of **22 June 2005 inclusive**. Later items and content are restricted using bundled local data.

## Installation status

**Prepared for Plugin Hub review; not yet approved or installable from the Hub.** The intended installation is through Plugin Hub in your existing RuneLite. This repository builds a plugin-only JAR and does not distribute RuneLite. Downloading the JAR is not an installation method for ordinary RuneLite.

After approval: open your usual RuneLite, select Plugin Hub, search for **2005Scape**, install and enable it. See [installation status](site/installation.html). Public source and a reviewed Plugin Hub submission are required by [RuneLite's submission process](https://github.com/runelite/plugin-hub#submitting-a-plugin).

The project includes a reversible classic gameframe, item/menu restrictions, missed-loot accounting, Slayer advice and account-specific progression. A historical spell whitelist, quest dialogue guards, 26 activity interface guards, conservative modern-area bounds and 11,323 scoped modern NPC/object IDs limit later content. Movement, exits and ordinary inventory escape actions remain available. Later monsters remain visible; attempted attacks show a warning and are blocked.

## Your equipment checklist

Edit **`src/main/resources/goal-definitions.json`** to choose your BIS and full-dragon goals. Each goal has a unique `key`, display `name`, a `group`, and an `items` list of exact historical item names. Items within one goal are alternatives: obtaining any one completes it. Optional `itemIds` lists exact eligible IDs instead. GitHub builds regenerate the runtime checklist automatically and reject unknown or later items. Keep keys stable when renaming goals. Only witnessed new acquisitions count; starting possessions do not.

Plugin Hub's standard build uses the committed `historical-goals.json`. After changing your definitions, commit the regenerated file too. If editing entirely on GitHub, download the **generated-checklist** artifact from a successful build and upload that JSON before generating a submission manifest. The submission workflow checks this to prevent reviewing a stale checklist.

Progress requires an item gain plus a recognised source: eligible loot, a listed historical shop with payment, historical quest completion, or a production recipe with material losses and XP. Starting inventory/equipment and bank transfers form a baseline and never generate unlocks or source-review prompts. Only new witnessed receipts count.

With **Strict receipt sources** enabled, an unrecognised new receipt is held for review in the sidebar. Enter its historical source and choose **Confirm historical source**, or **Keep restricted**. This supports legitimate gathering, quest steps and methods the automatic recogniser cannot attribute. Confirmation is a manual assertion, not independent verification. Confirmed modern-source receipts cannot be approved through that review. Restricted receipts quarantine the entire item type until no units remain when the bank is open; identical units cannot be distinguished. Missed-loot totals reconcile offered loot and actual receipts so the same stack is not counted twice.

## Historical database

The searchable audit page is in **[site/index.html](site/index.html)**. Download and open it locally, or host the `site` folder as a static website. It includes source links, dates and explicit unverified records. Hosting is not automatically enabled by this repository.

The website homepage is `site/index.html`, with separate `site/style.css` and `site/app.js` files. The root `index.html` redirects to it if Pages is configured to serve the repository root, so the README is never used as the website landing page. GitHub's repository Code view will still display the README normally. The preferred Pages workflow publishes `site` directly. The homepage database shows the plugin decision for each exact ID; unverified entries clearly display **Blocked · unverified** under the default rules.

For GitHub Pages, select **Settings → Pages → Build and deployment → Source: GitHub Actions**, then run **Publish historical audit** from Actions. The entire `site` folder is published, including `index.html`, `installation.html` and `.nojekyll`. Expected address for this repository: `https://tkoh-v7.github.io/2005scape/`; deployment must succeed before that address is live. Public Pages is intended here. Personal review/setup notes and raw research downloads are excluded from the upload.

The runtime data is in `src/main/resources`. Imported metadata is a historical snapshot, extended with explicit scoped current IDs. Unverified items remain restricted by default. This is a best-effort personal challenge, ready for in-game testing: a plugin cannot restore historical maps, drop tables, combat balance or server-side task assignment, nor guarantee interception of every possible action. Modern NPC/object coverage and automatic acquisition recognition are partial; the review fallback closes unknown receipts without silently treating their sources as historical.

Documented replacements for historical items are allowed by `historical-replacements.json`. Their actual modern release dates are preserved; the audit marks them **Eligible (replacement)**. This includes modern crystal bow/shield forms and pure essence, which replaced members' rune essence in 2006. Modern upgrades, essences, altars and acquisition sources remain outside the challenge rules. A replacement does not authorize every item with a matching name.

## Development

Java 11 and Gradle 8.10.2; the default RuneLite dependency follows `latest.release`, as recommended by RuneLite. Run `python3 tools/generate_goals.py` after checklist edits. `gradle clean test jar dependencyInventory` builds the plugin and tests it. GitHub Actions provides the plugin JAR, generated checklist, test results and exact build/test dependency sources. RuneLite supplies runtime libraries; none are bundled in the plugin JAR.

The developer launcher is in `src/test/java`, excluded from the plugin JAR. `gradle run` is only a developer test harness; normal use is through Plugin Hub after approval. No automated login or account-credential handling is implemented.

## Plugin Hub submission

Upload the project contents to the repository root and make the source repository public. Complete in-game validation before submitting. Run **Prepare Plugin Hub submission** from Actions. It builds/tests the plugin, requires public source, and creates a manifest for the exact uploaded commit. It never changes visibility or submits a pull request. Download **2005scape-hub-submission**, place its `2005scape` manifest in `plugins/2005scape` in your fork of `runelite/plugin-hub`, and open a review pull request using the supplied description.

When replacing an earlier manual upload, delete the obsolete `launch/` folder and `src/main/java/com/tkoh/scape2005/DevelopmentLauncher.java` if present. The new developer entry point is under `src/test/java`. Manual uploads do not automatically remove files that disappeared from the project. Remove any previously uploaded personal documents too; they are not part of this package.

`build=standard` uses RuneLite's standard build and requires no extra plugin dependencies or runtime downloads. GPL-3.0 licensing and upstream attribution are retained in resources so they survive standard packaging. Restrictions on combat clicks, conditional menu filtering and UI/model hiding need reviewer assessment. Approval is not guaranteed; any requested changes must be addressed before the plugin can be installed normally.

## Attribution

Factual item/NPC metadata derives from [OSRSBox](https://github.com/osrsbox/osrsbox-db). Its license is in `third-party/OSRSBox-LICENSE.txt`; provenance and transformations are retained in the source. See [NOTICE.md](NOTICE.md). No extracted game sprites are distributed.

Plugin source is licensed under [GPL-3.0](LICENSE). RuneLite attribution uses its BSD 2-Clause license. Historical and current-ID evidence links are retained in the database and public audit.
