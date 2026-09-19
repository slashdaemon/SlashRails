# CLAUDE.md — SlashRails

SlashRails turns zigzag vanilla rail staircases into smooth curves — smooth to look at and smooth
to ride in a **vanilla** minecart. A player uses the **Track Smoother** item on a rail; the mod
fits a curve over that run of rails, draws the curve instead of the rail models, and moves carts
along it. The rails stay real vanilla blocks (redstone, powered/detector/activator rails keep
working; removing the mod leaves the world intact). Design rationale and prior art:
`docs/RESEARCH.md`.

It ships for **every Minecraft version from 1.20.1 to 26.3** on Fabric and NeoForge, plus
MinecraftForge 1.20.1 (that jar also runs on NeoForge 1.20.1): 25 jars from one source tree.

## Layout

One shared source tree, composed per band. **There are no per-band copies of the mod logic** — a
fix is written once.

| Path | What |
|---|---|
| `core/` | Pure Java (17-compatible), no Minecraft. Curve fitting (`CurveFitter`), arc-length curve (`SmoothCurve`). JUnit suite. |
| `common-mc/src/main` | All version-neutral mod logic (Mojang names): run detection, `SmoothRunRegistry`, `CurveRide` physics, the wire format (`RunWire`), item, `/slashrails` command, self-test. |
| `common-mc/src/client` | Client: `ClientRuns` index, `TrackMesh` geometry, `CartPose`, `CurveOverlay` (what to draw), `VisualTest`. |
| `common-mc/src/devtools` | Store-gallery demo recorder. Compiled into the primary band (1.21.1 Fabric) only. |
| `compat/<axis>-<variant>/` | The calls that differ between Minecraft versions, a few dozen lines each: `main/`, `client/`, `resources/`, plus `mixins.txt` / `client-mixins.txt` naming the mixins it adds. |
| `loader-fabric/`, `loader-neoforge/`, `loader-forge/` | Loader bridges (`Platform` impl, registration, events). `variants/<axis>-<variant>/` hold loader-API differences (rail-model wrappers, overlay hooks, networking). |
| `gradle/band-sources.gradle` | Composes a band: shared trees + its variants, through a rename pass (`gradle/renames.gradle`), and writes the mixin lists. |
| `gradle/{fabric,neoforge,forge}-band.gradle` | Per-loader band composers (Loom / ModDevGradle / MDG legacyforge). |
| `versions/<mc>-<loader>/` | One band = `build.gradle` (its variant list) + `gradle.properties` (coordinates). |
| `versions/26.x/` | Nested builds for MC 26.x (Gradle 9, JDK 25, Loom's non-remapping plugin): `band-fabric/` + `band-neoforge/`, same composers. |

A band's whole build file is its variant list:

```gradle
plugins { id "fabric-loom" }
ext.rails = [
        compat : ["cart-behavior", "chunk-flags", ...],   // compat/<name>
        loader : ["model-blockstate", "overlay-none", ...], // loader-fabric/variants/<name>
        renames: ["getvalue", "ride3", "mc1211"],          // gradle/renames.gradle sets
]
apply from: "${rootDir}/gradle/fabric-band.gradle"
```

**Renames vs variants.** Shared sources pass through `gradle/renames.gradle` before compiling. Use a
rename set only for a pure rename (Mojang moved `ResourceLocation` to `Identifier`, a package moved,
a method was renamed with the same meaning). Anything that changes signatures or behaviour gets a
`compat/` (or `loader-*/variants/`) directory holding just the differing calls. A variant must
never shadow a shared file — the source sync fails on duplicates.

## Bands

| Axis | Variants (MC versions) |
|---|---|
| cart | `cart-legacy` ≤1.21.1 (physics on `AbstractMinecart`) · `cart-behavior` 1.21.2+ (`OldMinecartBehavior`; the experimental `NewMinecartBehavior` is left to vanilla) |
| tick hook | `tick-wrap-legacy` (MixinExtras) · `tick-redirect-legacy` (Forge 1.20.1 has no MixinExtras); 1.21.2+ lives in `cart-behavior` |
| chunk | `chunk-moving` ≤1.21.4 (`boolean isMoving`) · `chunk-flags` 1.21.5+ (`int flags`) |
| item | `item-plain` ≤1.21.1 · `item-setid` 1.21.2+ (`Properties.setId`) |
| recipe | `recipe-1201` · `recipe-1205` (`recipes/`, result `id`) · `recipe-121` (`recipe/`) · `recipe-1212` (string ingredients) |
| nbt / store | `nbt-plain` ≤1.21.4 · `nbt-optional` 1.21.5+; `store-legacy` 1.20.1 · `store-factory` 1.20.5–1.21.4 · `store-codec` 1.21.5+ with `savedtype-string` ≤1.21.11 / `savedtype-id` 26.1+ |
| ids | `ids-ctor` ≤1.20.6 · `ids-factory` 1.21+ |
| sprites | `sprite-atlasfn` ≤1.21.8 · `sprite-atlasmanager` 1.21.9+ |
| tooltip | `tooltip-level` 1.20.1 · `tooltip-context` 1.20.5–1.21.4 · `tooltip-display` 1.21.5+ |
| lines | `lines-vertex` 1.20.1 · `lines-vertexpose` 1.20.5–1.20.6 · `lines-buffer` 1.21–1.21.10 · `lines-gizmo` 1.21.11+ (vanilla gizmos via a `DebugRenderer.emitGizmos` mixin, both loaders) |
| misc | `shot-plain` ≤1.21.5 / `shot-scale` 1.21.6+; `perms-level` ≤1.21.10 / `perms-set` 1.21.11+; `net-bytebuf` 1.20.1 / `net-payload` 1.20.5+ |

Loader variants — Fabric: rail model `model-forwarding-legacy` ≤1.20.6 · `model-forwarding` 1.21–1.21.3 ·
`model-delegate` 1.21.4 · `model-blockstate` 1.21.5+; overlay hook `overlay-events` ≤1.21.8 ·
`overlay-debugmixin` 1.21.9–1.21.10 (Fabric API has no world render events there) · `overlay-none`
1.21.11+ (gizmos). NeoForge: rail hooks `cartext-minecart` ≤21.1 · `cartext-rail` 21.2–21.11 ·
`cartext-none` 26.1+; model `model-bakedwrapper-legacy` 20.6 · `model-bakedwrapper` 21.1–21.3 ·
`model-delegate` 21.4 · `model-blockstate` 21.5–21.11 · `model-blockstate26` 26.x; overlay
`overlay-stage` ≤21.5 · `overlay-afterparticles` 21.6–21.10 · `overlay-none` 21.11+.

Coordinates: `versions/*/gradle.properties`. Fabric Loader: 0.16.10 by default, 0.18.6 on the
1.21.6+ bands (Fabric API there needs ≥0.16.13 / 0.17.0 / 0.17.3). NeoForge 21.6/21.7/21.9 have no
stable builds, so 1.21.6–1.21.8 and 1.21.9–1.21.10 ride the 21.8 and 21.10 builds; NeoForge 26.3 is
beta-only (published as beta). The Forge band compiles against Forge **47.1.3** so the same jar
serves NeoForge 1.20.1 — do not bump it.

### Adding a band

1. `include` it in `settings.gradle`, add it to `bands` in `build.gradle`.
2. `versions/<mc>-<loader>/gradle.properties` + a `build.gradle` copied from the nearest band.
3. Build. If it compiles, run the self-test. If not, the compiler is naming a new drift: add a
   rename (pure renames) or a variant directory (anything else), and set it on every band that
   needs it. Read vanilla source for the version first (Loom's mapped jars in
   `~/.gradle/caches/fabric-loom/minecraftMaven` decompile with the cached Vineflower).

## Build / test

JDK 21 (Prism `java-runtime-delta`, see `Projects/CLAUDE.md`); the 26.x nested builds need JDK 25
(`java-runtime-epsilon`, or set `JAVA25_HOME`). NeoForge 1.20.6/1.21.3 need one online resolve per
checkout before `--offline` works.

```bash
export JAVA_HOME="/c/Users/slash/AppData/Roaming/PrismLauncher/java/java-runtime-delta"
./gradlew buildAll                          # core tests + all 25 jars -> build/release/
./gradlew :versions:1.21.5-fabric:build     # one band
./gradlew build263                          # one nested 26.x build (both loaders)
scripts/selftest.sh 1.21.5-fabric           # dev server + /slashrails selftest over RCON
scripts/selftest.sh 26.2-neoforge           # nested bands work the same way
```

`scripts/selftest.sh <band>` creates `versions/<band>/run/` from `scripts/dev-server.properties`
(RCON 25591, password `slashrails`, flat world, `pause-when-empty-seconds=0` — 1.21.2+ servers pause
when empty and stall the test otherwise). **Never run a Gradle build in this checkout while a
self-test runs**: a concurrent build killed the dev server's daemon mid-test. Run self-tests as
background tasks, one at a time (they share the port); compile experiments meanwhile belong in a
git worktree.

`/slashrails selftest` rides a loaded cart over each fixture on vanilla rails and again smoothed,
and fails on derails, not finishing, a turn-per-tick above the fixture's limit, or a detector lamp
that never lights. `/slashrails testtrack <kind> [size] [smooth]` builds one fixture by hand;
`/slashrails probe` measures a ride you're on. The self-test covers the server side only; rendering
(rail models, cart pose, overlay) needs a client (`runVisualTest`).

`scripts/persist-test.sh fabric|neoforge` smooths a run, restarts the server and checks it is
still there. `runVisualTest` (every band) opens a real client window and saves scripted
screenshots to `run/screenshots/slashrails-*.png`; it needs `run/saves/visual-world` (copy a
self-test world) and `pauseOnLostFocus:false` in `run/options.txt`.

`scripts/mp-test.sh` (Fabric, opens a client window) joins the dev server as `SlashTester` from
`versions/1.21.1-fabric/run-client` and checks the join snapshot, a live add and a live remove.
Compatibility runs: add `-Psodium=mc1.21.1-0.6.13-fabric -Piris=1.8.8+1.21.1-fabric` (or the
`-neoforge` Sodium build) to `runVisualTest`; Sodium on NeoForge needs NeoForge ≥ 21.1.115, and
Sodium 0.8.x can't load under this Loom (1.13) in dev.

**Store-gallery demo clips:** `scripts/record-demos.sh [scene…]` (scenes `click`, `ride-vanilla`,
`ride-smooth`, `preview`; opens a 1920x1080 client window per scene), then `scripts/make-gallery.sh`
→ `build/gallery/` (WebP + GIF ≤ 5 MiB for Modrinth's gallery limit, MP4 masters, before/after
stills). Scenes live in `common-mc/src/devtools/.../demo/DemoScenes.java` and are inert unless
`-Dslashrails.demo` is set. Recording runs the game at 0.2× via `/tick rate` and assembles frames on
game time, so the video plays at true speed with every frame freshly rendered. Needs the portable
ffmpeg at `C:\Users\slash\tools\ffmpeg` (or set `FFMPEG=`). The stage world is
`versions/1.21.1-fabric/run-demo/stage-world` (gitignored); each take starts from a copy.

**Shared machine:** other sessions drive real keyboard/mouse into Minecraft clients (StreamCraft
testkit). Ask before opening a client window — focus steals break their runs. The testkit also
force-kills every `java` whose command line contains `nogui`, so the dev servers here run with
`-Djava.awt.headless=true` (and Loom's `serverWithGui()`) instead of `nogui`.

**Publishing:** `scripts/publish-{curseforge,modrinth}.py --version <v> [--dry-run]` pick up every
jar in `build/release/`; their `BAND_GAME_VERSIONS` table says which MC versions each jar claims.
The Forge jar is tagged Forge + NeoForge.

## How it works (load-bearing details)

- **Fitting smooths the vanilla cart path, not the rail centres.** Two rails of a staircase step sit
  side by side; anchoring to centres forces a jog. The path (edge midpoint to edge midpoint) is
  resampled every 0.5 blocks, Whittaker-smoothed (curvature penalty, solved by CG), and kept within
  0.55 blocks of the path (re-weight + clamp). Open ends are pinned to the end rails' outer edge
  midpoints with tangents along the vanilla rail axis, so entry/exit is seamless.
- **Tight rails.** A 0.98-wide cart rides dead centre on vanilla rails; a curve that swings sideways
  would scrape walls, tunnels, a lamp beside the track. `Clearance` marks rails with any colliding
  block around them (and their neighbours) as tight; the curve keeps to the vanilla line there.
- **Determinism.** Server and client each fit the curve from the same synced rail list (plain double
  maths, `Math.sqrt` only). Only rail positions, exits and tight flags go over the wire (`RunWire`).
- **Riding.** The cart's `tick` (≤1.21.1) or `OldMinecartBehavior#tick` (1.21.2+) calls to
  `moveAlongTrack` / `comeOffTrack` are wrapped. On a smoothed run `CurveRide.step` mirrors vanilla
  physics along the curve tangent; the rail that owns the cart's arc length supplies
  powered/detector/activator behaviour. `Carts` (per cart variant) holds the vanilla internals.
- **Rendering.** Chunk geometry, not a BER: each loader wraps the rail block models (Fabric FRAPI,
  NeoForge/Forge model wrappers) and emits the smoothed slice for rails on a run. On 1.21.5+ Fabric
  models must return no geometry key for smoothed rails, or a cache would share their quads. The
  cart model follows the curve because the minecart renderer positions it via `getPos`/`getPosOffs`
  (on the cart ≤1.21.1, on `OldMinecartBehavior` after), which the client mixin answers from the
  curve (`CartPose`).
- **Overlay.** `CurveOverlay` decides which curves to show (tool preview, debug); `CurveLines` draws
  them into a buffer source (≤1.21.10), and from 1.21.11 a `DebugRenderer.emitGizmos` mixin emits
  them as vanilla gizmos — the only line API that survives 26.2's renderer rewrite.
- **Invalidation.** `LevelChunkMixin` notices changes at smoothed rail positions; a rail that is
  broken or re-shaped reverts its whole run (server and clients).

## Scope

Flat runs only (runs stop at slopes), opt-in tool, mod required on both sides. NeoForge 26.1+
removed the rail-pass and rail-speed hooks, so modded rails' custom behaviour does not apply on
smoothed runs there. Later: slope smoothing, per-chunk sync, camera follow.

## Rules

Follow `Projects/CLAUDE.md`: no `Co-Authored-By: Claude` trailer, never publish or deploy without
explicit approval, release notes need owner sign-off.
