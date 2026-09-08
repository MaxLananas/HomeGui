# In-game smoke test

This is the gate between "built and validated in CI" and "tested in game". It cannot
be automated: it needs a real client, a real display and a real server. Run it against
every target before that target is published, and record the result at the bottom.

Budget about fifteen minutes per target. Every step has an expected result; a step
that merely does not crash is a failure if the expected result did not appear.

## Preparation

1. A clean instance: an empty `.minecraft` (or a Prism/MultiMC instance) with only
   the loader, the loader API, and the HomeGui JAR under test.
2. A server, or a LAN world, with a home plugin installed. EssentialsX or CMI. If
   neither is available, a vanilla server with a command-block or datapack alias that
   prints `Homes: base, farm` in response to `/homes` is enough for steps 5–9.
3. Two home names that already exist on that server, and one that does not.
4. The client language set to something other than the one you read, for step 14.

## The test

| # | Step | Expected result |
|---|---|---|
| 1 | Launch the client | No HomeGui error in the log. `HomeGui` appears in the mod list with version `4.1.0` and described as client-side |
| 2 | *Controls → HomeGui* | One bindable key, "Open HomeGui", default **H** |
| 3 | Join the server, press **H** | The screen opens over the game. The game is **not** paused in singleplayer; in multiplayer the chat is not opened |
| 4 | Watch the first frame | A loading state reading "Requesting homes", not an empty panel and not a stale list |
| 5 | Wait for the reply | The homes the server reported, and only those. No duplicates, no homes from prose |
| 6 | `/homes` in chat while the screen is open | The list updates in place; the screen does not close and does not need reopening |
| 7 | Type in the search field | The list narrows as you type. Focus stays in the field — the caret does not jump away after one character |
| 8 | Search for something absent | "No match for …", not a blank area |
| 9 | Click a home | The screen closes, `/home <name>` is sent, and the player teleports |
| 10 | Reopen, press **H** again immediately | The screen closes. The keybind toggles |
| 11 | Keyboard only: `Tab` through every control | A visible focus ring on each, in a sensible order, wrapping at both ends. Nothing is reachable by mouse but not by keyboard |
| 12 | `↑`/`↓` through a list longer than the screen (grid: `←`/`→`) | The list scrolls to follow the focus; the last home is reachable |
| 13 | `Enter` on a focused home | Same as step 9 |
| 14 | Turn on the narrator (*Accessibility*) and move focus | Each control announces a name that identifies it, not "Button" for everything |
| 15 | Change the client language and reopen | The interface follows the client when the language setting is "Auto" |
| 16 | Set GUI scale to **Auto** and to the maximum, and resize the window small | Nothing is clipped or overlapping at any size; the list still fits |
| 17 | Right click a home | Its favourite star toggles and survives a reopen |
| 18 | *History* view | The teleports from steps 9 and 13, with a relative time. **A refused teleport is not listed** — try `/home` on a name the server will reject |
| 19 | *Stats* view | Counts match the teleports that actually happened, not the number of commands sent |
| 20 | *Settings*: change every setting | Each one changes something visible. Density, theme, transparency, coordinates, use counts, animations, grid columns and language must all be observable |
| 21 | Turn animations off | Nothing moves or fades any more |
| 22 | *Settings → Export*, then delete a favourite and *Import* | Both report their result in the interface, and the favourite comes back |
| 23 | *History → Clear* | A confirmation dialog first. Cancelling keeps the history; confirming clears it |
| 24 | Install a resource pack that overrides one HomeGui string | That string changes. The rest of the interface still works |
| 25 | Disconnect from the server with the screen open | The screen does not crash and does not show the previous server's homes |
| 26 | Join a **different** server and press **H** | Only that server's homes and favourites. Data is per server |
| 27 | Quit, relaunch, rejoin | Favourites, history, statistics and settings all survived the restart |
| 28 | Make `.minecraft/config/homegui/homegui.json` read-only, change a setting | The interface says the settings could not be saved |
| 29 | Corrupt `homegui.json` by hand, relaunch | The mod starts, and the old file is preserved as `homegui.json.corrupt-<timestamp>` |
| 30 | Remove every other mod except the loader and API, relaunch | No crash from a missing optional dependency |

## Recording the result

Copy this block into the pull request or release notes. A target may only be described
as tested in game when every line is filled in by someone who ran it.

```
Target:        homegui-<version>-<loader>-<minecraft>.jar
Loader:        <name and version>
Minecraft:     <version>
Java:          <version>
Home plugin:   <EssentialsX x.y / CMI x.y / other>
OS / display:  <os, windowed or fullscreen, GUI scale>
Date:          <yyyy-mm-dd>
Tester:        <github handle>
Steps failed:  <numbers, or "none">
Notes:         <anything worth knowing>
```
