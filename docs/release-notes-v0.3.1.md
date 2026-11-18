## SlashRails 0.3.1

Fixes for problems found when SlashRails runs alongside other rail mods. Thanks to the CityWorld author for the report.

**Fixes**

- Locomotives from other mods no longer stall on smoothed track. A train that drives itself, like a Choo Choo locomotive, now goes round smoothed bends. It shows on the curve, facing along it, while its own physics keep running on the rails underneath.
- Smoothing track no longer makes the server pause. Smoothing near the edge of the loaded area used to load, or even generate, the next chunk, and the server could freeze for 10 to 15 seconds. That chunk is now left alone, and the track right at the edge keeps to the original rail line.
- Rails from other mods look right when smoothed. Some modded rails, Railcraft's among them, were drawn with their corner texture and looked like twisted pieces of curve. Smoothed rails now always use their straight texture.
- Furnace minecarts keep going round smoothed curves on Minecraft 1.21.1 and earlier. Before, they slowed to a stop in a bend, and could turn back on a loop.

**Good to know**

- Carts from other mods that only add looks or storage still ride the smooth curve exactly like vanilla carts.
- On 26.2 Fabric servers that allow players without SlashRails, those players still see self-driving modded carts on the original rail line.

**Tested**

- Automated ride test with ridden, hopper and furnace minecarts, plus smoothing at the edge of the loaded area. It passed on Minecraft 1.20.1 Forge, 1.21.1 Fabric and NeoForge, 1.21.5 Fabric, 1.21.11 NeoForge, 26.2 Fabric and 26.3 NeoForge, on Windows. It also passed on 1.20.1 Forge, 1.21.1 Fabric and NeoForge, 1.21.5 Fabric and 26.2 NeoForge on macOS.
- The curved track was checked in-game on those five versions on macOS.
- A Choo Choo 1.2.0 locomotive was run on smoothed bends on NeoForge 1.21.1, on a server and in-game. With 0.3.0 it stood still. With 0.3.1 it goes round, just as it does on unsmoothed track, without stutter.
- Its position on the curve was measured from top-down screenshots: within 2 degrees of the track and 0.08 blocks of its centre line on a gentle arc, a staircase and a radius-10 loop. On a radius-6 loop it stays within about 0.1 blocks, the closest a 2.35-block body can follow a curve that tight.
- Every file was tested on a real server for its own Minecraft version and loader.
