# CLAUDE.md — SlashTracks

SlashTracks turns zigzag vanilla rail staircases into smooth curves — smooth to look at and smooth
to ride in a **vanilla** minecart. A player uses the **Track Smoother** item on a rail; the mod
fits a curve over that run of rails, draws the curve instead of the rail models, and moves carts
along it. The rails stay real vanilla blocks (redstone, powered/detector/activator rails keep
working; removing the mod leaves the world intact). Design rationale and prior art:
`docs/RESEARCH.md`.

## Layout

| Path | What |
|---|---|
| `core/` | Pure Java 21, no Minecraft. Curve fitting (`CurveFitter`), arc-length curve (`SmoothCurve`). JUnit suite. |
| `common-mc/src/main` | Shared MC 1.21.1 code (Mojang names): run detection, `SmoothRunRegistry` (SavedData), `CurveRide` physics, mixins, payloads, item, `/slashtracks` command, self-test. |
| `common-mc/src/client` | Client: `ClientRuns` index, `TrackMesh` geometry, `CurveOverlay`, cart-render mixin. |
| `loader-fabric/`, `loader-neoforge/` | Loader bridges (`Platform` impl, registration, events, the rail model wrapper). |
| `versions/1.21.1-fabric/`, `versions/1.21.1-neoforge/` | The Gradle band projects. They compile the shared trees in via `srcDirs` (StreamCraft pattern) — the shared trees are not Gradle projects. |

## Build / test

JDK 21 (Prism `java-runtime-delta`, see `Projects/CLAUDE.md`). Dependencies are cached, so
`--offline` works.

```bash
export JAVA_HOME="/c/Users/slash/AppData/Roaming/PrismLauncher/java/java-runtime-delta"
./gradlew --offline :core:test          # curve maths (prints a smoothness table)
./gradlew --offline buildAll            # core tests + both bands; jars in build/release/
scripts/selftest.sh fabric|neoforge     # boots a dev server, runs /slashtracks selftest over RCON
```

`scripts/selftest.sh` expects `versions/1.21.1-<loader>/run/server.properties` with RCON on
(fabric 25591, neoforge 25593, password `slashtracks`; flat world, offline mode). The run dirs
are gitignored. Run the script as a **background** task — a dev server started from a foreground
tool call has been killed mid-test by the environment.

`/slashtracks selftest` rides a loaded cart over each fixture on vanilla rails and again smoothed,
and fails on derails, not finishing, a turn-per-tick above the fixture's limit, or a detector lamp
that never lights. `/slashtracks testtrack <kind> [size] [smooth]` builds one fixture by hand;
`/slashtracks probe` measures a ride you're on.

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
  maths, `Math.sqrt` only). Only rail positions, exits and tight flags go over the wire.
- **Riding.** `AbstractMinecartMixin` wraps the `moveAlongTrack` / `comeOffTrack` calls in `tick()`.
  On a smoothed run `CurveRide.step` mirrors vanilla physics along the curve tangent; the rail that
  owns the cart's arc length supplies powered/detector/activator behaviour.
- **Rendering.** Chunk geometry, not a BER: Fabric wraps rail models (FRAPI `emitBlockQuads`);
  NeoForge wraps them (`BakedModelWrapper` + `ModelData` from `getModelData`, which the NeoForge
  section compiler calls for every block). The cart model follows the curve because
  `MinecartRenderer` positions it via `getPos`/`getPosOffs`, which the client mixin answers from
  the curve.
- **Invalidation.** `LevelChunkMixin` notices changes at smoothed rail positions; a rail that is
  broken or re-shaped reverts its whole run (server and clients).

## Scope of v0.1

Fabric + NeoForge on 1.21.1, flat runs only (runs stop at slopes), opt-in tool, mod required on both
sides. Later: slope smoothing, 1.21.2+/26.x bands (`OldMinecartBehavior`/`NewMinecartBehavior`
hooks), per-chunk sync, camera follow.

## Rules

Follow `Projects/CLAUDE.md`: no `Co-Authored-By: Claude` trailer, never publish or deploy without
explicit approval, release notes need owner sign-off.
