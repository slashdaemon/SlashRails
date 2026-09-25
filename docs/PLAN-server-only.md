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

### In-game checks with real clients (26.2 dev server, Polymer AutoHost pack required)
- **Stock vanilla 26.2 joins** (Prism `SlashRails-Vanilla-26.2`): gets the pack, no registry or
  payload errors in its log. `/give slashrails:track_smoother` shows the mod's own model (not a
  stick). Right-clicking a rail with it reverts the 80-rail loop, and right-clicking again
  re-smooths it. The action bar reads "Reverted 80 rails to vanilla" / "Smoothed a loop of 80 rails":
  translations come from the pack.
- **What vanilla players see:** the original zigzag rails, as intended. Smoothing shows only in
  how carts move. Riding (third person, every 3 ticks and every tick): the rider follows the
  curve; the cart body snaps to each rail block's line and flips between straight and 45° as it
  crosses rails, as §2 predicted. The owner watched this live; see D1.
- **Modded 26.2 client** (Fabric 0.19.5, Fabric API 0.159, spike jar) on the same server: Polymer
  handshake ("Full sync"), smoothed loop drawn as curves, cart body and rider both follow the
  curve. Same as today.
- **Packet cost of every-tick updates** (`/slashrails-netstat`, one cart on the loop, one watching
  player, 10–12 s windows; counts include every tracked entity): entity-move packets
  (`move_entity_pos` + `_pos_rot` + `entity_position_sync`) went from about 30/s to about 40/s.
  That is ~10 extra small packets per second per cart per watcher while on a curve, ~150 B/s.
- **Bedrock:** not verified with a client. Geyser/Floodgate (the builds on the TBS reset-26.2
  branch) load and answer pings with SlashRails + Polymer present. There is no Bedrock client on
  this PC. The headless client (bedrock-protocol with JS RakNet) times out connecting, and the
  native RakNet backend needs a C++ toolchain that isn't installed.

**Spike verdict: PASS** on joining, the tool, riding and the modded client. Two things are still
open: how the snapped cart model looks (D1) and Bedrock (a real client).

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

### S1 — Decide the product shape  *(done)*
Depends on: S0 pass, owner decisions D1–D6 (decided, §7).
- One jar for 26.2 Fabric with Polymer nested (D5); it runs with or without the mod on clients.
- Cart update policy as a config key, default every tick (D3), replacing the spike command.
- Remove the spike-only commands, trace and packet counter.
- Done: `allowVanillaClients` (default true) and `cartSyncTicks` (default 1) in
  `config/slashrails.properties`, written only by builds that support vanilla clients.

