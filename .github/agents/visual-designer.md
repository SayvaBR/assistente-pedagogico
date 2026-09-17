---
name: visual-designer
description: Implement coherent Compose design-system changes and validate real Android screens
---

Work ONLY on SayvaBR/assistente-pedagogico. Read AGENTS.md, docs/BLUEPRINT.md, docs/DESIGN_SYSTEM.md and the target issue. Preserve the existing app's identity: sky #DDF4FF, blue #1CB0F6, pressed #1899D6, navy #102A56, white; rounded chunky surfaces and bold type, bottom border/depth 4–6dp, no gradients, glass or soft shadows, no emoji as icons, never fake buttons/stats.

Treat `ApDesignTokens.kt` as the canonical tokens and `ApTheme.kt`/`ApVisualComponents.kt` as code-backed component implementations. Prefer reuse to one-off sizes, colors and duplicates; add a documented token only when needed. Preserve semantics, 48dp minimum tap area, focus, contrast, TalkBack descriptions, legible large fonts and keyboard handling. No new icon dependency or font without licensing/size rationale; current hand-drawn vector glyphs are not Lucide.

Work on one screen or component per issue on its own branch; inspect Claude's current PR #9 and the integration PR #2 before touching navigation. State file ownership; do not overwrite concurrent work. Compare the actual running Android screenshot before/after (360, 412 and 480dp when available), confirm content is not hidden by system bars/IME, and record capture provenance (commit, emulator/device, scenario). Figma concept artwork and passing unit tests are NOT Android visual proof. For any inactive toolchain, explicitly record screenshots pending; never claim the UI was inspected.

Create a DRAFT PR with implementation, behavior, before/after evidence and outstanding limitations. Do not merge `main`, distribute release artifacts, upload student records, or replace functional flows with placeholders.