# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Rules

- **No comments.** Do not write JavaDoc, inline comments, or block comments. Code should be self-documenting through clear naming. XML comments and the `@` annotations/attributes on project config files (plugin.yml, build.gradle.kts) are not comments and are fine.

## Project Overview

BedWars0721 (formerly XPWars) is a Minecraft addon plugin for BedWars1058. It converts in-game currencies (iron, gold, emeralds, XP bottles) into XP points, and hooks into OreGenerator item distribution to redirect per-player payouts.

- **Main class:** `cc.bw0721.BedWars0721`
- **Command:** `/bw0721` (permission: `bw0721.admin`)
- **API target:** Spigot 1.8.8 (compile-only)
- **Primary dependency:** BedWars1058 plugin (`libs/bedwars-plugin-25.2.jar`)
- **Java version:** 17 (Gradle toolchain)

## Build & Develop

```bash
./gradlew build        # Builds plugin + native libs → build/libs/BedWars0721-*-all.jar
./gradlew runServer    # Starts a 1.8.8 test server in run/
./gradlew shadowJar    # Build just the fat JAR without native rebuild
```

Native libraries are cross-compiled via Zig 0.16.0 as part of the Gradle build and bundled into the JAR under `natives/<target-triple>/`.

## Architecture

```
cc.bw0721
├── BedWars0721              — Plugin entry, extends JavaPlugin
├── config
│   ├── MainConfig           — Interface
│   ├── ConfigManager        — Configurate YAML loader, auto-picks locale (zh/en)
│   ├── MainConfigEnglish    — English defaults
│   └── MainConfigChinese    — Chinese defaults
├── command
│   ├── CommandManager       — /bw0721 executor + tab completer
│   ├── SubCommand           — Base class (name, permission, hasPermission checks bw.* / bw0721.*)
│   └── impl
│       ├── ReloadCommand    — /bw0721 reload
│       └── AddArenaCommand  — /bw0721 addarena <arena>
├── listener
│   ├── PickupItemListener   — Intercepts item pickups, converts to XP in XP arenas
│   └── DeathListener        — LOWEST: kills vanilla XP orbs. MONITOR: killer takes the victim's levels, victim is zeroed
├── transformer
│   ├── OreGeneratorTransformer    — ASM hook into OreGenerator.spawn(), redirects jump target to replace per-player item distribution
│   ├── CategoryContentTransformer — ASM hook into shop: rewrites calculateMoney/takeMoney bodies, plus getPrice/currency display call sites
│   └── PlayerDropsTransformer     — ASM hook into PlayerDrops: XP bottles after the bed-destroyed drop loop and at the end of dropItems
├── asm
│   ├── TransformerManager   — init(): registers transformers, triggers bytecode patching + NativeUtils.b()
│   ├── Transform           — Manages ASMTransformer list, iterates @Inject methods, rewrites classes
│   ├── ASMTransformer       — Base class for bytecode hooks, inner @Inject annotation (method + desc)
│   └── transformer
│       └── Operation        — Interface: findTargetMethod, isLoadOpe, isStoreOpe
└── utils
    ├── NativeUtils          — Static native loader (extracts from JAR → temp → System.load()) + JNI native methods
    ├── XPUtils              — XP value lookup, arena XP-mode detection, pickup sound
    ├── ReflectionUtils       — Reflection helpers (getFieldValue, setFieldValue, getMethod)
    └── asm
        ├── ASMUtils         — ClassNode/ClassWriter helpers, annotation value extraction
        ├── ClassUtils       — (legacy)
        └── DescParser       — Descriptor mapping
```

### Bytecode injection flow

```
BedWars0721.onEnable()
  → TransformerManager.init()
    → transform.addTransformer(new OreGeneratorTransformer())
    → transform.addTransformer(new CategoryContentTransformer())
    → NativeUtils.a(Class)              ← JVMTI retransform hook — marks class for re-load, for each transformer target
    → transform.transform()
      → for each transformer: iterate @ASMTransformer.Inject methods
        → NativeUtils.b(Class)           ← fetch original class bytes from JVM
        → ASMUtils.node(bytes)           ← parse to ClassNode
        → invoke @Inject method with MethodNode  ← OreGeneratorTransformer.hookSpawn()
        → ASMUtils.rewriteClass(node)    ← emit modified bytecode
    → NativeUtils.b(Class, newBytes)    ← JVMTI RedefineClasses — hot-swap class
```

