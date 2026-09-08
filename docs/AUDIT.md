# Technical audit

## Existing behavior

HomeGui is a client-only visual layer over a server's command-based home plugin. `H` sends `/homes`, chat output is parsed into names, and clicking a name sends `/home <name>`. Local JSON stores favorites, display preferences, history, use counts, and approximate coordinates captured one second after a command. The UI also exposes search, list/grid layouts, sorting, history, statistics, export, and import.

No custom packet, server component, registry, world save, or external integration exists. Fabric API is used only for key registration and the end-client-tick event. Two narrowly scoped mixins observe received chat and outgoing commands.

## Findings and decisions

| Area | Finding | Decision |
|---|---|---|
| Home refresh | The screen copied the home list before the asynchronous reply and never observed later changes. | Revision-based refresh; preserve the last successful list while loading. |
| Parsing | Any bracketed chat could become homes; fallback parsing was over-broad. | Require a recognized home heading and validate every token. |
| Commands | Server-controlled names reached `sendCommand` without a trust-boundary check. | Central allowlist and complete-command parsing in loader-independent core code. |
| Statistics | History-screen teleports updated statistics directly and were then observed again by the outgoing-command mixin. | The outgoing command is the single tracking point. |
| Storage | Every stat operation rewrote the file, writes were non-atomic, malformed values were unbounded, and corruption deleted user data. | One transactional update, atomic replacement, bounded validation, and timestamped recovery copies. |
| Migration | Primitive history migration existed but had no format version. | Schema v2 plus idempotent readers for legacy config and v1 exports. |
| Localization | A large parallel-array translation table can silently drift. | Retained for compatibility in 4.0; missing entries safely fall back to English. Moving screen text to Minecraft language JSON is the next compatible cleanup. |
| GUI | Custom controls had narration but fixed dimensions and glyph-heavy labels. | Existing interaction model retained to avoid regressions; data refresh and destructive-state correctness fixed first. Visual/accessibility testing in a running client is required before claiming a redesign. |
| Coordinates | Position after one second is only an estimate and can be saved when teleport fails or is delayed. | Retained and documented as approximate; it is display-only. |
| Dependencies | Gson was redundantly declared even though Minecraft provides it. | Removed direct runtime dependency. |
| CI/release | CI overrode the wrapper, uploaded ambiguous JAR globs, release inputs could contradict metadata, Modrinth was not published, and webhook interpolation accepted untrusted text. | Wrapper-only build, artifact validator, guarded draft release, Mod Publish Plugin, and removal of the unsafe notification workflow. |
| Metadata | License and supported Minecraft version contradicted the repository and published artifact. | Corrected to CC-BY-NC-4.0 and exact 1.21.11. |

## Compatibility strategy

Version 4.0 deliberately supports **Fabric on Minecraft 1.21.11 only**. This is a tested compatibility statement, not a broad `1.21.x` claim. Minecraft patch releases in the 1.21 line changed input and widget signatures, so one binary cannot honestly cover all of them merely by loosening metadata.

The parser, command validation, and associated tests now live in `core` without Minecraft or loader APIs. Loader-independent extraction is the prerequisite for native NeoForge support. NeoForge and a maintained legacy line (most reasonably Fabric/Forge 1.20.1) should be separate artifacts and CI jobs; they must not be advertised until compilation and in-game smoke tests cover key registration, both mixins, chat parsing, screen interaction, and teleport dispatch. Quilt can generally load the Fabric artifact, but is not declared because it has not been independently tested. Forge on current versions has lower value than NeoForge; adding it would multiply GUI-version ports without improving the core behavior.

## Remaining manual verification

Automated tests cover untrusted text and command boundaries. Before publishing, a maintainer must run the client against representative EssentialsX and CMI servers and verify formatted list parsing, an empty list, timeout behavior, favorite toggling, list/grid scrolling, search input, all settings, import/export, history, coordinate display, keyboard rebinding, narration, and common GUI scales. The release workflow intentionally requires the protected `release` environment so this gate can be enforced.
