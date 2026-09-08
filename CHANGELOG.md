# HomeGui 4.0.0

### Improved

- Home lists now update in an already-open screen as soon as the server response arrives.
- Configuration writes are atomic, deterministic, schema-versioned, and bounded to prevent malformed data from growing without limit.
- Invalid configuration files are preserved with a `.corrupt-<timestamp>` suffix before safe defaults are created.
- Legacy history entries and v1 exports remain importable.

### Fixed

- Teleports launched from the history screen are no longer counted twice.
- `/home` command tracking now requires a complete, valid command instead of accepting trailing input.
- Unrelated bracketed chat messages can no longer be mistaken for a home list.
- Home names received from chat are validated before being inserted into a command, closing a command-injection boundary.
- Timeout state is now propagated to the open interface.
- Metadata now consistently identifies Minecraft 1.21.11 and the repository's CC BY-NC 4.0 license.

### Changed

- Export format `homegui-v2` uses the same validated data model as the main configuration. Existing `homegui-v1` exports are migrated during import.
- Corrupt data is handled per field where possible instead of discarding every valid preference.

### Performance

- Home snapshots are immutable and no longer copied on every render.
- Recent sorting uses a rank map instead of repeated linear scans.
- Teleport statistics and history are persisted in a single write.

### Compatibility

- Fabric support is updated and verified for Minecraft 1.21.11, Fabric Loader 0.19.3, Fabric API 0.141.6, and Java 21.
