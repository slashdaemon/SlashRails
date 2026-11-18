# Changelog

## 0.3.1 — unreleased

Fixes from the CityWorld author's report (CityWorld smooths its subway bends through SlashRails).

- Modded carts with their own rail physics no longer stall on smoothed track. Choo Choo's
  locomotive applies its pull inside its own rail movement, which a smoothed run skipped; such carts
  (an overridden `moveAlongTrack` or NeoForge/Forge `moveMinecartOnRail`, or from 1.21.2 a
  non-vanilla minecart behaviour) now run their own physics on the rails under a smoothed run, so
  their engines, couplings and derail checks see real rails. Clients draw them on the curve, at the
  curve's angle: the client-side cart is moved onto the curve each tick and its rail pose comes from
  the curve.
- Smoothing never loads or generates a chunk. The clearance check read one block past a run that
  ended at the edge of the loaded area, which loaded the next chunk on the server thread (pauses of
  10–15 s in a new world). A neighbour in an unloaded chunk now counts as blocked, so the curve keeps
  to the vanilla line there.
- Smoothed modded rails are drawn with their straight texture. The texture came from the rail
  model's particle sprite, which a modded corner model (Railcraft's) takes from the curved texture.
  It now comes from the rail's straight (north-south) shape, for vanilla rails too.
- Furnace minecarts keep going round smoothed curves on Minecraft 1.21.1 and earlier. Their push
  turns to follow their motion in vanilla code that a smoothed run skipped, so they slowed to a stop
  in a bend (or reversed on a loop). 1.21.2 and later were not affected.
- Self-test: furnace carts over an arc and a loop; smoothing at the edge of the loaded area.

## 0.3.0 — 2026-10-05

Server-only mode on Minecraft 26.2 Fabric. Every other band is unchanged.

- The 26.2 Fabric jar nests Polymer (core, resource pack, AutoHost) and lets clients without
  SlashRails join (`allowVanillaClients`, default true). They get the Track Smoother as a
  Polymer-backed stick with the mod's model from the server pack, no SlashRails payloads, vanilla
  rail rendering, and the curve ride from the server's cart positions.
- `cartSyncTicks` (default 1): position updates for carts on smoothed runs every tick, so vanilla
  riders glide instead of stepping every 3 ticks.
- All SlashRails text carries its English string as a fallback, and the item keeps the plain stick
  model when the server pack isn't loaded, so a server without AutoHost degrades to readable text.
- The server logs which path each player takes on join.
- `prodtest.py` checks server-only jars (log line, AutoHost pack contents) and takes `--jar`.
- Known: the arm doesn't swing when a vanilla player uses the tool.

## 0.2.0 — 2026-09-19

Every Minecraft version from 1.20.1 to 26.3.

- Fabric builds for 1.20.1, 1.20.2, 1.20.3–1.20.4, 1.20.5–1.20.6, 1.21, 1.21.1, 1.21.2–1.21.3, 1.21.4, 1.21.5,
  1.21.6–1.21.8, 1.21.9–1.21.10, 1.21.11, 26.1.2, 26.2 and 26.3.
- NeoForge builds for 1.20.4, 1.20.6, 1.21.1, 1.21.2–1.21.3, 1.21.4, 1.21.5, 1.21.6–1.21.8, 1.21.9–1.21.10,
  1.21.11, 26.1.2, 26.2 and 26.3 (26.3 as beta, like NeoForge 26.3 itself).
- MinecraftForge 1.20.1 build, which also runs on NeoForge 1.20.1.
- From 1.21.2, minecarts ride curves through Minecraft's new minecart-behaviour code; carts using the
  experimental "Minecart Improvements" physics are left to vanilla.
- From 1.21.11 the tool preview and debug lines are drawn with Minecraft's own debug-line (gizmo)
  renderer.

## 0.1.0 — 2026-09-19

First version. Minecraft 1.21.1, Fabric and NeoForge.

- Track Smoother item: use on a rail to turn its run of flat rails into one smooth curve; use again
  to revert. Green preview line while holding it.
- Curve fitting smooths the vanilla cart path (not the rail centres), keeps within 0.55 blocks of
  it, and meets the vanilla rail beyond each end exactly and tangentially. Loops are supported.
- Rails with blocks beside them keep the vanilla line, so carts don't scrape tunnels or walls.
- Vanilla minecarts ride the curve: vanilla speeds, friction, powered-rail boost and braking,
  detector and activator rails; no diagonal speed surge.
- Curved track is drawn as chunk geometry with each rail's own texture.
- Smoothed runs are saved with the world and synced to clients; breaking or re-shaping any rail
  reverts its run.
- Operator commands: `/slashrails testtrack`, `probe`, `selftest`, `list`.
- Config: `config/slashrails.properties` (max run length, operator-only tool, preview, debug
  overlay).