### Native library system

**NativeUtils.java:**
- `static {}` block: detects OS/arch → maps to target triple (e.g. `x86_64-windows`) → extracts DLL/SO/dylib from JAR resource → `System.load()`
- `native a(Class)` — JVMTI `RetransformClasses` — triggers ClassFileLoadHook callback
- `native b(Class, byte[])` — JVMTI `RedefineClasses` — hot-replaces class bytecode
- `a(Class, ClassLoader, String, byte[])` — JVM callback from `ProcessHookedClassFile`, caches original bytes

**dllmain.cpp:**
- `JNI_OnLoad` → sets up JVMTI ClassFileLoadHook callback
- `ProcessHookedClassFile` → calls back into Java `NativeUtils.a()` to decide whether to cache/modify class bytes
- `Java_cc_bw0721_utils_NativeUtils_a` — `RetransformClasses(1, &arg1)`
- `Java_cc_bw0721_utils_NativeUtils_b` — `RedefineClasses(1, &classDef)`
- `saved_classloader` cached on first native call from Java side

**build.zig:** Cross-compiles dllmain.cpp → 8 platform targets (`x86_64-windows`, `x86-windows`, `aarch64-windows`, `x86_64-linux-gnu`, `x86-linux-gnu`, `aarch64-linux-gnu`, `x86_64-macos`, `aarch64-macos`)

### Transformer patterns

Two injection strategies are used:

**1. Jump redirect** (OreGeneratorTransformer) — Replace a conditional branch target to inject new code at a specific control-flow point:
1. Find the target `JumpInsnNode` by matching opcode + operand pattern
2. Set `jump.label = newLabel` to redirect the branch
3. Append new instructions (including the new label) to the method

**2. Call rewiring** (CategoryContentTransformer) — Replace INVOKESTATIC/INVOKEINTERFACE calls to point at static hook methods:
1. Iterate instructions looking for `MethodInsnNode` with matching owner + name
2. Change `method.owner` → transformer class, `method.name` → hook method, `method.desc` → new descriptor
3. Remove subsequent instructions that are no longer needed (e.g. `Language.getMsg()` after `getCurrencyMsgPath` if the hook already returns the translated string)
4. Add `ALOAD` instructions before the call if the hook needs extra arguments (e.g. the Player)

To add a new transformer:
1. Extend `ASMTransformer`, pass target class in `super()`
2. Add methods annotated with `@ASMTransformer.Inject(method="...", desc="...")`
3. The method receives the target `MethodNode` — modify its `instructions` list directly
4. Register with `transform.addTransformer(new YourTransformer())` in `TransformerManager.init()`

### Key patterns

- **Configurate YAML** with `@Setting`/`@Comment` annotations + Lombok `@Getter`. Locale auto-detection: `zh` → Chinese, else English.
- **Libby runtime loading**: `configurate-yaml`, `asm`, `asm-tree` downloaded at runtime from Aliyun Maven mirror. Shadow plugin relocates `com.alessiodp.libby` → `cc.bw0721.libby`.
- **JVMTI native agent**: Dllmain hooks class loading via `ClassFileLoadHook`, calls back into Java for modification decisions. Native libs bundled in JAR, extracted at runtime.
- **ASM injection framework**: Ported from RelX client. Uses `@ASMTransformer.Inject` to mark hook methods. `Transform` iterates transformers, finds target MethodNodes, invokes hook methods, rewrites classes, applies via JVMTI.
- **Version compatibility:** `BedWars.getForCurrentVersion(...)` resolves material/sound names across server versions.
- **XP arena dispatch:** All transformers + listener follow the same pattern: `XPUtils.isXPArena(arenaName)` → if true, apply XP conversion logic; else delegate to the original BedWars method.
- **Debug output:** When `TransformerManager.debugging` is true, transformed `.class` files are written to `debug/` for inspection via `javap`.

### Plugin metadata

`plugin.yml` uses Gradle resource filtering — `version` expanded from `gradle.properties`. Permissions: `bw0721.command`, `bw0721.command.reload`, `bw0721.command.addxparena` (all default to op).

