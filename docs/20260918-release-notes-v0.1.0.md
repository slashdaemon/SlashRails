# SlashTracks 0.1.0 — release notes (DRAFT)

> **Draft for owner review.** Not approved for publishing. Channels, license and store copy are
> undecided (see "Open before publishing" at the end).

## SlashTracks 0.1.0

Long, gentle curves built from vanilla rails come out as zigzags, and the minecart ride over them
jerks sideways at every step. SlashTracks turns them into real curves.

**How it works**
- Craft a **Track Smoother** (rail, iron ingot, stick).
- Hold it and look at a rail. A green line previews the curve.
- Use it on the rail. The whole connected run becomes one smooth curve, drawn smooth and ridden
  smooth by ordinary minecarts.
- Use it again on a smoothed rail to turn the run back into vanilla rails.

**Still vanilla underneath**
- The rails stay vanilla rails. Powered, detector and activator rails in a smoothed run keep
  working, and removing the mod leaves your world exactly as you built it.
- The carts are ordinary minecarts.
- Where blocks sit right beside the track (a tunnel, a wall), the curve keeps to the rail's line so
  carts don't scrape.
- Breaking any rail in a smoothed run turns that run back into vanilla rails.

**Requirements**
- Minecraft 1.21.1 with Fabric (plus Fabric API) or NeoForge.
- Install on the server **and** on every client.

**Known limits in 0.1.0**
- Flat track only: a run stops where the track goes up or down a slope.
- A single sharp 90° corner can only be rounded slightly.
- Furnace minecarts haven't been tested on curves yet.

---

## Open before publishing

- Owner sign-off on these notes and on each channel (GitHub release, Modrinth, CurseForge, Discord).
- License: jars currently say `ARR`, matching StreamCraft. Confirm or change.
- Item icon and mod logo are placeholders.
- Creating a GitHub repo (`slashdaemon/SlashTracks`, private) hasn't been done.
- Verified: client rendering on Fabric and NeoForge; Sodium 0.6.13 (both loaders) and Iris 1.8.8
  with a shader pack (Fabric); a client on a dedicated server receives runs on join and live adds
  and removals. Not verified: Sodium 0.8.x, Iris on NeoForge, two clients at once, and a client
  without the mod being turned away by a Fabric server.
