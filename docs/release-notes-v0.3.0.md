## SlashRails 0.3.0

On **Minecraft 26.2 with Fabric**, SlashRails can now run on the server alone. Players don't need to install anything to join.

**Players without the mod**
- Can join the server and use the Track Smoother. It shows as a stick with its own icon, from the server's resource pack.
- Ride the smooth curves. The server sends cart positions every tick on a curve, so the ride glides instead of stepping.
- Still see the original zigzag rails, and the cart model steps from rail to rail while it moves along the curve. Players who install SlashRails see the curved track as before.

**For server owners**
- Two new settings in `config/slashrails.properties`: `allowVanillaClients` (default `true`) and `cartSyncTicks` (default `1`; `3` is vanilla's rate).
- The icon and text need Polymer's resource-pack hosting turned on (`config/polymer/auto-host.json`). Without it, players without the mod see a plain stick with English text.
- The server log says, for each player who joins, whether they have SlashRails.

**Good to know**
- Only the 26.2 Fabric file has this. Every other version and loader still needs SlashRails on the server and every client.
- When a player without the mod uses the Track Smoother, their arm doesn't swing. Everything else works.
