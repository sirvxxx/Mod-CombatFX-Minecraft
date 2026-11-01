# COMBATFX RPG

**A lightweight client-side HUD overhaul for Minecraft designed for RPG enthusiasts.**

Fabric · Minecraft 26.1.2 / 26.2 · Client-side only · Requires Fabric API

## Features

### RPG HUD
- Compact horizontal-bar layout (plus an optional circular layout)
- Health, hunger and armor bars, level badge, player face, coordinates and FPS
- 6 built-in color themes, plus **Auto** — borders and backgrounds adapt to your active resource pack
  (dark/slate, classic wood or vanilla) by reading the pack's own GUI texture

### CombatFX
- Floating 3D-style damage numbers with pop and drift animation (normal / heavy / critical)
- Combo counter with pulse animation, tier colors and a smooth timeout bar

### Loot & mining feedback
- Item pickup popups with the item icon, `+N Name`, durability and enchantment list (with level badges)
- Queue system: simultaneous pickups never overlap, identical items merge
- Minimal monochrome block-breaking progress bar next to the crosshair (linear or circular)
- Clean item-name popup when switching hotbar slots

### Customization
- In-game settings menu (`H`) with sidebar tabs
- Drag-and-drop HUD editor (`J`) — every element can be moved and scaled, saved automatically
- Everything stored in a single JSON file: `config/rpgcombat.json`

## Installation
1. Install Fabric Loader and Fabric API.
2. Drop the `.jar` into your `mods` folder.
3. Press `H` in game to open the settings.

Client-side only: it does not need to be installed on the server.
