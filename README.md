# COMBATFX RPG

A lightweight client-side HUD overhaul for Minecraft designed for RPG enthusiasts.
Fabric, Minecraft 26.1.2 / 26.2 (build matrix di `.github/workflows/build.yml`).

## Modul
- **RPG HUD** (`dev.rpghud`): bar horizontal / lingkaran, 6 tema warna + tema Auto dari resource pack
- **CombatFX** (`dev.combatfx`): angka damage melayang, combo counter, critical hit
- **Extras** (`dev.rpgcombat.extras`): bar progress hancur blok (monokrom), popup item pickup (ikon + enchant + durability), nama item hotbar
- **Menu** (`dev.rpgcombat.gui`): `SettingsScreen` (tombol `H`) dan `HudCustomizerScreen` (tombol `J`, drag & drop)

## Config
Satu file: `.minecraft/config/rpgcombat.json` (seksi `hud`, `combat`, `extras`). Posisi dan skala setiap elemen
tersimpan otomatis saat mouse dilepas di mode edit.

## Build
JDK 25 + Gradle 9.4+: `gradle build`. Hasil: `build/libs/combatfx-rpg-<versi>+mc<mc>.jar`.
Untuk versi lain: `gradle build -Pminecraft_version=26.1.2 -Pfabric_version=<versi fabric api>`.

## Command
`/rpghud ...`, `/combatfx gui|toggle|reset`
