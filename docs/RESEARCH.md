# SlashRails — Research

SlashRails makes a gradual curve built from vanilla rails **look** smooth and **ride** smooth in a vanilla minecart, instead of the staircase zigzag vanilla produces.

This document covers three things: vanilla mechanics, prior art, and the implementation options. Every claim is tagged with where it came from:

- **[code]** read from Mojang-mapped decompiled sources (NeoForge 1.21.1-era and 26.3 builds, both carrying NeoForge patches)
- **[src]** from a cited external source
- **[inference]** reasoning, not directly confirmed
- **[unverified]** stated by a source or from memory, but not checked

---

## 1. The problem

A long, gentle curve built from vanilla rails becomes straight runs joined by pairs of corner rails. It looks like a zigzag, and a cart riding it jerks sideways and swings its heading at every kink.

## 2. Vanilla mechanics

### 2.1 Rail data model

- `RailShape` has 10 values: `NORTH_SOUTH`, `EAST_WEST`, four `ASCENDING_*`, and four corners (`SOUTH_EAST`, `SOUTH_WEST`, `NORTH_WEST`, `NORTH_EAST`). **[code]**
- Each rail block holds exactly one shape. Plain rails use `BlockStateProperties.RAIL_SHAPE`. **[code]**
- Powered and activator rails use `RAIL_SHAPE_STRAIGHT` (`BaseRailBlock(isStraight=true)`), so they can never be corners. **[code]** Detector rails are also straight-only. **[inference]** This follows the same constructor pattern; the file wasn't opened.
- `RailState.connectTo()` / `place()` picks the shape: **[code]**
  - A straight is chosen by which neighbour connects.
  - A corner is chosen when exactly two perpendicular neighbours connect and the rail isn't straight-only.
  - `ASCENDING_*` is chosen when a neighbouring rail sits one block higher.
- Movement geometry comes from the static map `AbstractMinecart.EXITS: Map<RailShape, Pair<Vec3i, Vec3i>>`, which gives two unit exit vectors per shape. **[code]**
  - This means a corner is a **straight line between two edge midpoints**, not an arc.

### 2.2 Legacy movement

This is `AbstractMinecart.moveAlongTrack` on 1.21.1, and `OldMinecartBehavior` on 1.21.2+.

- **Snapping.** Every server tick, the cart's position is projected onto the current block's exit-to-exit line, which erases any sideways offset. Velocity is rotated onto that line, and its magnitude is capped at 2.0. **[code]**
- **Block transitions.** On entering a new block, velocity is re-aimed along the grid axis just crossed, then realigned to the new block's line on the next tick. **[code]**
- **Diagonals.** In an alternating corner staircase, the edge-midpoint lines line up, so the cart travels a truly straight 45° line. **[code]** The Minecraft Wiki agrees. **[src]**
- **Speed.** The movement cap is applied to each axis separately: `move(Mth.clamp(0.75*vx, ±0.4), …)`. **[code]**
  - Diagonals therefore reach about 0.566 blocks/tick (11.3 m/s), against 8 m/s on straights. Corners speed the cart *up*. **[code]** The wiki gives the same 11.314 m/s. **[src]**
  - Friction comes from `applyNaturalSlowdown`: 0.997 when ridden, 0.96 when empty. **[code]**
- **Yaw.** The server derives yaw from each tick's movement, `atan2(zo-z, xo-x)`, with a `flipped` fix for 180° turns. Pitch (`xRot`) is 0. **[code]**
- **Rendering.** `MinecartRenderer` ignores the entity's yaw. It re-projects the interpolated position onto the rail with `getPos()`, and takes yaw and pitch from `getPosOffs(±0.3)`. **[code]**
  - As a result, the model's yaw swings 0°→45° within about 0.6 block at every straight-to-corner join.
- **Client sync.** Position is interpolated linearly between server snapshots, with `lerpTo()` setting `lerpSteps = steps + 2`. **[code]** In practice that's about a 5-tick interpolation. **[inference]**
- **Camera.** The rider's camera never rotates with the cart (MC-15925). **[src]**

### 2.3 Why a gentle curve feels jagged

**[inference]** A 1:N curve is a polyline with 45° kinks. The causes, ranked by how much they contribute:

