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
│   └── PickupItemListener   — Intercepts item pickups, converts to XP in XP arenas
├── transformer
│   └── OreGeneratorTransformer — ASM hook into OreGenerator.spawn(), replaces per-player item distribution
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
    → NativeUtils.a(Class)              ← JVMTI retransform hook — marks class for re-load
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
- `static {}` block: detects OS/arch → maps to target triple (e.g. `x86_64-windows`) → extracts DLL/SO from JAR resource → `System.load()`
- `native a(Class)` — JVMTI `RetransformClasses` — triggers ClassFileLoadHook callback
- `native b(Class, byte[])` — JVMTI `RedefineClasses` — hot-replaces class bytecode
- `a(Class, ClassLoader, String, byte[])` — JVM callback from `ProcessHookedClassFile`, caches original bytes

**dllmain.cpp:**
- `JNI_OnLoad` → sets up JVMTI ClassFileLoadHook callback
- `ProcessHookedClassFile` → calls back into Java `NativeUtils.a()` to decide whether to cache/modify class bytes
- `Java_cc_bw0721_utils_NativeUtils_a` — `RetransformClasses(1, &arg1)`
- `Java_cc_bw0721_utils_NativeUtils_b` — `RedefineClasses(1, &classDef)`
- `saved_classloader` cached on first native call from Java side

**build.zig:** Cross-compiles dllmain.cpp → 6 platform targets (`x86_64-windows`, `x86-windows`, `aarch64-windows`, `x86_64-linux-gnu`, `x86-linux-gnu`, `aarch64-linux-gnu`)

### Transformer pattern

To add a new bytecode hook:
1. Extend `ASMTransformer`, pass target class in `super()`
2. Add methods annotated with `@ASMTransformer.Inject(method="...", desc="...")`
3. The method receives the target `MethodNode` — modify its `instructions` list directly
4. Register with `TransformerManager.transform.addTransformer(new YourTransformer())` in `TransformerManager.init()`

See `OreGeneratorTransformer` for example: replaces the `if (players.length > 1)` else branch in `OreGenerator.spawn()` by finding the `IF_ICMPGT` jump instruction and redirecting its label to new code.

### Key patterns

- **Configurate YAML** with `@Setting`/`@Comment` annotations + Lombok `@Getter`. Locale auto-detection: `zh` → Chinese, else English.
- **Libby runtime loading**: `configurate-yaml`, `asm`, `asm-tree` downloaded at runtime from Aliyun Maven mirror. Shadow plugin relocates `com.alessiodp.libby` → `cc.bw0721.libby`.
- **JVMTI native agent**: Dllmain hooks class loading via `ClassFileLoadHook`, calls back into Java for modification decisions. Native libs bundled in JAR, extracted at runtime.
- **ASM injection framework**: Ported from RelX client. Uses `@ASMTransformer.Inject` to mark hook methods. `Transform` iterates transformers, finds target MethodNodes, invokes hook methods, rewrites classes, applies via JVMTI.
- **Version compatibility:** `BedWars.getForCurrentVersion(...)` resolves material/sound names across server versions.

### Plugin metadata

`plugin.yml` uses Gradle resource filtering — `version` expanded from `gradle.properties`. Permissions: `bw0721.admin`, `bw0721.command.reload`, `bw0721.command.addarena` (all default to op).

## Agent skills

### Issue tracker

Issues live as **GitHub issues** on `jiuxian1337/BedWars1058-XPWars` (using `gh` CLI). See `docs/agents/issue-tracker.md`.

### Triage labels

Five canonical labels: `needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix` (all default names). See `docs/agents/triage-labels.md`.

### Domain docs

**Single-context** — one `CONTEXT.md` + `docs/adr/` at repo root. See `docs/agents/domain.md`.
