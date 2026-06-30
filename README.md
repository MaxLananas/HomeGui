<div align="center">

<img width="172" height="58" alt="image" src="https://github.com/user-attachments/assets/5be51ecc-08eb-4a97-bfc2-5df2adc12f41" />

<br/>

# HomeGUI

**A clean, client-side home management interface for Minecraft.**  
No server installation required. Just open, click, and teleport.

<br/>

[![Modrinth](https://img.shields.io/badge/Available%20on-Modrinth-1bd96a?style=for-the-badge&logo=modrinth&logoColor=white)](https://modrinth.com/mod/homegui)
[![Discord](https://img.shields.io/badge/Join%20the-Discord-5865F2?style=for-the-badge&logo=discord&logoColor=white)](https://discord.gg/pnJhKuU2QK)
[![Fabric](https://img.shields.io/badge/Mod%20Loader-Fabric-DB8135?style=for-the-badge)](https://fabricmc.net)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-62B47A?style=for-the-badge)](https://minecraft.net)

</div>

---

## Overview

HomeGUI replaces the need to type `/homes` and `/home <name>` manually.  
Press a single key, and a graphical interface opens with all your homes listed, searchable, and ready to teleport to in one click.

The mod intercepts the server's chat response to `/homes`, parses the home list regardless of server formatting, and displays everything in a custom-built screen — entirely on the client side.

---

## Features

| Feature | Description |
|---|---|
| Instant GUI | Open your home list with the `H` key |
| Smart Parsing | Supports multiple server chat formats automatically |
| Search | Filter homes by name in real time |
| Favorites | Right-click any home to mark it as a favorite |
| History | Tracks your last 15 teleportations with timestamps |
| Statistics | View total teleports and your top 5 most visited homes |
| Language | Full English and French support |
| Persistence | All data saved locally to `config/homegui.json` |

---

## Installation

**Requirements**

- Minecraft `1.21.1`
- [Fabric Loader](https://fabricmc.net/use/)
- [Fabric API](https://modrinth.com/mod/fabric-api)

**Steps**

1. Install Fabric Loader for your Minecraft version
2. Download the latest HomeGUI jar from [Modrinth](https://modrinth.com/mod/homegui)
3. Drop the jar into your `.minecraft/mods/` folder
4. Launch and play

---

## Keybind

| Action | Default Key |
|---|---|
| Open HomeGUI | `H` |

Can be changed in `Options > Controls > Key Binds > HomeGUI`.

---

## Configuration

Location: `.minecraft/config/homegui.json`

```json
{
  "themeIndex": 0,
  "compactMode": false,
  "language": "en",
  "totalTeleports": 0,
  "favorites": [],
  "useCounts": {},
  "history": []
}
```

The file is generated on first launch. If the file is corrupted or unreadable,  
it resets automatically without crashing.

---

## Compatibility

HomeGUI works with any server that uses standard `/homes` and `/home <name>` commands.  
It is purely client-side and requires no plugin or mod on the server.

Servers with heavily custom or obfuscated chat formatting may not be parsed correctly.  
If your server format is not supported, feel free to open an issue or report it on Discord.

---

## Links

<div align="center">

[![Modrinth](https://img.shields.io/badge/Download%20on-Modrinth-1bd96a?style=for-the-badge&logo=modrinth&logoColor=white)](https://modrinth.com/mod/homegui)
[![Discord](https://img.shields.io/badge/Get%20Support%20on-Discord-5865F2?style=for-the-badge&logo=discord&logoColor=white)](https://discord.gg/pnJhKuU2QK)

</div>

---

<div align="center">
<sub>Built with Fabric — Client-side only — No server installation required</sub>
</div>
