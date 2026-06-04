# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

BedWars1058-XPWars is a Minecraft addon plugin for the BedWars1058 minigame. It introduces an "XP mode" where in-game currencies (iron, gold, emeralds, XP bottles) can be converted into XP/experience points for use in a special shop.

- **Main class:** `cc.xpWars.XPWars`
- **API target:** Spigot 1.8.8 (compile-only)
- **Primary dependency:** BedWars1058 plugin (`libs/bedwars-plugin-25.2.jar`)
- **Java version:** 11 (Gradle toolchain)

## Build & Develop

```bash
# Build the plugin JAR
./gradlew build

# The output JAR will be at:
# build/libs/BedWars1058-XPWars-1.0-SNAPSHOT.jar
```

Only a single source file exists (`XPWars.java`), so there are no test tasks configured.

## Architecture

```
cc.xpWars
├── XPWars              — Plugin entry point, extends JavaPlugin
└── config
    ├── ConfigManager   — Loads/saves config.yml using Configurate (YAML)
    ├── MainConfigChinese — Chinese locale config model (messages, currency mappings)
    └── MainConfigEnglish — English locale config model (messages, currency mappings)
```

### Key patterns

- **Configuration:** Uses SpongePowered Configurate with object mapping. Config classes use `@Setting` and `@Comment` annotations to map YAML keys, with Lombok `@Getter` for access. `ConfigManager` handles file resolution and loader initialization.
- **Version compatibility:** Uses `BedWars.getForCurrentVersion(...)` to resolve material names across different server versions (e.g., `EXP_BOTTLE` vs `EXPERIENCE_BOTTLE`).
- **Dependency resolution:** The BedWars1058 plugin JAR is kept in `libs/` and included as a compile-only dependency via `fileTree("libs")`. When upgrading the BedWars dependency, replace the JAR in `libs/`.
- **Lombok:** `@Getter` is used on config classes; the annotation processor is configured in `build.gradle.kts`. Ensure Lombok is enabled in your IDE.

### Currency system

The config maps Minecraft material names to their XP value when converted. The default mapping:
- Iron Ingot = 1 XP
- Gold Ingot = 10 XP
- XP Bottle = 10 XP
- Emerald = 100 XP

### Plugin metadata

`src/main/resources/plugin.yml` uses Gradle resource filtering — the `version` field is expanded from `gradle.properties` during `processResources`.
