# Changelog

Only changes that actually happened are listed here. Where a line says a behaviour is
covered by a test, that test runs in CI on every push; where it says a behaviour is
unverified, it has not been observed in a running client.

## 4.1.0

### Changed

- **New architecture.** The mod is no longer one module. `core` holds the domain
  (parsing, validation, request state, storage), `ui` holds the interface kernel
  (layout, focus, states, icons, translations), `mc-common`/`mc-modern` hold the
  Minecraft bridge, and each published artifact is its own module. `core` and `ui`
  are plain Java 17 with no Minecraft on the classpath, so 140 JUnit tests run
  without a client.
- **One screen instead of four.** Homes, History, Stats and Settings are views in a
  single screen with a tab bar, rather than four separate screens opened from each
  other.
- **Translations moved to Minecraft's own format.** The 17 built-in locales are now
  standard `assets/homegui/lang/*.json` files instead of a Java map, so a resource
  pack can override any string, and every locale carries all 87 keys.
- **Icons are drawn, not typed.** The fragile Unicode glyphs (including a surrogate
  pair medal) are replaced by 19 procedural 9×9 bitmaps rendered as fills.
- **Artifact naming.** Release files are now `homegui-<version>-<loader>-<minecraft>.jar`,
  for example `homegui-4.1.0-fabric-1.21.11.jar`.
- **No sources JAR is produced.** The release artifact is the single file a player
  installs.

### Fixed

- `HomeGui could not understand that` was parsed as a heading naming five homes
  ("HomeGui", "could", "not", "understand", "that"). A heading now requires a
  separator after the homes word.
- EssentialsX's counted heading `Homes (3): base, farm, mine` was read as a single
  home called "Homes" in a world called "3". A parenthesised number is a count, not
  a world name.
- Prose following a heading was split on whitespace into one-word homes, so
  `Homes: safe, ok_name, two words` produced phantom homes `two` and `words`. The
  inline body now splits on punctuation only.
- Replies spread over several chat messages were lost; only the first message was
  read. Parsing is now a session that accumulates lines until the reply is complete.
- Statistics were counted when `/home` was *sent*, so a refused or failed teleport
  still incremented the counters. They are now counted only after the player has
  actually moved at least 3 blocks within the confirmation window.
- Destination coordinates were sampled 20 ticks after the command regardless of
  outcome. They are captured from the player's position at confirmation.
- A name containing a control character was silently trimmed into a *different*
  name (`\u0000nul` became `nul`). It is now rejected.
- Server keys kept stray punctuation, so `../exa\u0000mple.net` produced the bucket
  `..example.net`. Repeated dots collapse and leading or trailing punctuation is
  dropped.
- History ignored the pre-server-scoped bucket that favourites and use counts
  already merged, so a migrated 4.0.x install showed an empty History tab, and
  "clear history" would not have cleared those entries either.
- Arrow-key navigation stopped at the last row that happened to be on screen, so
  homes below the fold were unreachable by keyboard.
- Typing in the search field did not refilter the list until the interface was
  rebuilt for another reason.
- A settings change that could not be written to disk was accepted silently. A
  failed write is now reported in the interface and to the narrator.
- `compactMode` and `themeIndex` were stored but changed nothing on screen.
- Export and import results were discarded. Both now report what happened,
  including a missing export file.
- `clearHistory()` ran without confirmation. Destructive actions now ask, and the
  confirmation can be turned off in Settings.
- Buttons narrated only the generic default text. Every control now carries its own
  accessible name, which is also what the screen reader announces.
- The search field was recreated on every keystroke and lost focus.
- Panel widths were fixed at 320/280/310 pixels and clipped on small windows or high
  GUI scale. The layout is now derived from the window size.

### Compatibility

- Built and validated for **Minecraft 1.21.11 on Fabric** (Java 21). This is the only
  target claimed in this release.
- Compiling against a Minecraft version is not the same as working in it. No
  in-game run was possible in the build environment, so screen opening, `/homes`
  capture and `/home` dispatch are **unverified** for every target, including this
  one. See `docs/SMOKE_TESTS.md` for the procedure and `docs/AUDIT.md` for what is
  and is not claimed.
- Minecraft 1.16.5 is not supported: it is Java 8 with pre-`GuiGraphics` rendering,
  which would be a second codebase rather than a third module.

## 4.0.0

- First release of the rewritten configuration storage: atomic, schema-versioned,
  bounded writes, corrupt files preserved as `.corrupt-<timestamp>`.
- Home names received from chat are validated before being placed in a command.
- Teleports launched from the history screen are no longer counted twice.
- Export format `homegui-v2`; `homegui-v1` exports are migrated on import.
