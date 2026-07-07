# Inspector Copy for LLM

PyCharm / IntelliJ plugin — copies selected inspection errors from the **Problems panel** or **Inspection Results** tree as a structured Markdown block, ready to paste into any LLM chat.

## Output format

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

## Usage

1. Open **Problems** tool window (`Alt+6`) or run **Analyze → Inspect Code**
2. Select one or more error nodes in the tree
3. Right-click → **Copy for LLM**, or press `Ctrl+Alt+Shift+C`
4. Paste into ChatGPT / Claude / Copilot Chat

## Build & run locally

```bash
./gradlew runIde          # launches a sandboxed PyCharm with the plugin loaded
./gradlew buildPlugin     # produces build/distributions/*.zip
./gradlew verifyPlugin    # IntelliJ Plugin Verifier
```

Requires JDK 21+. The Gradle wrapper is included.

## Two entry points

| Panel | How it works |
|---|---|
| **Problems** (highlight-based) | Reads `HighlightingProblem` nodes from the current-file or project-wide tab |
| **Inspections Run** | Reads `ProblemDescriptionNode` nodes from the `Analyze → Inspect Code` result tree |

## Roadmap

- [ ] Include inspection name / tool ID in output
- [ ] "Copy all" button on the toolbar (no selection needed)
- [ ] Configurable context lines (default: ±5)
- [ ] Option: include module `__manifest__.py` excerpt for Odoo errors
- [ ] JetBrains Marketplace publish
