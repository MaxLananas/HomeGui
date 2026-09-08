<div align="center">
  <img src="src/main/resources/assets/homegui/icon.png" width="128" alt="HomeGui icon">

# HomeGui

A lightweight, client-side interface for command-based Minecraft server homes.

[Modrinth](https://modrinth.com/mod/homegui) · [Issues](https://github.com/MaxLananas/HomeGui/issues)
</div>

HomeGui does not create or store server homes. Press **H** and it sends `/homes`; it converts a compatible server response into a searchable interface. Selecting a home sends the same `/home <name>` command a player would type.

## Features

- Searchable list and grid views
- Favorites and favorite-first sorting
- Recent teleport history and local usage statistics
- Alphabetical, recent, and most-used sorting
- Approximate destination coordinates captured after a teleport command
- English, French, and additional built-in interface translations
- Import and export of local preferences and history
- No server installation, custom packets, telemetry, or background polling

## Compatibility

| HomeGui | Minecraft | Loader | Java | Status |
|---|---|---|---|---|
| 4.0.x | 1.21.11 | Fabric Loader 0.19.3+ | 21+ | Supported |

[Fabric API](https://modrinth.com/mod/fabric-api) is required. HomeGui is client-only; do not install it on a dedicated server.

Compatibility also depends on the server home plugin's chat format. The parser recognizes conventional `Homes: base, farm` and `Your homes: [base] [farm]` responses. Unknown formats are ignored rather than interpreted unsafely. Open an issue with a plain-text, privacy-safe sample if a format is missing.

NeoForge, Forge, Quilt, and broad `1.21.x` support are **not currently claimed**. See [the compatibility audit](docs/AUDIT.md#compatibility-strategy) for why untested loaders and patch versions are not listed.

## Installation and use

1. Install Fabric Loader for Minecraft 1.21.11.
2. Install the matching Fabric API.
3. Put the HomeGui release JAR in `.minecraft/mods`.
4. Join a server with `/homes` and `/home <name>` commands.
5. Press **H**. The binding can be changed under Minecraft's Controls screen.

Left-click teleports. The star control toggles a favorite. Search, sorting, layout, history, statistics, and settings are available from the main screen.

## Local data and migration

HomeGui writes `.minecraft/config/homegui.json`. It contains interface preferences, favorites, counts, history, and approximate coordinates—never authentication or server credentials.

Writes use a temporary file and atomic replacement where supported. Invalid fields fall back independently. If the root file cannot be read, HomeGui preserves it as `homegui.json.corrupt-<timestamp>` and creates safe defaults. Configurations from 3.x, primitive legacy history entries, and `homegui-v1` exports are migrated while loading; the current schema is version 2.

Use **Export** to create `homegui-export.json` in the config directory and **Import** to merge it. Imports are bounded and validated. Back up the config before moving data between machines.

## Development

Requirements: JDK 21 and network access to Fabric's Maven repository.

```bash
./gradlew check build
./scripts/validate-artifacts.sh
```

The release JAR is `build/libs/homegui-fabric-<version>.jar`. Loader-independent parsing and validation are in `core`; Minecraft/Fabric integration remains under `src/main`.

- [Technical audit and manual test plan](docs/AUDIT.md)
- [Current release notes](CHANGELOG.md)

## Release process

1. Update `mod_version` in `gradle.properties` and write `CHANGELOG.md` from the actual diff.
2. Open a pull request; the Build workflow runs tests, builds the remapped JAR, validates metadata and contents, and uploads the artifact.
3. Complete the in-game checks listed in the audit and merge to `main`.
4. Run **Release** manually with the exact version as confirmation.
5. The protected workflow rebuilds from a clean checkout, validates the JAR, creates a draft GitHub release, publishes that exact remapped JAR to Modrinth project `AyBYdiRl`, and only then publishes the GitHub release.

The release job reads `MODRINTH_API_KEY` only from GitHub Actions secrets. Never place the token in Gradle properties, repository files, workflow inputs, or logs. A missing token, existing tag, inconsistent version, failed test, malformed metadata, empty changelog, or invalid artifact stops publication.

## License

[Creative Commons Attribution-NonCommercial 4.0 International](LICENSE).
