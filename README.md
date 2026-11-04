# COMBATFX RPG

A lightweight client-side HUD overhaul for Minecraft designed for RPG enthusiasts.
Fabric, Minecraft 26.1.2 / 26.2 (build matrix di `.github/workflows/build.yml`).

## Modul
- **RPG HUD** (`dev.rpghud`): bar horizontal / lingkaran, 6 tema warna + tema Auto dari resource pack
- **CombatFX** (`dev.combatfx`): angka damage melayang, combo counter, critical hit
- **Extras** (`dev.rpgcombat.extras`): bar progress hancur blok (monokrom), popup item pickup (ikon + enchant + durability), popup nama + enchant item di hotbar
- **Menu baru** (`dev.rpgcombat.menu`, `dev.rpgcombat.ui`): `CardDeckScreen` (tombol `H`) = dek kartu melayang, transisi ke panel navigasi + grid fitur, search, favorit, suara UI lokal. Halaman = daftar `Row`, didaftarkan di `Features.java`. Menu klasik tetap ada di QoL > Menu & sounds.
- **Efek tempur** (`dev.rpgcombat.fx`, `dev.rpgcombat.preview`): Trails, Hit effects, Kill effects. Satu `FxStyle` dipakai efek di dunia (partikel vanilla) DAN preview 2D di menu. Preview: `PreviewCanvas` + `TrailScene` / `HitScene` / `KillScene`.
- **Menu lama** (`dev.rpgcombat.gui`): `SettingsScreen` (tombol `H`, tab Opacity per elemen) dan `HudCustomizerScreen` (tombol `J`, drag & drop; kartu pemain, status bar, koordinat/FPS, combo, popup loot, bar menambang, nama hotbar)

- **Real Wounds** (`dev.rpgcombat.vfx`): busur tebasan custom + partikel spark/debris/ledakan (pilih gaya "Real Wounds" di menu Hit effects)

## Config
Satu file: `.minecraft/config/rpgcombat.json` (seksi `hud`, `combat`, `extras`). Posisi dan skala setiap elemen
tersimpan otomatis saat mouse dilepas di mode edit.

## Build
CI: `.github/workflows/build.yml` menjalankan `./gradlew build` untuk 26.2 (dan 26.1.2 eksperimental) pada setiap push dan mengunggah jar ke Artifacts.
JDK 25 + Gradle 9.4+: `gradle build`. Hasil: `build/libs/combatfx-rpg-<versi>+mc<mc>.jar`.
Untuk versi lain: `gradle build -Pminecraft_version=26.1.2 -Pfabric_version=<versi fabric api>`.

## Command
`/rpghud ...`, `/combatfx gui|toggle|reset`