1. **The model's yaw swings** 0→45→0 within about 1.4 blocks, instead of following the curve's true tangent.
2. **The path jogs sideways** at every kink, because of the snap-to-line projection.
3. **Speed surges** on corner blocks when the cart is at the speed cap.
4. **Rider wobble.** The rider rides the linearly interpolated entity position, which cuts corners differently from the rail-projected model.

Path geometry and yaw snapping are the main culprits. Render interpolation only partly hides them.

### 2.4 The new physics: "Minecart Improvements" experiment

- `AbstractMinecart` chooses `NewMinecartBehavior` when `FeatureFlags.MINECART_IMPROVEMENTS` is on, and `OldMinecartBehavior` otherwise. Both extend `MinecartBehavior`, which defines `tick`, `moveAlongTrack`, `stepAlongTrack` and `getMaxSpeed`. In 26.3 the package is `net.minecraft.world.entity.vehicle.minecart`. **[code]**
- **Stepping.** `moveAlongTrack` loops a `TrackIteration`, stepping block by block toward each exit and carrying `movementLeft` into the next block, so the cart no longer skips rails at high speed. **[code]**
- **Corners.** `adjustToRails` still projects onto the exit-to-exit line. Speed is capped on total magnitude, so the diagonal speed surge is gone. **[code]**
- **Max speed** is `gamerule max_minecart_speed / 20`, halved in water. **[code]** The gamerule was renamed from `minecartMaxSpeed` in 25w44a. **[src]**
- **Sync.** Each step becomes a `MinecartStep(pos, movement, yRot, xRot, weight)`. **[code]**
  - `ServerEntity.handleMinecartPosRot` sends the steps in `ClientboundMoveMinecartPacket`.
  - The client replays them over `POS_ROT_LERP_TICKS = 3`, with rotations interpolated between steps.
