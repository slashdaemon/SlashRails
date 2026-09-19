# SlashRails — Demand evidence

Evidence that players have asked Mojang for smooth, curved or diagonal rails, and for a smoother
minecart ride on them. It is here to inform the CurseForge/Modrinth listing. Companion to
`RESEARCH.md`, which covers mechanics and prior art.

Collected 2026-09-19. Engagement numbers are snapshots.

**Reliability notes:**
- Feedback-site votes and quotes are exact; they came from the site's public API.
- Mojira vote counts appear to have been reset by the 2025 migration, so they're left out.
- Reddit could not be read directly. Scores and quotes come from the Pullpush archive (r/minecraftsuggestions only), and the r/Minecraft item is a Sportskeeda paraphrase.
- Minecraft Forum quotes came through a summariser. Check each quote against the source before using it publicly.

## Summary

- **The request is 16 years old and still unmet.** The earliest dated ask is "Diagonal minecart tracks" on the Minecraft Forum, 2010-07-04. It recurs on the forum in 2010, 2012, 2013 and 2016; on Reddit from 2012 to 2025; and on the official feedback site in 2019, 2021 and 2025.
- **No single request is huge.** The best-quantified one is a comment inside a large minecart thread. The strength of the case is persistence, plus the cosmetic workarounds players already install.
- **Mojang has acknowledged the ride problem but not the geometry.**
  - The Minecart Improvements experiment (24w33a, 2024) aims to make carts "smoothly turn along with the track". It is still experimental as of 26.3 and doesn't exist on 1.21.1.
  - Two open, confirmed bugs show stutter and twitching on diagonal and curved track under the experiment.
  - Mojira staff have closed diagonal-rail reports as Invalid with the comment "There are no diagonal rails in Minecraft."
- **The current trigger:** dai's video of 2026-09-18 names both halves of the pitch in one breath: the zigzag look, and a track that "looks zigzaggy when I'm riding it". He ends with "I hope one day Mojang fixes this."

## 1. Creator coverage

