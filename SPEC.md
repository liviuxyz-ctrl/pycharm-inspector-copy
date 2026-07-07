# PyCharm Inspector Copy — Project Spec

## What this is

An IntelliJ Platform plugin (targets PyCharm, works in any JetBrains IDE) that copies
selected inspection errors from the IDE's Problems panel or Inspection Results tree as a
structured Markdown block, ready to paste into any LLM chat (Claude, ChatGPT, Copilot, etc.).

## Problem being solved

When you get an inspection error in PyCharm you currently have to:
1. Read the error in the panel
2. Navigate to the file
3. Manually copy the message + surrounding code
4. Paste and re-format it for the LLM

This plugin collapses that into one keypress.

## Output format (what gets copied)

```markdown
## Inspection Problems

### `maptara_codexa/models/contract.py:142` — Error
Cannot resolve symbol 'many2one_link'

\`\`\`python
 137:     _name = 'codexa.contract'
 138:     _inherit = ['mail.thread']
 139:
 140:     partner_id = fields.Many2one(
 141:         'res.partner',
>142:         many2one_link,
 143:     )
\`\`\`
```

Fields per entry: file path (relative to project root), line number, severity label,
inspection message, ±5 lines of code with the error line marked with `>`.

## Entry points

| Trigger | Where |
|---|---|
| `Ctrl+Alt+Shift+C` | anywhere, acts on focused tree |
| Right-click → **Copy for LLM** | Problems panel context menu |
| Right-click → **Copy for LLM** | Inspect Code results tree context menu |

## Two data source paths

### Path 1 — Problems panel (highlight-based)
- Tool window opened with `Alt+6`
- Shows current-file or project-wide highlighting errors
- Node type: `ProblemNode` wrapping a `HighlightingProblem`
- Line derived from `HighlightInfo.startOffset` → `Document.getLineNumber()`

### Path 2 — Inspect Code results tree (batch inspection run)
- Opened via Analyze → Inspect Code
- Node type: `ProblemDescriptionNode` (extends `InspectionTreeNode`)
- Line derived from `PsiElement.textOffset` → `PsiDocumentManager.getDocument()`

The action tries Path 1 first; falls back to Path 2 if nothing collected.

## Tech stack

| Thing | Choice |
|---|---|
| Language | Kotlin 2.0 |
| Build | Gradle + IntelliJ Platform Gradle Plugin v2 (`org.jetbrains.intellij.platform`) |
| Target IDE | PyCharm (`platformType = PY`) |
| Min build | 233 (2023.3) |
| Max build | 251.* (2025.1) |
| JDK | 21 |

## Repository layout

```
pycharm-inspector-copy/
├── build.gradle.kts
├── gradle.properties
├── settings.gradle.kts
├── SPEC.md                          ← this file
├── README.md
└── src/main/
    ├── kotlin/ai/capps/inspectorcopy/
    │   ├── CopyForLlmAction.kt      ← AnAction, wires both paths
    │   └── ProblemFormatter.kt      ← markdown formatter + snippet extractor
    └── resources/META-INF/
        └── plugin.xml               ← action registration, keyboard shortcut
```

## Current state (2026-07-07)

- Scaffold committed and pushed to `github.com/liviuxyz-ctrl/pycharm-inspector-copy`
- Both data-source paths are coded; **not yet tested against a live IDE sandbox**
- No Gradle wrapper committed yet (run `gradle wrapper` before first `./gradlew runIde`)

## Known risks / gotchas

- `ProblemNode` and `InspectionTreeNode` are IntelliJ internal APIs marked
  `@ApiStatus.Internal` — they can change on major IDE versions. Pin `untilBuild`
  conservatively and test on each new IDE release.
- The focused-tree fallback (`KeyboardFocusManager.focusOwner as? JTree`) is a
  best-effort heuristic; it breaks if focus moves before the action fires. A more
  robust approach is to register a `DataProvider` on the tool window panel itself.
- SSH push to `git@github.com:liviuxyz-ctrl/...` fails because the machine's SSH key
  is tied to the work GitHub account. Use HTTPS remote (`https://github.com/...`) for
  all pushes from this machine.

## Immediate next tasks

1. Run `gradle wrapper` to commit the Gradle wrapper scripts
2. `./gradlew runIde` — open the sandboxed PyCharm, open a project with inspection
   errors, trigger the action, verify the clipboard output
3. Fix any API compatibility issues found in step 2
4. Add a toolbar button to the Problems panel (no selection needed → copies all)
5. Make context line count configurable via IDE Settings page

## Roadmap (lower priority)

- Include inspection tool ID in the output (e.g. `PyUnresolvedReferencesInspection`)
- "Copy all" mode (entire panel, no selection)
- Option to include module `__manifest__.py` excerpt for Odoo-specific errors
- JetBrains Marketplace publish
