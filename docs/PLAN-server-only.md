# SlashRails — server-only mode (vanilla clients) — Plan

Goal: SlashRails on a server where clients may be stock vanilla, so it can go into The Block
Survival (TBS). A client with the mod keeps the full experience (curved track models, cart
following the curve, tool preview). A client without it joins and plays normally; its carts ride
the curve, but it sees the original zigzag rails.

TBS rule (`Projects/TBS/CLAUDE.md`, `TBS-mod-strategy.md`): a stock vanilla client gets the full
gameplay; server mods add no client-visible content, or present it as vanilla content through
Polymer. TBS will require the SlashSlabs server resource pack (Polymer AutoHost), so SlashRails'
assets can ride in the same pack.

Branch `server-only`. Scope of the spike: the MC 26.2 Fabric band only.

---

## 1. What changes

| Piece | Today | Server-only mode |
|---|---|---|
| Join | A client without the `slashrails:runs` channel is disconnected | Allowed; it gets no snapshot and no payloads (the existing `clientHasMod` gate already filters every send) |
| Track Smoother | Registered item `slashrails:track_smoother`, synced to clients | Same server item, with a Polymer overlay (`PolymerItem.registerOverlay`): clients get a vanilla stick carrying `item_model = slashrails:track_smoother`; Polymer removes it from registry sync |
| Assets | In the mod jar (client side) | Also in the Polymer server pack (`addModAssets("slashrails")`): model, texture, item definition, lang (chat/tooltip keys are translatable) |
| Recipe | Data-driven, result `slashrails:track_smoother` | Unchanged; Polymer rewrites the result for vanilla clients |
| Cart riding | Server moves carts along the curve | Unchanged. Optional: position packets every tick for carts on smoothed runs (§3) |
| Rails | Modded clients draw the curve instead of the rail models | Vanilla clients see vanilla rails |

Build switch: a band sets `polymer: true` in `ext.rails`; `gradle/fabric-band.gradle` then adds
the `vanilla-polymer` loader variant and nests `polymer-core`, `polymer-resource-pack` and
`polymer-autohost` (the same modules SlashSlabs nests). Every other band gets `vanilla-required`,
which keeps the current behaviour. No shared file is forked.

## 2. Evidence from the 26.2 source (decompiled, Vineflower)

- **The vanilla client does not simulate carts** (`OldMinecartBehavior.tick`, client branch): it
  only runs `InterpolationHandler.interpolate()`, a 3-step lerp toward the last position packet.
  So a vanilla rider follows whatever path the server sends. The owner's theory holds for the
  *rider*.
- **Update rate:** minecarts use the default `updateInterval` 3 (`EntityType.Builder`), so
  `ServerEntity.sendChanges` sends a position every 3 ticks; the client lerps each over 3 ticks,
  i.e. it rides straight chords between samples 3 ticks apart.
- **The vanilla cart *model* does not follow the curve.** `AbstractMinecartRenderer.oldExtractState`
  calls `OldMinecartBehavior.getPos(x, y, z)`, which projects the cart onto the straight line of
  the rail block it is in, and `oldRender` translates the model there and takes the model's yaw
  from the rail's direction (`getPosOffs(±0.3)`). On a smoothed staircase the model therefore
  snaps sideways and turns 45°/90° per rail, while the rider (positioned from the entity, not the
  model) glides along the curve. This is the part of the theory that does not hold.
- Forcing a send every tick: setting `Entity.syncPosition` in the cart's tick makes
  `ServerEntity` send its position that tick (one `move_entity_pos` packet). `needsSync` would
  also work but adds a motion packet.

## 3. Spike results (26.2 Fabric)

### Server side — PASS
- `/slashrails selftest` with the Polymer build: **13/13 PASS**, run three times (plain,
  traced, traced + Geyser/Floodgate loaded). Polymer generated the pack
  (`polymer/resource_pack.zip`); AutoHost enabled.
- Geyser 2.11.3-b1246 + Floodgate 2.2.6-b67 load beside Polymer and SlashRails; Geyser reports
  "Registered 1 custom items" at startup (not yet confirmed to be the Track Smoother) and
  answers RakNet pings.

### Riding — modelled from real server traces
`/slashrails-trace` (spike command) recorded every cart's server position per tick through the
whole self-test and a tight r=4 arc; `scripts/spike_client_model.py` replays it through a port of
the vanilla client's interpolation and cart renderer. Rider turn = heading change of the rider's
motion per tick (the vanilla zigzag is ~75–85°/tick on the same fixtures).

