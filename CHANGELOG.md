# Changelog

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