### S2 — Harden the 26.2 Fabric band  *(done except the items marked open)*
Depends on: S1.
- Config: `serverOnlyMode` (allow vanilla clients) and `cartSyncTicks`.
- Done: every SlashRails text carries its English string as fallback (`Texts.tr`, from the
  mod's `en_us.json`), and a client without the server pack keeps the stick's own model
  (`PolymerResourcePackUtils.hasMainPack`), so a server without AutoHost degrades to readable
  English instead of raw keys and a missing-model cube. With vanilla clients allowed the pack is
  marked required.
- Found: AutoHost is **off** by default on a production server (Loom's dev run had it on). A
  server that wants textures for vanilla players must enable it (TBS does, via SlashSlabs' setup).
- Vanilla-client checks (26.2, stock client, subtitles on):
  - With the pack: custom icon, name "Track Smoother", English tooltip. Using it plays the
    sounds (subtitles "Anvil used" on smooth, "Block broken" on revert) and shows the action-bar
    message.
  - Without the pack (AutoHost off): plain stick model, name and tooltip in English (the name
    needed an override of Polymer's final stack, `getPolymerItemStack`: Polymer rebuilds the name
    from the translation key), action-bar text in English.
  - The server logs which path each player takes ("joined without SlashRails: vanilla rails, no
    run data" / "joined with SlashRails").
  - **Open, cosmetic: the arm doesn't swing** when a vanilla player uses the tool. The client holds
    a stick and predicts nothing. A server-side `swing(hand, true)` in `useOn` and Polymer's
    `isPolymerBlockInteraction` hook both failed to animate it, though vanilla `/swing` does.
    Server state at the call was `swinging=false`. Not investigated further.
- Open, optional — tool preview for vanilla clients: particles along the run the tool would smooth,
  since they can't draw the overlay.
- Done: `prodtest.py` checks server-only jars on a real Fabric server (AutoHost enabled and
  required, as TBS runs it): self-test, the server-only log line, and the generated pack holding
  SlashRails' item definition, model, texture and lang. `--jar` tests an unreleased build.
  Result: `SELFTEST PASSED 13/13, server-only OK`.

### S3 — Shared pack with SlashSlabs  *(done)*
Depends on: S2; SlashSlabs on the same server.
- Both mods add assets to Polymer's one generated pack; check the merged pack on a server running
  both (no namespace collisions: `slashrails:*` vs `slashslabs:*`), and one AutoHost config.
- Polymer version alignment: both jars nest Polymer; Fabric Loader keeps the newest nested copy.
  Pin both to the same Polymer build for TBS.
- Done: prodtest server with SlashSlabs 0.2.1 (26.2) beside the SlashRails jar, both nesting
  Polymer 0.17.5+26.2. One pack: `slashrails` 12 files + `slashslabs` 51; SlashRails self-test
  13/13, server-only OK.

### S3b — Bedrock (Geyser)
Depends on: S2; the TBS reset-26.2 pack (Geyser 2.11.3-b1246, Floodgate 2.2.6-b67 — the same builds
the spike loaded).
- Real Bedrock client through Geyser: join, hold the Track Smoother, use it on a rail, ride a
  smoothed run. Bedrock never registers the SlashRails channel, so it gets no payloads; carts
  arrive as ordinary entity moves. Bedrock renders its own cart model, so it may not snap to the
  rails the way the Java model does. Check in-game.
- Add a Rainbow-generated Geyser item mapping for the Track Smoother, built alongside SlashSlabs'
  mappings, so Bedrock players see its texture instead of a stick. SlashRails has no custom
  blocks, so it needs no block mappings.

### S4 — Other bands (only if wanted, D4)
Depends on: S2.
- Fabric bands with a Polymer build for their MC version. Polymer exists for Fabric only, so
  NeoForge/Forge bands stay mod-required.

### S5 — TBS integration
Depends on: S3, the TBS reset on 26.2, owner go-ahead.
- `TBS-server`: SlashRails jar (server-only), config, AutoHost settings shared with SlashSlabs.
- `TBS-mod-strategy.md` / TBS `CLAUDE.md`: SlashRails listed as a Polymer-path server mod, and
  (D6) as an optional TBS-client mod: amend the "StreamCraft is the only cross-side mod" contract
  and pin the same SlashRails version in both packs, bumped together.
- `TBS-client`: SlashRails entry (Modrinth-first per the client pack's policy; the Modrinth
  listing must be public first).
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

## 7. Decisions (decided 2026-09-24)

- **D1 — Snapping cart model on vanilla clients: acceptable for v1.** Vanilla riders glide on the
  curve while the cart body snaps to each rail (up to ~0.5–0.9 blocks off, ~50° p95 yaw error).
- **D2 — Display-entity overlay (§6): parked.** Nothing built; revisit after TBS players have used
  v1. The costed write-up stays in §6.
- **D3 — Cart updates: every tick on smoothed runs, as a config key** (`cartSyncTicks`, default 1;
  3 = vanilla rate). Cost ~10 small packets/s per cart per watching player while on a curve.
- **D4 — Bands: 26.2 Fabric only.** Other Fabric bands can be added later with the `polymer` flag,
  each needing its own Polymer build and a vanilla-client test (S4).
- **D5 — Packaging: Polymer nested in the server-only jars** (polymer-core, polymer-resource-pack,
  polymer-autohost), as SlashSlabs does. Jar ~1 MB; Polymer also loads on modded clients
  (tested harmless).
- **D6 — TBS client pack: yes, SlashRails optional in TBS-client** for curved visuals. Needs a
  TBS contract change (StreamCraft is today the only mod in both packs) and lockstep client/server
  jar versions, like StreamCraft. Folded into S5.