| Fixture | Server (ideal) max / p95 | Vanilla client, every 3 ticks | Vanilla client, every tick | Cart model offset from rider, max / p95 | Model yaw error, p95 |
|---|---|---|---|---|---|
| staircase 1:2 | 2.1 / 2.0 °/t | 6.1 / 4.6 | 2.0 / 1.8 | 0.45 / 0.32 blocks | 28° |
| arc r=16 | 1.3 / 1.3 | 4.0 / 3.7 | 1.3 / 1.3 | 0.81 / 0.49 | 52° |
| loop r=10 | 1.7 / 1.6 | 5.1 / 4.2 | 1.7 / 1.6 | 0.49 / 0.42 | 37° |
| 90° corner | 11.7 / 9.3 | 31.2 / 10.3 | 9.6 / 8.3 | 0.74 / 0.60 | 40° |
| arc r=4 (tightest) | 5.0 / 3.8 | 14.2 / 7.3 | 3.7 / 3.4 | 0.48 / 0.38 | 18° |

- Rider path error stays under 0.07 blocks (every 3 ticks) and under 0.17 blocks (every tick,
  which lags slightly) on every fixture.
- Every 3 ticks: the rider's heading changes in steps of about 3× the ideal rate (chords).
  That is still an order of magnitude smoother than vanilla rails. Every tick: at or below the
  server's own rate.
- The model offset and yaw error do not depend on the update rate: they come from the renderer
  snapping to rails.

### Visual and join checks with real clients — PENDING (needs owner OK to open client windows)
- Stock vanilla 26.2 (Prism `SlashRails-Vanilla-26.2`, created): joins with the required pack;
  Track Smoother shows its texture and name; using it smooths a run; riding a smoothed staircase
  and the r=4 arc at every-3 and every-tick updates; how bad the detached cart model looks.
- Modded 26.2 client (Prism `SlashRails-Fabric-26.2`, created: Fabric 0.19.5 + Fabric API +
  the spike jar): sees the curves and the tool preview, rides as today, holds a working Track
  Smoother.
- Packet cost: `/slashrails-netstat` counts packets per second per type while a player watches
  a looping cart, every 3 ticks vs every tick.
- Bedrock: a real Bedrock client through Geyser (TBS reset-26.2 uses the same Geyser/Floodgate
  builds as the spike). There isn't one on this PC. The headless
  Bedrock client (bedrock-protocol, JS RakNet) can't finish a connection to Geyser, and its
  native RakNet needs a C++ toolchain that isn't installed.

## 4. Pass / kill criteria

- **Pass:** a stock vanilla 26.2 client joins the Polymer build, holds and uses the Track
  Smoother, rides a smoothed run and it looks acceptable to the owner; a modded client on the
  same server sees and rides curves as today; no SlashRails registry entry or payload reaches
  the vanilla client (its log shows no unknown-registry or custom-payload errors; server
  `clientHasMod` false for it); a Bedrock player isn't kicked or crashed.
- **Kill:** vanilla clients can't join, or riding is still too choppy at every-tick updates.
  The detached cart model is judged separately (D2): it doesn't kill the spike, but it may
  make the display-entity work (§6) a requirement rather than an option.

## 5. Steps (dependency-ordered)

### S0 — Feasibility spike (26.2 Fabric)  *(built; client checks pending)*
Depends on: nothing.
- Polymer overlay item, pack assets, join gate, `vanilla-polymer`/`vanilla-required` variants.
- Spike-only tools: `/slashrails-cartsync 1|2|3`, `/slashrails-trace <ticks>`,
  `/slashrails-netstat [reset]`, `scripts/spike_client_model.py`.
- Client checks in §3. **Exit:** pass → S1; kill → stop, document.

### S1 — Decide the product shape
Depends on: S0 pass, owner decisions D1–D4.
- Keep one jar per band that runs either way, or ship a separate server-only artifact.
- Choose the cart update policy (D3) and make it a config key, replacing the spike command.
- Remove the spike-only commands, trace and packet counter.

### S2 — Harden the 26.2 Fabric band
Depends on: S1.
- Config: `serverOnlyMode` (allow vanilla clients) and `cartSyncTicks`.
- Chat/tooltip text for vanilla clients: rely on the pack's lang, or send literal text when a
  client has no pack.
- Track Smoother for vanilla clients: right-click on a rail with a stick representation (no
  client prediction). Check the swing animation and sounds.
- Tool preview for vanilla clients (optional): particles along the run the tool would smooth,
  since they can't draw the overlay.
