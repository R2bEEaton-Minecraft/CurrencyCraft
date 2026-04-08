# Multi-Version Build Workflow

This project now supports profile-based builds via `-PmcTarget=<target>`.

## Supported Targets

- `1.20.1` (Forge `47.4.16`, Java `17`)
- `1.20.2` (Forge `48.1.0`, Java `17`)
- `1.20.4` (Forge `49.2.4`, Java `17`)
- `1.20.6` (Forge `50.2.4`, Java `21`)
- `1.21.1` (Forge `52.1.10`, Java `21`)
- `1.21.10` (Forge `60.1.8`, Java `21`)

Run:

```powershell
./gradlew listSupportedMcTargets
```

## Build One Target

```powershell
./gradlew build -PmcTarget=1.20.1
```

Artifacts are named with the MC version and written under:

- `build/libs/<minecraft_version>/`

Example:

- `build/libs/1.20.1/currencycraft-mc1.20.1-<mod_version>.jar`

## Build Matrix

Build all configured targets:

```powershell
./gradlew buildCompatibilityMatrix
```

Or run one matrix task:

```powershell
./gradlew buildMc1_20_1
./gradlew buildMc1_20_2
./gradlew buildMc1_20_4
./gradlew buildMc1_20_6
./gradlew buildMc1_21_1
./gradlew buildMc1_21_10
```

## Data Generation Toggle

By default, `build` depends on `runData`.

Disable for faster compatibility compiles:

```powershell
./gradlew build -PmcTarget=1.20.1 -PskipDataGen=true
```

## Notes

- These build profiles are the scaffolding layer.
- Source compatibility still needs to be ported per target line.
- Java 21 is required for Forge targets `1.20.6+`.