| Source | Date | Reach | What was said |
|---|---|---|---|
| **dai** (@daitadori, 39.7K subs), "Block By Block" ep. 23 — [video](https://www.youtube.com/watch?v=0G3RQ9ngtAI). Title is A/B tested ("The Train Station Every Minecraft World Needs" / "The Railway That Changed My Minecraft World") | 2026-09-18 | ~22.4K views, 745 likes | ~6:47: "it feels kind of off with all these zigzags here… if I do want to have a curving track like this… I can't have it smooth." ~7:05: "either I have like a cool looking track… that bends across but looks zigzaggy when I'm riding it or… a straight line or like a 90° angle turn which just doesn't feel realistic to me… I hope one day Mojang fixes this." ~12:27: "still feels a bit too abrupt for me, but this is the best I can do." (from auto-captions) |
| **Skip the Tutorial** (10.6M subs), "41 Tiny Texture Updates Minecraft Should Add" — [video](https://www.youtube.com/watch?v=9uR1ILvx1LM) | 2026-04-12 | 2.9M views | Diagonal rails are the first item: "diagonal rails that are actually diagonal"; "Minecraft doesn't have a lot of curves, so why are our rails curved?" The description links the Diagonal Rails pack. |
| **ShalzLIVE**, "This Mod fixes Minecarts" — [video](https://www.youtube.com/watch?v=BzyJ-EtWwYo) | 2024-12-18 | 1.64M views | Showcases Splinecart, which uses its own track blocks. Shows that the audience exists. |
| **BoxBlair**, "Diagonal Rails SUCK in Beta Minecraft" — [video](https://www.youtube.com/watch?v=ToO_aFi14Zs) | 2026-03-21 | ~3.1K views | The complaint in the title. |

Not checked: Nekoma, "I Fixed Travel In Minecraft" (798K views), which has a minecart chapter at 8:37–12:08.

## 2. Official feedback site (feedback.minecraft.net)

| Post | Date | Votes / comments | Evidence |
|---|---|---|---|
| [Making Minecarts more robust](https://feedback.minecraft.net/hc/en-us/community/posts/360009328511) | 2018-05-26 | 1,362 / 76, **Not Planned** | Comment (2019-01-13, 15 votes): "We can also add a better derail mechanic, diagonal rails and longer curves (allow higher speeds and looks cool)." Comment (2020-03-30): "Diagonal rails instead of the right~left~right elbow joint rails…" |
| [Let's talk about Minecarts! (Mojang, 24w33a)](https://feedback.minecraft.net/hc/en-us/community/posts/29056767276685) | 2024-08-07 | 143 / 106 | Mojang: "we aim to make riding minecarts a smooth experience… Minecarts will now smoothly turn along with the track." Player (2024-08-16): "Carts traveling down diagonal tracks don't move smoothly, they wiggle left and right." |
| [Experimental Minecart Suggestions](https://feedback.minecraft.net/hc/en-us/community/posts/29304642562829) | 2024-08-15 | 40 / 2 | "having the player camera rotate smoothly with the minecart" |
| [Curved Power Rails](https://feedback.minecraft.net/hc/en-us/community/posts/360078475091) | 2021-06-16 | 14 / 4 | "trying to make an angled road… the powered rails are not curving" |
| [Minecart Overhaul](https://feedback.minecraft.net/hc/en-us/community/posts/360077569051) | 2021-04-10 | 5 / 2 | Comment: "not having only such sharp corner, but smoother longer curves over multiple blocks" |
| [Minecart Update](https://feedback.minecraft.net/hc/en-us/community/posts/40806469001357) | 2025-11-02 | 1 / 0 | "Make more gentle curves… Right now the curves are all 90° turns, maybe you could add 45° and 22.5° curves aswell." |
| [Diagonal minecart tracks](https://feedback.minecraft.net/hc/en-us/community/posts/42071102851213) | 2025-12-19 | 1 / 0 | "you have a terrible time trying to build diagonal tracks… I could try to make a diagonal rail by making a bunch of 90 degree turns…" |
| [Minecart player rotation](https://feedback.minecraft.net/hc/en-us/community/posts/29358079767437) | 2024-08-17 | 0 | Under the experiment: "the intensity of turning speed is very high… After sometime i was feeling dizzy" |

Mojang has not replied on any of these, and no Mojang statement for or against diagonal or wide curves was found. "Better Rail Curve logic" (16677960281613) is only about which way a corner faces, so it isn't relevant.

## 3. Mojang bug tracker

| Issue | Status | Relevance |
|---|---|---|
| [MC-8884](https://bugs.mojang.com/browse/MC-8884), "Minecart rails will not run diagonally" (2013) | Invalid | Reporter: rails "turn left, turn right, turn left, turn right." Moderator: "There are no diagonal rails in Minecraft." |
| [MC-275756](https://bugs.mojang.com/browse/MC-275756), "Minecart visually stutters when snapping to diagonal rails" (2024) | Open, Confirmed | Under the experiment. |
| [MC-275929](https://bugs.mojang.com/browse/MC-275929), "Minecarts twitch when traveling around curved rails with blocks next to them" (2024) | Open, Confirmed | Moderator: "occurs more generally than originally described." |
| [MC-201](https://bugs.mojang.com/browse/MC-201), "Field of Vision does not turn at all with a turning minecart" (2012) | Reopened, Confirmed | Fixed only inside the experiment, behind a toggle. |
| [MC-7857](https://bugs.mojang.com/browse/MC-7857), "Curved rails cannot be sloped" (2013) | Works As Intended | "A slope cannot be curved." |

## 4. Community history

**Minecraft Forum, Suggestions**

- [Diagonal minecart tracks](https://www.minecraftforum.net/forums/minecraft-java-edition/suggestions/8237-diagonal-minecart-tracks), **2010-07-04**, the earliest found: "instead of being stuck with awkward right and left tracks."
- [Minecarts: Curved Tracks](https://www.minecraftforum.net/forums/minecraft-java-edition/suggestions/11661-minecarts-curved-tracks), 2010-09-19: "It bugs me that minecart tracks can only turn at 90 degree angles, making for very disorienting rides."
- [Diagonal Rails](https://www.minecraftforum.net/forums/minecraft-java-edition/suggestions/88115-diagonal-rails), 2013-12-14: "It looks ugly when the rails are curved back and forth when the cart goes diagonal."
- [Diagonal Tracks](https://www.minecraftforum.net/forums/minecraft-java-edition/suggestions/2750015-diagonal-tracks), 2016-10-13: "this ugly zig-zag pattern that is just not pleasing to ride." It proposes detecting the zigzag and turning it diagonal, which is essentially our approach.

**Reddit, r/minecraftsuggestions** (archived score / comments)

- [How about diagonal rails? … DONK DONK DONK DONK DONK DONK](https://reddit.com/r/minecraftsuggestions/comments/9vmjhd/), 2018-11-09, 107 / 24.
- [Diagonal rails?](https://reddit.com/r/minecraftsuggestions/comments/yg16z/), 2012-08-18: "Going zigzag is annoying. Diagonal tracks would save material, time and make the ride smoother."
- [Rails Flexible Angle](https://reddit.com/r/minecraftsuggestions/comments/1ohsu5/), 2013-10-15: "one down two or more forward are very awkward… What if a track from C to B was a smooth line?" This is exactly the gentle staircase.
- [Diagonal rails and turnouts](https://reddit.com/r/minecraftsuggestions/comments/4szht8/), 2016-07-15: "Rails should also run optical diagonally (internally still stored as zigzag)." That is our design, requested by a player.
- [diagonal rails texture…](https://reddit.com/r/minecraftsuggestions/comments/7n3915/), 2017-12-30, 31 / 2: "mine carts should also go straight on the diagonal rails instead of 'dancing' due to the curves."
- [Your head should rotate with the minecart](https://reddit.com/r/minecraftsuggestions/comments/8bpklw/), 2018-04-12, 87 / 16.

**Reddit, r/Minecraft**

- [Diagonal rail layout thread](https://www.reddit.com/r/Minecraft/comments/1i5m5wj/), January 2025, as reported by [Sportskeeda](https://www.sportskeeda.com/minecraft/minecraft-player-showcases-best-way-make-diagonal-rail-tracks). Commenters say the zigzag "makes them seasick" and wish Mojang "would just add diagonal rails". These are paraphrases, not verbatim.

## 5. What players already install

Download counts, 2026-09-19.

| Project | Downloads | What it fixes | What it leaves |
|---|---|---|---|
| Splinecart + forks (ForkCart, CoasterCart, MCoaster, peterwolf, …) | ~560K combined | Smooth curves | Own track blocks; mostly Fabric. The most-repeated issue request is a NeoForge/Forge port (#11, #21, #51; MCoaster #9). Splinecart #31: "it doesnt connect to vanilla rails". |
| Diagonal Perfect Rails 3D PBR + Diagonal Rails (resource packs) | ~173K (Modrinth) | 45° runs look straight | Looks only, 45° only. The DPR author says a pack can't tell a corner from a diagonal (issue #1). |
| Nautilus3D (1.5M) | — | Added diagonal rails after a user request (#47) | Looks only |
| Smooth Minecarts | ~8.3K | Smooth ride on vanilla rails | Needs the experiment, Fabric only, no visuals |
| Create (210M CF) / Steam 'n' Rails / MTR | — | Curved track | Separate train systems |

## 6. Implications for the listing

**Claims the evidence supports**

- *Headline:* rail curves that are actually curves, drawn smooth and ridden smooth, with no zigzag.
- *Longstanding:* "Players have asked for this since 2010." Safe, but check the 2010 forum thread first. "for over a decade" is the safer wording.
- *Vanilla carts on vanilla rails, no new blocks, removable.* This answers the Splinecart complaint ("doesn't connect to vanilla rails") and the resource packs' limits.
- *No experiment needed.* True on 1.21.1, where Minecart Improvements doesn't exist.
- *NeoForge and Fabric.* NeoForge is the most-requested gap in this space.
- *Both halves together.* Every existing fix does looks or ride, never both. dai's quote describes both problems at once.
- *Any gentle angle, not just 45°.* The 2013 "Rails Flexible Angle" post and the 2025 "22.5° curves" post ask for exactly this, and the resource packs can't do it.

**Player language for copy:** zigzag, zigzaggy, "DONK DONK DONK", wiggle, dancing, seasick, "can't have it smooth", "curves more naturally", "diagonal rails that are actually diagonal".

**Avoid**

- *"Mojang said no."* The only "Not Planned" is on a broad minecart post, and the Mojira closures are staff triage, not design statements. "Vanilla still can't do this" is accurate.
- *Quoting or naming dai (or embedding the clip) on the store page without asking him.* Quoting from a public video is probably fine legally, but it reads as endorsement. The safer route is to reach out; a mention or a showcase from him would be worth more than the quote.
- *Disparaging named competitors.* Compare by capability ("works with the rails you already built") rather than by name.
- *Anything implying Mojang affiliation.* Keep the existing disclaimer.
