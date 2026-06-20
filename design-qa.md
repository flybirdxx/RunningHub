# Design QA

final result: passed

Date: 2026-06-19

Source visuals:
- `docs/superpowers/specs/assets/wireframe-01-creation-hub.png`
- `docs/superpowers/specs/assets/wireframe-02-plaza-first.png`
- `docs/superpowers/specs/assets/wireframe-03-history-console.png`
- `docs/superpowers/specs/assets/n4RET6.png`

Implementation screenshots:
- `output/redline_final_create.png`
- `output/redline_final_plaza.png`
- `output/redline_final_history.png`

Checks:
- Create now follows the reference single-column generation flow: brand/header, category chips, selected API model card, prompt, ratio, resolution, cost, and generate bar.
- Plaza now follows the reference dark feed: title actions, segmented sort, mode tabs, horizontal tag filters, two-column media grid, RH badges, and overlay metadata.
- History now follows the reference console: centered title, project selector, pinned projects, status tabs, notice bar, date grouping, timeline rows, status pills, cost, and action buttons.
- Bottom navigation is unified to the dark/lime reference palette.

Remaining P3 notes:
- Exact RunningHub logo asset and exact production icon set are not present in the repo, so local Material icons/text logo are used.
- Guest mode history uses local reference rows when the cloud API returns token errors, so the designed console state is visible without login.

## Creation Hub Scope Fix

final result: passed

Date: 2026-06-20

Source visuals:
- `docs/superpowers/specs/assets/wireframe-01-creation-hub.png`
- `C:/Users/ADMINI~1/AppData/Local/Temp/codex-clipboard-26dac605-0ce6-4540-a6eb-aef1d743091b.png`

Implementation screenshots:
- `output/create_design_fix.png`
- `output/create_design_fix_final_scrolled.png`

Checks:
- Removed visible search API model field and horizontal model selector chips from the Create tab.
- Replaced the API/PRO model card presentation with the reference single recommended `全能图片 V2` card.
- Reset visible defaults to empty prompt, `1:1`, and `1024 × 1024 (1:1)`.
- Recent task rows now use `全能图片 V2`, localized `成功`, and real output thumbnails when available.
- XML validation confirmed `搜索 API 模型`, `API 模型`, `全能图片PRO`, `PRO-`, `FAST_WEBAPP`, `SUCCESS`, and `resolution` are absent from visible states.
## Creation Hub History Drawer

final result: passed

Date: 2026-06-20

Source visual:
- Generated drawer concept from the current conversation.

Implementation screenshots:
- `output/create_history_drawer.png`
- `output/create_history_drawer_closed.png`

Checks:
- The Create header right action now opens a right-side `生成历史` drawer instead of showing a search action.
- The main Create page no longer renders the inline `最近任务` section.
- The drawer shows title, count, close action, filter chips, grouped generated-file rows, localized status pills, thumbnails, metadata, and `查看全部历史`.
- Closing the drawer removes drawer body content while leaving the header history icon available.