- Server-only mode also on dedicated servers without the mod on any client: prodtest target
  that joins no client but asserts the join gate and pack contents.
- `prodtest.py` gains a Polymer target (shipped jar on a real Fabric server, AutoHost pack built,
  self-test PASS), like SlashSlabs'.

### S3 — Shared pack with SlashSlabs
Depends on: S2; SlashSlabs on the same server.
- Both mods add assets to Polymer's one generated pack; check the merged pack on a server running
  both (no namespace collisions: `slashrails:*` vs `slashslabs:*`), and one AutoHost config.
- Polymer version alignment: both jars nest Polymer; Fabric Loader keeps the newest nested copy.
  Pin both to the same Polymer build for TBS.

### S4 — Other bands (only if wanted, D4)
Depends on: S2.
- Fabric bands with a Polymer build for their MC version. Polymer exists for Fabric only, so
  NeoForge/Forge bands stay mod-required.

### S5 — TBS integration
Depends on: S3, the TBS reset on 26.2, owner go-ahead.
- `TBS-server`: SlashRails jar (server-only), config, AutoHost settings shared with SlashSlabs.
- `TBS-mod-strategy.md` / TBS `CLAUDE.md`: SlashRails listed as a Polymer-path server mod;
  optional client install for curved visuals (a client-pack entry is possible because the mod is
  harmless on a vanilla server — decide in D5).
- LocalServer rehearsal with the TBS modset, then deploy only with explicit approval.

### S6 — Display-entity overlay (not built; see §6)
Depends on: S5 live and the owner wanting vanilla players to see curves.

## 6. Display-entity overlay (option, not built)

Goal: vanilla players see curved track (and optionally a cart model that follows the curve).

- **Track:** per smoothed run, `item_display` (or `block_display`) entities carrying track-slice
  models from the resource pack, placed and rotated along the curve (e.g. one per 0.5–1 block of
  arc). Polymer's virtual entity API (`polymer-virtual-entity`, element holders attached to
  chunks) sends them only to players who don't have the mod.
- **Hiding the zigzag rails from vanilla clients:** Polymer can rewrite block states per player
  (`PolymerBlock`-style state replacement for vanilla blocks is not a public API; it would need a
  mixin on chunk and block-update packets swapping rails at smoothed positions for air, per
  player). Rails that are hidden still exist server-side, so redstone and cart logic are unchanged.
- **Cart model:** the vanilla renderer snaps the cart to the rail block. Options: send vanilla
  clients the cart as a virtual display entity following the curve, with the real cart hidden
  from them; or leave the snapped model.
- **Costs:**
  - Entities: roughly 1–2 display entities per rail. A 100-rail run is 100–200 entities per
    watching player, sent on chunk load. Display entities are cheap to render but not free on low-end
    clients; Bedrock (Geyser) has only partial display-entity support.
  - Packets: spawn + metadata per entity per player on chunk load; static afterwards.
  - Work: track-slice models (straight, and bends at many angles, or a few segment lengths rotated
    freely), a rail-hiding packet mixin (touches chunk data encoding — the riskiest part),
    invalidation when rails change, and a new prodtest target.
  - Visual mismatch: display entities don't take block light the same way as rails; seams at
    run ends; no powered-rail glow unless modelled per state.

## 7. Decisions needed

- **D1 — Is the detached cart model acceptable for a first version?** Vanilla riders glide on the
  curve while their cart model snaps to each rail up to ~0.5–0.9 blocks away and turns up to
  ~50° (p95) off the direction of travel. Needs an in-game look (§3 pending).
- **D2 — If not acceptable:** accept a vanilla ride where the rider also snaps (i.e. don't smooth
  for vanilla riders — not possible per player, the cart is shared), or fund the §6 cart-model
  part only.
- **D3 — Cart update policy:** vanilla rate (every 3 ticks, rider turns in ~3× steps) or every
  tick on smoothed runs (matches the ideal). Cost to be measured with `/slashrails-netstat`
  (one small position packet per cart per tick per watching player while on a curve).
- **D4 — Which bands get server-only mode:** 26.2 Fabric only (TBS), or every Fabric band.
- **D5 — Packaging:** Polymer nested in every Fabric jar (bigger jar, Polymer loads on modded
  clients too) vs a separate `-server` artifact vs Polymer as an optional dependency the server
  already has (TBS gets it from SlashSlabs; fragile).
- **D6 — TBS client pack:** offer SlashRails in TBS-client for curved visuals, or keep TBS
  vanilla-looking for everyone.
