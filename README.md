<div align="center">
  <img src="shared/src/main/resources/assets/homegui/icon.png" width="128" alt="HomeGui icon">

# HomeGui

A lightweight, client-side interface for command-based Minecraft server homes.

[Modrinth](https://modrinth.com/mod/homegui) · [Issues](https://github.com/MaxLananas/HomeGui/issues)
</div>

HomeGui does not create or store server homes. Press **H** and it sends `/homes`; it
converts a compatible server response into a searchable interface. Selecting a home
sends the same `/home <name>` command a player would type. Nothing is injected, and
nothing is sent that a player could not have typed.

## Features

- One screen with **Homes**, **History**, **Stats** and **Settings** views
- Searchable list and grid views, with a favourite filter
- Favourites, and sorting by server order, name, most used, most recent, or
  favourites first
- Loading, empty, timed-out, unrecognised-format and no-match states, each saying
  what happened and what to do about it
- Teleport statistics counted only after the teleport actually happened
- Approximate destination coordinates captured when a teleport is confirmed
- Confirmation before destructive actions; import and export that report their result
- 17 built-in languages in Minecraft's own lang format, so a resource pack can
  override any string; the language can follow the client or be pinned
- Full keyboard navigation, visible focus, and a screen-reader name on every control
- Four colour themes, a high-contrast one among them, plus compact/comfortable
  density and optional transparency
- Animations that can be turned off entirely
- No server installation, custom packets, telemetry, or background polling

## Compatibility

| HomeGui | Minecraft | Loader | Java | Status |
|---|---|---|---|---|
| 4.1.x | 1.21.11 | Fabric Loader 0.19.3+ | 21+ | Built and validated in CI |

[Fabric API](https://modrinth.com/mod/fabric-api) is required. HomeGui is client-only;
do not install it on a dedicated server.

**What "built and validated" means, precisely:** the artifact compiles, its metadata
matches its filename about loader, Minecraft and Java version, it contains no sources,
test classes or development output, and its 140 core and interface tests pass. It does
**not** mean the mod was run in a client — see
[the audit](docs/AUDIT.md) and the [smoke-test procedure](docs/SMOKE_TESTS.md).

NeoForge, Forge, Quilt, Minecraft 26.x and broad `1.21.x` support are **not claimed**.
Minecraft 1.16.5 is not supported at all: Java 8 and pre-`GuiGraphics` rendering would
make it a second codebase.

Server compatibility depends on the home plugin's chat format. The parser recognises
EssentialsX (`Homes: base, farm`, `Homes (3): base, farm, mine`,
`Your homes: [base] [farm]`, paginated replies), CMI (`base (world) 100,64,200`,
`Home: base | world | 100 64 200`) and empty-list phrasings in the built-in languages.
A reply it cannot read is reported as an unrecognised format rather than guessed at.
Open an issue with a plain-text, privacy-safe sample if a format is missing.

## Installation and use

1. Install Fabric Loader for Minecraft 1.21.11.
2. Install the matching Fabric API.
3. Put the HomeGui release JAR in `.minecraft/mods`.
4. Press **H** (rebindable under *Controls → HomeGui*).

| Key | Action |
|---|---|
| `H` | Open or close |
| `/` or `Ctrl+F` | Jump to the search field |
| `Tab` / `Shift+Tab` | Move between controls |
| `↑` `↓` (grid: `←` `→`) | Move through the list |
| `Enter` or `Space` | Activate the focused control |
| `F5` | Request the home list again |
| Right click a home | Toggle its favourite |
| `Esc` | Leave the current view, then close |

Local data lives in `.minecraft/config/homegui/homegui.json`. Use **Settings →
Export** to write `homegui-export.json`, and **Import** to read it back; both report
what they did.

## Building from source

```bash
./gradlew :core:check :ui:check      # 140 tests, no Minecraft needed
./gradlew :fabric-1.21.11:build      # the artifact
./scripts/validate-artifacts.sh      # name, metadata and contents checks
```

`core` and `ui` carry the parsing, validation, storage and interface logic and have no
Minecraft dependency, which is what makes them testable. `mc-common` and `mc-modern`
are the bridge, and `artifacts/<module>` assembles one release JAR per loader and
Minecraft pair.