- **Rider rotation.** `Minecart.positionRider` rotates the player only if the client option `rotateWithMinecart` is on (it's off by default) *and* the experiment is enabled. **[code]**
- **Status.** Still experimental and off by default as of 26.3. **[code]** [src: wiki Experiments page]
- **Net effect.** **[inference]**
  - *Fixed:* rail skipping, the diagonal speed surge, and step-accurate client replay.
  - *Not fixed:* the geometry. The path is still the same 45° polyline, and yaw still changes at every corner, just smoothed over the replay.
  - Bug MC-275756, "Minecart visually stutters when snapping to diagonal rails", is specific to the experiment and still unresolved. **[unverified]** The tracker page couldn't be loaded.

### 2.5 Vanilla limits a smoothing mod hits

- **One shape per block, and special rails can't curve.** Since each block has one shape and powered, detector and activator rails are straight-only, a smooth curve must be an *overlay* on vanilla shapes, or a new block or state.
- **Snapping is hard.** Each tick re-projects the cart onto the block's line, so an off-grid path needs the per-rail movement step *replaced* while on a curve, not merely tweaked.
- **Sync differs between the two behaviours.**
  - The legacy path relies on the client's linear interpolation. **[inference]**
  - The new path relies on server step lists in `ClientboundMoveMinecartPacket`, which could be fed denser spline samples.
  - Rotations travel as bytes, about 1.4° resolution. **[code]**
- **Tracking.** Minecarts use `clientTrackingRange(8)`. **[code]** The update interval is 3 ticks. **[inference]**
- **Collision.** Rails have no collision box, so the cart's own box and `move()` do the colliding. **[inference]**
- **Chunk loading.** Carts don't load chunks. **[inference]** This wasn't verified in code.
- **Related bugs:** MC-275756, MC-51368 (full-speed carts on diagonal track don't slow down), MC-15925 (camera doesn't turn with the cart).

## 3. Prior art

### 3.1 Vanilla carts on vanilla rails (closest)

- **Smooth Minecarts** (SequentialEntropy) — Fabric, 1.21.2–26.2, MIT, about 5.2K downloads. **[src]**
  - *Approach:* a physics-only change that looks up to 6 rails ahead and averages them to smooth curves, shallow diagonals and slopes. It adds no blocks and can be removed safely.
  - *Self-declared limits:* it requires Minecart Improvements enabled, carts don't collide while on rails, curves become faster than vanilla, and minecart farms can break.
  - *Visuals:* none. The rails still look zigzag.
- **Minecart Improvements** — Mojang's experiment (see §2.4). **Risk:** if it ships for real, the "rides smooth" half of the pitch shrinks.
- **Speed mods** — Audaki Cart Engine, High-Speed Rail (server-only, MIT, about 76K downloads, slows carts on curves), Express Carts. These address speed, not curve appearance. **[src]**
- **Camera-only mods** — Do a Minecart Roll (Fabric 1.21.10–11, MIT; camera banking and anti-shake), Minecart Turning (Fabric/Quilt up to 1.21.1, MIT, 34K; free look while riding), Smooth Movement (CurseForge; smooths lag jitter). **[src]**

### 3.2 Own spline track, still a minecart

- **Splinecart** (FoundationGames) — Fabric 1.21–1.21.3, MIT, 325K downloads. **[src]**
  - *Approach:* the player places oriented "Track Tie" blocks and links them with a Track item; the rail is a curve computed between the ties. It's aimed at roller coasters.
  - *Complaints:* harsh camera snapping (it ships a command to disable rotation for motion sickness), a 90° camera snap when switching rail types, 44 open issues, and many requests for NeoForge and newer versions.
  - The curve math and whether it uses the vanilla cart entity are **[unverified]**.
  - *Forks:* MCoaster (5° steps and banking, 1.21.2–1.21.4), Forkcart, and peterwolf's fork.

### 3.3 Train mods (own track and own vehicles)

| Mod | Loaders / latest MC | License | Scale | Curve method |
|---|---|---|---|---|
| Create | NeoForge + Fabric port / 1.21.1 | Custom (Create Mod License) | 26.1M | Cubic Bezier between two track blocks, min radius 8 |
| Create: Steam 'n' Rails | Fabric/Forge/NeoForge/Quilt / 1.20.1 | LGPL-3.0 | 12.8M | Create's curves, 8 extra track types |
| Immersive Railroading | Forge/NeoForge / 1.21.4 | LGPL-2.1 | 150K | Blueprint track placement **[unverified]** method |
| Minecraft Transit Railway | Fabric/Forge/NeoForge / 1.21.4 | MIT | 2.1M | Connector between rail nodes, 22.5° node steps |
| RealTrainMod | Forge / 1.12.2 | All Rights Reserved | 925K | Bezier |
| Traincraft | Forge / 1.7.10 | All Rights Reserved | 1.4M | Own |
| LargeCurvedRails (AlexIIL) | Forge / 1.8.9–1.12, abandoned | — | — | Own curves |

- **LargeCurvedRails** issue #14 asks for vanilla minecarts to work on its curves. That shows demand going back to 2016.
- **Other rail mods:** Railcraft Reborn (NeoForge 1.21+, custom license, no smooth curves found), Modern Minecarts, More Minecarts and Rails, Improved Rails, Rail Placement Fix. None of them smooth curves.
- **Unrelated:** Automobility is road vehicles, not rails.

### 3.4 Resource packs and datapacks

- **Diagonal Rails / Diagonal Perfect Rails 3D** (MIT, 1.20–1.21.4) retexture the corner rail so a 45° run looks like a straight diagonal. **[src]**
  - A pack changes each block's model on its own, so it can't draw a real arc across many blocks, handle angles other than 45°, or change physics.
- **No datapack approach exists**, beyond the one that just enables Minecart Improvements.

### 3.5 Comparison

| Mod | Vanilla cart | Vanilla rails | Smooth visuals | Smooth ride | Loaders |
|---|---|---|---|---|---|
| Smooth Minecarts | Yes | Yes | No | Yes (experiment required) | Fabric |
| Splinecart | [unverified] | No | Yes | Yes | Fabric |
| Create / S'n'R / IR / MTR | No | No | Yes | Yes | Varies |
| Diagonal Perfect Rails | Yes | Yes | 45° only | No | Resource pack |
| Minecart Improvements | Yes | Yes | No | Partial | Vanilla (experimental) |
| **SlashRails (target)** | **Yes** | **Yes** | **Yes** | **Yes** | **Fabric + NeoForge** |

### 3.6 The gap

No existing mod takes **existing vanilla rail staircases**, figures out the curve they approximate, **renders them as that smooth curve**, and **moves vanilla minecarts along the same curve**. The target also has these properties, none of which existing options combine:

- no new blocks, so it's safe to remove and works in existing worlds
- normal collision
- no experiment required
- Fabric and NeoForge, on 1.21.x and 26.x

**Durability.** The visual half is the durable differentiator: it survives even if Mojang ships Minecart Improvements. Camera banking, as in Do a Minecart Roll, is a natural extra.

## 4. Implementation options

### 4.1 Reference: Create's track system

Read from the Create source, `mc1.21.1/dev` branch. **[src]**

- **Curve data.** `BezierConnection` stores a cubic Bezier: two endpoints, axis directions that set the control points, and face normals.
- **Length and parameterisation.**
  - Length is computed by sampling, with segment count `(int)(length*2)`.
  - `getStepLUT()` maps segment index to parameter t by cumulative distance, and `incrementT()` uses the derivative to advance by a distance.
  - In other words, it's arc-length reparameterisation by lookup table.
- **Caching.** Per-segment poses and lights are cached lazily (`getBakedSegments()`, `getBakedGirders()`). The curve serialises to NBT and to `FriendlyByteBuf`.
- **Storage.** `TrackBlockEntity` holds `Map<BlockPos, BezierConnection>`, stored at both ends with an `isPrimary()` flag. Only the primary end manages the intermediate `FakeTrackBlock`s, which have no collision or occlusion.
- **Rendering.** `getRenderBoundingBox()` returns `AABB.INFINITE`. The package has both `TrackRenderer` and `TrackVisual`; `TrackVisual` is probably the Flywheel instanced path **[unverified]**.
- **Removal.** `manageFakeTracksAlong(connection, true)` deletes the fake tracks, and `removeInboundConnections()` cleans up the far end.
- **Vehicles.** Trains don't use vanilla carts. They move `TravellingPoint`s along a track graph.

This is for reference only. Create's license is custom, so **do not copy code**.

### 4.2 Curve math

- **Cubic Bezier or Hermite, reparameterised by arc length through a lookup table.** This is proven by Create. Hermite through fitted endpoint tangents gives smooth heading (G1 continuity), which is all yaw needs. **[judgment]**
- **Clothoid (Euler spiral).** Real railways use it as a transition curve because curvature changes linearly with distance, which avoids sudden jumps in sideways acceleration. **[src]** Its coordinates are Fresnel integrals with no closed form; the historical workaround is a cubic approximation. **[src]**
  - At cart speeds (at most about 8 m/s), a clothoid isn't worth numerical integration up front. A "clothoid-ish" option (tuned Bezier control points or a precomputed Fresnel table) can come later if requested. **[judgment]**
- **Slopes.** Parameterise height separately against arc length, for example with a smoothstep across the ascending run. Vanilla slopes are exactly 1:1, so smoothing the vertical corners gives most of the visual gain. **[judgment]**

### 4.3 Option A — Overlay (recommended)

Vanilla rail blocks stay the source of truth. The mod detects curves and renders and rides them smoothly. **[judgment]**

- **Detection.**
  - Walk connected rails, collect the rail centres, and reject runs that include junctions.
  - Fit arc or spline segments within a tolerance.
  - Detection is cheap. The hard parts are ambiguity (S-curves, zigzags built on purpose) and the need for an opt-out.
- **Storage.** Keep a compact segment descriptor (endpoints plus fitted control points, keyed by start position) in a chunk attachment. Rebuild it on neighbour and block-update events; don't recompute per tick or per frame.
  - Recomputing on the client alone risks the client and server disagreeing on the path.
  - Attachment sync is native on Fabric 1.21.4+ **[src]** and NeoForge 26.x **[src]**. On 1.21.1, use a custom packet: Fabric has no backport **[inference]**, and NeoForge 21.1 sync is **[unverified]**.
- **Rendering.** Suppress the vanilla rail quads and emit the curve mesh into chunk geometry.
  - *Fabric:* a Fabric Rendering API (FRAPI) model wrapping the vanilla rail model through a model-loading plugin, which reads render data and emits the curve slice covering the block. Sodium 0.6+ supports FRAPI natively (Indium is no longer needed). **[src]**
  - *NeoForge:* `AddSectionGeometryEvent` fires on the main thread while its renderer runs on the rebuild thread, so data must be copied during the event. **[src]** `ModelData` on a wrapped model is an alternative.
  - *Why chunk geometry:* it costs almost nothing, batches with the terrain, and is safe with Iris shaders. A block entity renderer costs something every frame and is only suitable for a proof of concept. **[judgment]**
- **Riding.** While the cart's rail position is inside a registered segment, set position and yaw from the curve at distance s. Advance s by the speed vanilla already computed, so powered-rail boosts, braking and gravity still apply. Everywhere else, vanilla runs unchanged. **[judgment]**
  - *1.21.1 hook:* mixin into `AbstractMinecart.moveAlongTrack`.
  - *1.21.2+ hook:* mixin into both `OldMinecartBehavior` and `NewMinecartBehavior` at their per-rail step. The old behaviour is the one most players will actually hit.
- **Redstone.** Powered, detector and activator rails keep working because they're still real blocks that the cart occupies.
  - The risk is that a curved path cuts a corner and skips a detector rail's block. Clamp the path so the cart's block position still visits every rail in the run.
- **Multiplayer.** The server is authoritative, and yaw is computed from the curve on both sides. Use the synced segment for client yaw, not the interpolated movement delta.
  - On the new behaviour, the server can feed denser curve samples into the minecart step packet. **[inference]**
- **Block broken.** Invalidate the segment. The staircase reappears instantly, and carts fall back to vanilla.
- **Performance.** Detection is event-driven and linear in segment length. The cart override is one table lookup per tick. The chunk mesh costs next to nothing.

### 4.4 Option B — Explicit placement (Create-style)

- The player clicks a start and end point with tangents, and the mod stores a Bezier connection with placeholder blocks for collision and drops.
- *Pros:* freeform curves at any angle, and explicit intent, so nothing needs detecting.
- *Cons:*
  - placeholder blocks
  - paired primary/secondary connection bookkeeping
  - a placement-preview UI
  - more chance of conflicting with Create
  - vanilla redstone rails can't sit inside a curve without curve-aware powered and detector segments
- *Verdict:* a later power tool, not the first release. **[judgment]**

### 4.5 Option C — Custom vehicle vs. mixin

- A custom cart entity gives clean physics, but it breaks farms, hopper carts and every mod that expects vanilla carts, which is wrong for this product.
- **Mixin into vanilla movement only while on a curve segment.** **[judgment]**

### 4.6 Differences between loaders and versions

| Concern | 1.21.1 | 1.21.2+ | 26.x |
|---|---|---|---|
| Cart movement hook | `AbstractMinecart.moveAlongTrack` | `OldMinecartBehavior` + `NewMinecartBehavior` | Same split, package `...vehicle.minecart` |
| Chunk attachment sync | Custom packet | Fabric native from 1.21.4 | NeoForge native (docs) |
| Chunk geometry | FRAPI (Fabric) / `AddSectionGeometryEvent` (NeoForge) | Same; FRAPI model API reworked around 1.21.5 **[unverified]** | Unobfuscated mappings; block entity renderers moved to render-state extraction **[unverified]**, which favours chunk geometry |

Verify each of these per band before building, using the minecraft-mod-expert skill.

## 5. Smallest version that proves the idea

1. Fabric on 1.21.1, with one mod covering client and server.
2. Detect flat 45° staircases and single-radius arcs only.
3. Fit a Hermite curve and keep it in a world-level map, rebuilt on block updates. Nothing is saved to disk yet.
4. Render with a simple renderer (a helper block entity renderer or a world-render hook), and suppress the vanilla rail model.
5. Mixin into `moveAlongTrack` so the cart follows the curve with continuous yaw.

That proves both "looks smooth" and "rides smooth". Then, in order:

1. FRAPI and NeoForge chunk geometry
2. chunk attachments and persistence
3. slopes
4. redstone-rail tests
5. `OldMinecartBehavior` / `NewMinecartBehavior` hooks
6. 26.x bands
7. camera banking as an option

## 6. Risks and open questions

- **Mojang ships Minecart Improvements by default.** This reduces the ride differentiator; the visual one remains.
- **Curve detection ambiguity.** S-curves and zigzags built on purpose need an opt-out, via an item, a per-segment toggle or config.
- **Detector-rail skipping.** The path must visit every rail block (§4.3).
- **Two movement behaviours to hook**, plus API differences per band (§4.6).
- **Interaction with speed mods** such as Smooth Minecarts, Audaki Cart Engine and High-Speed Rail, which modify the same movement methods.

## 7. Licensing and naming

- **Name.** Renamed from SlashTracks to SlashRails. The earlier checks (no "SlashTracks" on Modrinth, no "Smooth Tracks" on CurseForge) do not cover the new name. **Not checked:** Modrinth and CurseForge for "SlashRails", and a trademark search.
  - Nearby names that could cause confusion: "Tracks" (a walking-path mod), "Track API" (Immersive Railroading's dependency), "Trackwork", "Smooth Minecarts".
- **Code reuse.**
  - *Don't copy:* Create (custom license), Railcraft Reborn (custom license), RTM and Traincraft (All Rights Reserved).
  - *Copyleft if copied:* Steam 'n' Rails (LGPL-3.0) and Immersive Railroading (LGPL-2.1).
  - *Reusable with attribution:* Smooth Minecarts, Splinecart and MTR are MIT.
  - Writing our own curve fitting avoids all of this.
- **Mojang commercial guidelines.** A free mod plus a paid service fits, as with StreamCraft. Keep "Minecraft" out of the product name.

## Sources

- Minecraft Wiki: [Rail](https://minecraft.wiki/w/Rail), [Minecart](https://minecraft.wiki/w/Minecart), [Minecart Improvements](https://minecraft.wiki/w/Minecart_Improvements), [Experiments](https://minecraft.wiki/w/Experiments), [Java Edition 1.21.2](https://minecraft.wiki/w/Java_Edition_1.21.2)
- Mojang bugs: [MC-275756](https://bugs-legacy.mojang.com/browse/MC-275756), [MC-51368](https://bugs.mojang.com/browse/MC-51368), [MC-15925](https://bugs.mojang.com/browse/MC-15925)
- [fastcarts](https://github.com/DeeKahy/fastcarts) (Old/New behaviour split on 26.2)
- [Smooth Minecarts](https://modrinth.com/mod/smooth-minecarts) · [source](https://github.com/SequentialEntropy/Smooth-Minecarts)
- [Splinecart](https://modrinth.com/mod/splinecart) · [issues](https://github.com/FoundationGames/Splinecart/issues) · [MCoaster](https://modrinth.com/mod/mcoaster)
- [Audaki Cart Engine](https://modrinth.com/mod/audaki-cart-engine) · [High-Speed Rail](https://modrinth.com/mod/highspeed-rail) · [Express Carts](https://modrinth.com/project/Xog4t7Fl)
- [Do a Minecart Roll](https://modrinth.com/project/xlwewwYA) · [Minecart Turning](https://modrinth.com/mod/minecart-turning) · [Smooth Movement](https://www.curseforge.com/minecraft/mc-mods/smooth-movement)
- [Create](https://github.com/Creators-of-Create/Create) · [track package](https://github.com/Creators-of-Create/Create/tree/mc1.21.1/dev/src/main/java/com/simibubi/create/content/trains/track) · [train carriage system (DeepWiki)](https://deepwiki.com/Creators-of-Create/Create/3.7-train-carriage-system) · [Train Track wiki](https://create.fandom.com/wiki/Train_Track)
- [Create: Steam 'n' Rails](https://modrinth.com/mod/create-steam-n-rails) · [Immersive Railroading](https://modrinth.com/mod/immersive-railroading) ([source](https://github.com/TeamOpenIndustry/ImmersiveRailroading)) · [MTR rails wiki](https://wiki.minecrafttransitrailway.com/mtr:rails)
- [RealTrainMod](https://www.curseforge.com/minecraft/mc-mods/realtrainmod) · [Traincraft](https://www.curseforge.com/minecraft/mc-mods/traincraft) · [Railcraft Reborn](https://www.curseforge.com/minecraft/mc-mods/railcraft-reborn) · [LargeCurvedRails #14](https://github.com/AlexIIL/LargeCurvedRails/issues/14)
- [Diagonal Perfect Rails 3D](https://modrinth.com/resourcepack/diagonal-perfect-rails-3d) · [source](https://github.com/GrakePch/DiagonalPerfectRails)
- [Track transition curve (Wikipedia)](https://en.wikipedia.org/wiki/Track_transition_curve)
- [Sodium 0.6.0](https://www.curseforge.com/minecraft/mc-mods/sodium/files/5909714/dependencies) · [Indium](https://github.com/comp500/Indium)
- [NeoForge AddSectionGeometryEvent](https://nekoyue.github.io/ForgeJavaDocs-NG/javadoc/1.21.x-neoforge/net/neoforged/neoforge/client/event/AddSectionGeometryEvent.html) · [NeoForge data attachments](https://docs.neoforged.net/docs/datastorage/attachments/)
- [Fabric 1.21.4 announcement](https://fabricmc.net/2024/12/02/1214.html) · [Fabric data attachments](https://docs.fabricmc.net/develop/data-attachments)
