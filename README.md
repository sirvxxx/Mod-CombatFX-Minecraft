# RPG Combat HUD (single jar)

Gabungan **Custom RPG HUD** (`dev.rpghud`) dan **CombatFX** (`dev.combatfx`) untuk Fabric, Minecraft 26.2.

- `H` : menu pengaturan master  |  `J` : mode edit HUD (drag & drop)
- Config tunggal: `.minecraft/config/rpgcombat.json` (seksi `hud` dan `combat`)
- Config HUD lama `custom_rpg_hud.json` diimpor otomatis sekali kalau `rpgcombat.json` belum ada
- Extras: bar progress hancur blok (linear/circular + persen) dan popup item pickup (antrean, detail enchant, durability)
- Command: `/rpghud ...`, `/combatfx gui|toggle|reset`

Build: JDK 25 + Gradle 9.4+, jalankan `gradle build`. Atau pakai `.github/workflows/build.yml`.

Catatan: kode `dev.rpghud` berasal dari decompile jar custom-rpg-hud 3.0.0 (CFR), lalu dirapikan.
