# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Rules

- **No comments.** Do not write JavaDoc, inline comments, or block comments. Code should be self-documenting through clear naming. XML comments and the `@` annotations/attributes on project config files (plugin.yml, build.gradle.kts) are not comments and are fine.

## Project Overview

BedWars0721 (formerly XPWars) is a BedWars1058 addon that turns the vanilla XP level into the in-game currency — "XP mode" (经验起床). It hot-patches BedWars1058 classes with ASM + JVMTI so that resources convert to levels, the shop/upgrade/trap menus charge levels, and deaths move levels around. See [XP mode standard](#xp-mode-standard-经验起床) for the behaviour spec.

- **Main class:** `cc.bw0721.BedWars0721`
- **Command:** `/bw0721 reload`, `/bw0721 addxparena <arena>` (root permission `bw0721.command`)
- **Primary dependency:** BedWars1058 (`libs/bedwars-plugin-25.2.jar`, compile-only) — also the ground truth for any bytecode work
- **Compile target:** Spigot 1.8.8 (compile-only), `plugin.yml` `api-version: 1.13`
- **Java version:** 17 (Gradle toolchain)

## Repository layout

```
src/         Java sources + plugin.yml
libs/        compile-only jars (BedWars1058 25.2, sidebar libs)
NativeUtils/ dllmain.cpp (JVMTI agent), build.zig, jni/jvmti headers
skid/        decompiled BedWars1058 25.9 sources — read-only reference, never compiled (25.2 bytecode wins on conflict)
run/         1.8.8 test server (run-paper): worlds, configs, plugins, debug/ output
docs/        agent skills docs
```

## Build & Develop

```bash
./gradlew build        # zig natives + plugin → build/libs/BedWars0721-<version>.jar
./gradlew shadowJar    # jar only, still rebuilds natives through processResources
./gradlew runServer    # starts the 1.8.8 test server in run/
```

- The shadow jar has no classifier: `build/libs/BedWars0721-1.0.jar`.
- `processResources` depends on the zig build, so **zig (0.16.0, same version as CI) must be on PATH** for any jar build. Natives land in the jar under `natives/<target-triple>/`.
- `runServer` copies the built jar into `run/plugins/` as `BedWars0721-1.0_RunServer_plugin.jar`; `run/` already contains BedWars1058, Citizens, LuckPerms and arena worlds.

## Configuration

`plugins/BedWars0721/config.yml`, written on first enable by `ConfigManager` and re-written by `/bw0721 reload`. Configurate `@Setting`/`@Comment` classes; the locale picks the defaults (`zh` → `MainConfigChinese`, else `MainConfigEnglish`).

- `messages.prefix` / `messages.experience` / `messages.experience-color` — prefix, currency label and colour in XP mode
- `currency` — material name → levels granted per item; a missing or `0` entry stays an ordinary item
- `xp-arenas` — arena names running XP mode (write with `/bw0721 addxparena`, tab-completes from `BedWars1058/Arenas/*.yml`)

## Architecture

```
cc.bw0721
├── BedWars0721              — Plugin entry: config → command → TransformerManager.init() → listeners
├── config
│   ├── MainConfig           — Interface
│   ├── ConfigManager        — Configurate YAML loader, auto-picks locale (zh/en)
│   ├── MainConfigEnglish    — English defaults
│   └── MainConfigChinese    — Chinese defaults
├── command
│   ├── CommandManager       — /bw0721 executor + tab completer
│   ├── SubCommand           — Base class; hasPermission accepts bw.* / bw0721.* / its own node
│   └── impl
│       ├── ReloadCommand    — /bw0721 reload
│       └── AddArenaCommand  — /bw0721 addxparena <arena>
├── listener
│   ├── PickupItemListener   — Cancels item pickups in XP arenas and pays levels instead
│   └── DeathListener        — LOWEST: kills vanilla XP orbs. MONITOR: killer takes the victim's levels, victim is zeroed
├── transformer
│   ├── OreGeneratorTransformer    — Hooks OreGenerator.spawn() to pay gen-split payouts as levels
│   ├── CategoryContentTransformer — Rewrites the two money methods and the shop price/currency display
│   └── PlayerDropsTransformer     — Hooks PlayerDrops to drop XP bottles on death
├── asm
│   ├── TransformerManager   — init(): registers transformers, retransforms targets, applies new bytecode
│   ├── Transform           — Iterates transformers and their @Inject methods, rewrites classes
│   ├── ASMTransformer       — Base class (holds the target Class) + inner @Inject annotation (method + desc)
│   └── transformer
│       └── Operation        — findTargetMethod (desc via DescParser), isLoadOpe, isStoreOpe
└── utils
    ├── NativeUtils          — Native loader + JNI natives + original-byte cache filled by the JVMTI hook
    ├── XPUtils              — Currency map lookup, XP-arena lookup, pickup sound, XP bottle material
    ├── ReflectionUtils      — Reflection helpers (getFieldValue, setFieldValue, getMethod)
    └── asm
        ├── ASMUtils         — ClassNode/ClassWriter helpers, annotation value extraction
        ├── ClassUtils       — Class.forName lookup used for frame computation
        └── DescParser       — Descriptor mapping
```

### Bytecode injection flow

```
BedWars0721.onEnable()
  → TransformerManager.init()
    → for each transformer: NativeUtils.a(targetClass)     ← native RetransformClasses
        → JVMTI ClassFileLoadHook → ProcessHookedClassFile
          → Java NativeUtils.a(cls, loader, name, bytes)   ← caches the ORIGINAL bytes per target class
    → Transform.transform()
      → per transformer: NativeUtils.b(targetClass)        ← plain Java: read the cached bytes
        → ASMUtils.node(bytes)                             ← ClassNode
        → invoke each @Inject method with its target MethodNode
        → ASMUtils.rewriteClass(node)                      ← COMPUTE_MAXS | COMPUTE_FRAMES
    → NativeUtils.b(targetClass, newBytes)                 ← native RedefineClasses — hot swap
```

### Native library system

`NativeUtils` loads the agent at class-init: detect OS/arch → target triple → extract `natives/<triple>/libNativeUtils.{dll,dylib,so}` from the jar to a temp file → `System.load()`. `isLoaded()` reports the result.

- `native a(Class)` — JVMTI `RetransformClasses`; the only way to make the JVM hand us a class's current bytes
- `native b(Class, byte[])` — JVMTI `RedefineClasses`; hot-swaps the class
- `a(Class, ClassLoader, String, byte[])` — **not native**: called back from `ProcessHookedClassFile` for every transformer target during the retransform, caches the original bytes in `cachedClassBytes`
- `b(Class)` — **not native**: reads that cache (`Transform` uses it instead of asking the JVM again)

`NativeUtils/dllmain.cpp`: `JNI_OnLoad` installs the `ClassFileLoadHook` callback; `ProcessHookedClassFile` calls the Java `a(...)` above; `Java_cc_bw0721_utils_NativeUtils_a`/`_b` wrap `RetransformClasses`/`RedefineClasses`.
`NativeUtils/build.zig` cross-compiles it for 8 triples (`x86_64`/`x86`/`aarch64` × windows, and `x86_64`/`x86`/`aarch64` linux-gnu, plus `x86_64`/`aarch64` macos).

### Transformer patterns

Three strategies, all driven by `@Inject(method, desc)` on a `MethodNode`:

1. **Jump redirect** (`OreGeneratorTransformer`) — find the `JumpInsnNode` by opcode + operand pattern, set `jump.label` to a fresh `LabelNode`, append the new block (label + `ALOAD`s + `INVOKESTATIC` + `RETURN`) to the method.
2. **Call rewiring** (`CategoryContentTransformer.getItemStack`/`execute`, `PlayerDropsTransformer.handlePlayerDrops` style) — retarget a `MethodInsnNode` at a `public static` hook: change `owner`/`name`/`desc`, push extra arguments with `ALOAD`/`ILOAD`, and delete instructions the hook now covers (e.g. the `Language.getMsg` after `getCurrencyMsgPath`, or a whole `sendMessage` block).
3. **Body replacement / insertion** (`CategoryContentTransformer.calculateMoney`+`takeMoney`, `PlayerDropsTransformer.dropItems`) — replace `method.instructions` wholesale with a single delegation call (clear `tryCatchBlocks` and `localVariables` too), or insert `ALOAD`+`INVOKESTATIC` before the last `RETURN`.

`Transform` only dispatches methods that take exactly one `MethodNode`; a hook may never call back into the method it rewrote (infinite recursion — reimplement the vanilla fallback inside the hook instead).

To add a transformer: extend `ASMTransformer` with the target class in `super()`, add `@Inject` methods, and register it in `TransformerManager.init()`.

### Key patterns

- **Hook targeting is bytecode-exact.** Read the real class with `javap -p -c -classpath libs/bedwars-plugin-25.2.jar <class>` before writing a pattern; `skid/` (25.9) drifts from the shipped 25.2 jar.
- **XP arena dispatch:** every hook starts from `XPUtils.isXPArena(arena.getArenaName())` (and `Arena.getArenaByPlayer` may be null) — outside XP arenas the vanilla BedWars path must run unchanged.
- **Version compatibility:** `BedWars.getForCurrentVersion(v1_8, v1_12, v1_13)` resolves material/sound names; `XPUtils` uses it for the pickup sound and the XP bottle material.
- **Configurate YAML** with `@Setting`/`@Comment` + Lombok `@Getter`; `ConfigManager.save()` rewrites the file from the config object.
- **Libby runtime loading:** `configurate-yaml`, `asm`, `asm-tree` are downloaded on enable from the Aliyun mirror; the shadow plugin relocates `com.alessiodp.libby` → `cc.bw0721.libby`.
- **ASM injection framework** ported from the RelX client: `@ASMTransformer.Inject` marks hooks, `Transform` finds each target and calls the hook, `NativeUtils` applies the result via JVMTI.
- **Debug output:** with `TransformerManager.debugging` the rewritten classes are written to `debug/` **relative to the server working directory** (`run/debug/`) at every enable — inspect with `javap -p -c <file>.class`.

## XP mode standard (经验起床)

Behaviour spec, distilled from the BedWars1058-XP fork: rules 1–8 are what the code does today, rule 8 lists the known gaps.

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

**Status:** all of the above is implemented and checked against the shipped jar — a test server enable redefines `OreGenerator`, `CategoryContent` and `PlayerDrops` with no errors, and `run/debug/*.class` shows the expected bytecode at every injection point. In-game behaviour (level transfer, bottle drops, upgrade purchases) has not been playtested yet.

## Agent skills

### Issue tracker

Issues live as **GitHub issues** on `jiuxian1337/BedWars0721` (using `gh` CLI). See `docs/agents/issue-tracker.md`.

### Triage labels

Five canonical labels: `needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix` (all default names). See `docs/agents/triage-labels.md`.

### Domain docs

**Single-context** — one `CONTEXT.md` + `docs/adr/` at repo root. See `docs/agents/domain.md`.
