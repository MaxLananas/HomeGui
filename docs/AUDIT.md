# Compatibility audit

This document records what HomeGui claims to support, why, and what evidence each
claim rests on. It exists because "it compiles" has repeatedly been mistaken for "it
works", and because a mod that lists twelve versions it has never run is worse than
one that lists the version it has.

## Evidence tiers

Every claim in the README sits in exactly one of these tiers. Nothing is promoted
without the evidence the tier demands.

| Tier | Evidence required | Claim wording |
|---|---|---|
| **A. Verified in game** | The smoke test in `SMOKE_TESTS.md` completed by a human on a real client, recorded with the date, loader and Minecraft version | *Tested in game* |
| **B. Built and validated** | CI compiles the module, the 140 core and interface tests pass, and `scripts/validate-artifacts.sh` accepts the JAR | *Built and validated in CI* |
| **C. Compiles only** | The module compiles but the artifact was not validated | Not published, not claimed |
| **D. Not attempted** | Nothing | Listed as unsupported with a reason |

**No version is published at tier C or below.** Publishing is the point at which a
claim becomes public, and a tier-C artifact has no evidence behind it at all.

## Current status

| Target | Loader | Minecraft | Java | Tier | Notes |
|---|---|---|---|---|---|
| `fabric-1.21.11` | Fabric 0.19.3 | 1.21.11 | 21 | **B** | The only artifact this release ships |
| `neoforge-1.21.11` | NeoForge 21.11.x | 1.21.11 | 21 | D | ModDevGradle; not attempted |
| `fabric-1.21.1` | Fabric 0.19.3 | 1.21.1 | 21 | D | Needs the pre-1.21.9 input API (`mc-classic`) |
| `neoforge-1.21.1` | NeoForge 21.1.x | 1.21.1 | 21 | D | |
| `fabric-1.20.1` | Fabric 0.19.3 | 1.20.1 | 17 | D | Last version legacy Forge also covers |
| `forge-1.20.1` | Forge 47.x | 1.20.1 | 17 | D | ForgeGradle 6 needs Gradle 8; Loom needs Gradle 9, so it cannot share one build |
| `fabric-26.2` | Fabric 0.19.3 | 26.2 | 25 | D | Client API unknown; port with `probe.yml` |
| `neoforge-26.2` | NeoForge 26.2.x | 26.2 | 25 | D | |
| Anything 1.16.5 | — | 1.16.5 | 8 | **Excluded** | See below |

### Why 1.16.5 is excluded

1.16.5 is Java 8 and renders through `MatrixStack` with no `GuiGraphics`. The
interface kernel would need a second painter and a second widget bridge, and the
build would need a Java 8 toolchain alongside the others. That is a separate codebase
kept alive in parallel, not a third module in this one, and it would be maintained by
nobody. Excluding it is a decision, not an oversight.

### Why Forge and Loom do not share one build

Loom 1.17 requires Gradle 9. ForgeGradle 6 requires Gradle 8. A single Gradle build
cannot use both, so a Forge target has to be a nested build with its own wrapper.
That is workable but it doubles the CI surface, and it is not worth doing before the
NeoForge targets exist.

## Why untested combinations are not listed

A version list is a promise. A player on 1.21.4 who installs a JAR labelled "1.21.x"
and gets a crash on the main menu has been lied to by a table in a README. The cost
of listing one more row is zero for the maintainer and unbounded for the player, so
rows are added only when the evidence exists.

Compiling proves that the signatures HomeGui calls existed when it was built. It says
nothing about:

- whether the screen opens, or opens over the right screen;
- whether the chat mixin still fires, or fires before the client has a connection;
- whether the keybind registration survived a loader change;
- whether the resource loading order changed so the icon or lang files are missing;
- whether the mixin config is still read at all.

Every one of those has broken real mods across Minecraft updates while the mod
continued to compile.

## What is verified for `fabric-1.21.11`

From CI, on every push:

- `:core:check :ui:check` — **140 tests, 0 failures**. Parsing (EssentialsX and CMI
  formats, multi-message replies, pagination, unicode names, unsafe names, prose that
  must not be read as homes), command construction and injection rejection, storage
  (atomic writes, schema 1→3 and 2→3 migration, corrupt-file recovery, bounded
  imports, per-server isolation), the request state machine (loading, ready, empty,
  timeout, unrecognised, disconnect, server change), teleport confirmation, sorting,
  and the whole interface (layout at several sizes, filtering, pagination and scroll
  clamping, focus traversal, dialogs, every state, every setting, painting of every
  view).
- `:fabric-1.21.11:build` — compiles against the mapped 1.21.11 jar with Loom.
- `scripts/validate-artifacts.sh` — the JAR is `homegui-4.1.0-fabric-1.21.11.jar`,
  non-empty, undamaged, `environment: client`, license `CC-BY-NC-4.0`, depends on
  Minecraft `1.21.11` and Java `>=21`, declares the mixin config and a client
  entrypoint, bundles the core, interface and bridge classes plus the icon and lang
  files, and contains no sources, test classes or development output. Two artifacts
  may not claim the same loader and Minecraft pair.

## What is **not** verified for `fabric-1.21.11`

Stated plainly, because it is the gap that matters:

- **The mod has never been run.** There is no Minecraft client, no display and no
  access to Mojang's or Loom's download hosts in the build environment. Nobody has
  seen the screen open.
- The chat and command mixins are declared and compile, but a mixin that loads is not
  a mixin whose target method still behaves as expected at runtime.
- Keybind registration, resource-pack overrides of the lang files, and the narrator
  path are all unexercised.
- Performance on a real client is unmeasured. The layout allocates on rebuild; the
  rebuild is driven by invalidation rather than per frame, but that reasoning has not
  been measured.

Promoting this target to tier A requires the smoke test in `SMOKE_TESTS.md` to be run
and recorded. Until then the README says "built and validated in CI" and not
"working".

## How a new target is added

1. Copy an artifact module, point it at the new Minecraft and loader versions, and
   add its source sets.
2. Run `.github/workflows/probe.yml` against it to dump the mapped API of the classes
   the bridge touches. Guessing signatures costs a CI round trip per mistake; the
   probe costs one.
3. Add a `mc-<generation>` module if the input or rendering API moved, and a
   `glue/<loader>-<generation>` module for the entrypoint.
4. Add the module to the CI matrix with the JDK that version needs.
5. Make `scripts/validate-artifacts.sh` accept the new name and metadata format.
6. Publish nothing until the smoke test has been run.

## Known limitations

- Realms worlds cannot be told apart from each other, because the client cannot see a
  per-world address. They share one data bucket rather than being folded into the
  singleplayer bucket.
- Destination coordinates are captured from the player's position after the teleport
  is confirmed, so they are accurate to where the player ended up, not to the home's
  stored position.
- A home list longer than 4096 entries is truncated; a single chat line longer than
  2048 characters is ignored. Both bounds exist so a malicious or broken server
  cannot exhaust memory.
- The parser recognises the formats listed in the README. A plugin that prints
  something new is reported as an unrecognised format, which is deliberate: guessing
  would invent homes that do not exist.