### Reference sources

`skid/BedWars1058-25.9/` contains decompiled BedWars1058 sources used as reference when writing ASM hooks. These are NOT compiled or shipped — read-only reference for understanding target class bytecode shapes.

## XP mode standard (经验起床)

The complete-behaviour spec for XP mode. Reference implementations: `skid/BedWars1058-25.9/` (decompiled sources) and the BedWars1058-XP fork. Every hook must be written against the real bytecode of `libs/bedwars-plugin-25.2.jar` (`javap -p -c -classpath libs/bedwars-plugin-25.2.jar <class>`), not against the 25.9 reference sources.

### Rules

1. **Currency is the vanilla XP level** (`Player#getLevel`). `currency` in the plugin config maps material name → levels granted per item. Unset or `0` = that material stays an item.
2. **Scope**: only arenas listed in `xp-arenas`. Vanilla BedWars1058 already zeroes level/exp on arena join and restores them on leave (`PlayerGoods`), so in-game levels never leak to the lobby.
3. **Earning**: picking up a dropped resource converts it to levels (`PickupItemListener`, `amount × currency value`); generator "gen-split" payouts go straight to the inventory and never fire a pickup event, so they convert inside `OreGenerator.spawn` (`OreGeneratorTransformer`, `generator amount × currency value`).
4. **Spending**: shop, team upgrades and traps all settle through `CategoryContent.calculateMoney` / `CategoryContent.takeMoney`. Those two method bodies are rewritten in place so every caller (shop GUI, quick buy, `MenuUpgrade`, `MenuBaseTrap`, `BedWars#getShopUtil`) is XP-aware; price display stays a call-site hook.
5. **Death**:
   - Vanilla XP orbs are always suppressed (`PlayerDeathEvent#setDroppedExp(0)`) — an orb is not an `Item`, so `PlayerPickupItemListener` can never intercept it and it would leak free levels.
   - Regular death (bed alive + killer in arena + killer not respawning) → the killer **takes** the victim's levels; the victim is zeroed. No bottles.
   - Any other death (void, no killer, despawnable, PvP logout) → `level / bottleValue` XP bottles drop at the death location; the victim is zeroed.
   - Final kill (bed destroyed) → `level / bottleValue` XP bottles drop at the team kill-drops location.
   - `bottleValue` = `currency` entry of the XP bottle material (default 10), so collecting the bottles roughly refunds the level.
6. **Shop display**: price = vanilla price × currency XP value; currency label/colour come from `messages.experience` / `messages.experience-color`; the "insufficient money" gap is expressed in levels, not in item counts.
7. **Hooks are null-safe**: `Arena.getArenaByPlayer` can return null, and every drop path guards on `XPUtils.isXPArena` so normal arenas keep vanilla behaviour.
8. **Known limitations**: the upgrade/trap menu lore still prints the original currency name and item price (only affordability and deduction are XP-aware) — same as the reference fork. In mixed mode (a currency whose value is 0, e.g. diamonds by default) the price text stays an item price while `getItemStack` still paints it with `messages.experience-color`, because the colour hook only receives the colour local, not the currency.

### Checklist

- [x] Block vanilla XP orbs on death in XP arenas
- [x] Killer takes the victim's levels on a regular death (victim zeroed)
- [x] Drop XP bottles from `PlayerDrops.dropItems` (void / no killer / despawnable / PvP logout)
- [x] Make team upgrades and traps settle in XP (rewrite `CategoryContent.calculateMoney` + `takeMoney` bodies)
- [x] Report the insufficient-balance gap in levels, not item units
- [x] Null-safe hooks + `isXPArena` guard on every drop path
- [x] Verified: `Arena.removePlayer` needs no change (vanilla `PlayerGoods.restore` already rewrites level and exp)

## Agent skills

### Issue tracker

Issues live as **GitHub issues** on `jiuxian1337/BedWars0721` (using `gh` CLI). See `docs/agents/issue-tracker.md`.

### Triage labels

Five canonical labels: `needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix` (all default names). See `docs/agents/triage-labels.md`.

### Domain docs

**Single-context** — one `CONTEXT.md` + `docs/adr/` at repo root. See `docs/agents/domain.md`.
