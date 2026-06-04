# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

BedWars1058-XPWars is a Minecraft addon plugin for the BedWars1058 minigame. It introduces an "XP mode" where in-game currencies (iron, gold, emeralds, XP bottles) can be converted into XP/experience points for use in a special shop.

- **Main class:** `cc.xpWars.XPWars`
- **API target:** Spigot 1.8.8 (compile-only)
- **Primary dependency:** BedWars1058 plugin (`libs/bedwars-plugin-25.2.jar`)
- **Java version:** 17 (Gradle toolchain)

## Build & Develop

```bash
# Build the plugin JAR
./gradlew build
# JAR output: build/libs/BedWars1058-XPWars-1.0-SNAPSHOT.jar

# Start a 1.8.8 test server with the plugin pre-loaded
./gradlew runServer
# Server directory: run/ — contains all plugin configs, worlds, and server.properties
```

No test tasks are configured.

## Architecture

```
cc.xpWars
├── XPWars                    — Plugin entry point, extends JavaPlugin
├── config
│   ├── MainConfig            — Interface: getPrefix(), getLevel(), getCurrency(), getXpArenas()
│   ├── ConfigManager         — Loads/saves config.yml via Configurate YAML; auto-picks locale
│   ├── MainConfigEnglish     — English defaults (implements MainConfig)
│   └── MainConfigChinese     — Chinese defaults (implements MainConfig)
└── command
    ├── CommandManager        — /xpwars executor + tab completer, dispatches to subcommands
    ├── SubCommand            — Base class (name, permission, canSee, getTabComplete)
    └── impl
        ├── ReloadCommand     — /xpwars reload (permission: xpwars.command.reload)
        └── AddArenaCommand   — /xpwars addarena <arena> (permission: xpwars.command.addarena)
```

### Key patterns

- **Configuration:** Configurate YAML with object mapping. Config classes use `@Setting`/`@Comment` annotations + Lombok `@Getter`. `ConfigManager` detects system locale: `zh` → `MainConfigChinese`, otherwise `MainConfigEnglish`. On first run, writes defaults to disk.
- **Runtime dependency loading (Libby):** `configurate-yaml` is NOT shaded into the JAR — `XPWars.onEnable()` uses `libby-bukkit` to download it from Aliyun Maven mirror at runtime. Shadow plugin relocates `com.alessiodp.libby` → `cc.xpWars.libby` to avoid conflicts.
- **Command system:** `CommandManager` holds a `Map<String, SubCommand>`. Each `SubCommand` declares its name and permission. `hasPermission()` checks `bw.*`, `xpwars.*`, or the specific permission. Tab completion for `addarena` scans `BedWars1058/Arenas/` for `.yml` files.
- **Version compatibility:** `BedWars.getForCurrentVersion(...)` resolves material names across server versions (e.g., `EXP_BOTTLE` vs `EXPERIENCE_BOTTLE`).
- **Dependency resolution:** BedWars1058 JAR in `libs/`, included via `compileOnly(fileTree("libs"))`. Replace the JAR to upgrade.

### Currency system

Minecraft material → XP value, configured in `config.yml`:
- Iron Ingot = 1 XP
- Gold Ingot = 10 XP
- XP Bottle = 10 XP
- Emerald = 100 XP

### Plugin metadata

`plugin.yml` uses Gradle resource filtering — `version` is expanded from `gradle.properties`. Permission nodes: `xpwars.admin` (parent), `xpwars.command.reload`, `xpwars.command.addarena` (all default to op).

## Agent skills

### Issue tracker

Issues live as **GitHub issues** on `jiuxian1337/BedWars1058-XPWars` (using `gh` CLI). See `docs/agents/issue-tracker.md`.

### Triage labels

Five canonical labels: `needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix` (all default names). See `docs/agents/triage-labels.md`.

### Domain docs

**Single-context** — one `CONTEXT.md` + `docs/adr/` at repo root. See `docs/agents/domain.md`.
