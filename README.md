# SniffRTP

A configurable random teleport plugin and server-side mod for **Minecraft 26.2**.

SniffRTP focuses on `/rtp`: finding suitable destinations, preparing chunks, handling countdowns and cooldowns, and displaying configurable feedback.

## Before RTP

![Before RTP](https://cdn.modrinth.com/data/cached_images/8360e9587ac18943fa53a66eb66300f57ebe0727.jpeg)

## After RTP

![After RTP with blindness](https://cdn.modrinth.com/data/cached_images/0a59c7f6c939bc7c63212987663b8df185255056.jpeg)

## Features

- Configurable minimum and maximum RTP radius
- Live world-border checks
- Dimension-specific Overworld, Nether, and End handling
- Full-floor and two-air-block landing checks
- Destination preparation before teleport
- Adaptive separation from recent destinations
- Countdown that holds at **1** until preparation finishes
- Normal-player, admin, and failed-attempt cooldowns
- Optional movement, damage, interaction, and combat cancellation
- Admin countdown, cooldown, and cancellation bypass settings
- Titles, actionbars, bossbars, sounds, particles, and potion effects
- Configurable messages and bold RTP chat prefixes
- Live config reload and deleted-config recovery
- Commented, organized YAML configuration

## Requirements and Installation

SniffRTP provides separate builds for:

| Platform | Installation folder |
| --- | --- |
| Bukkit / Spigot | `plugins/` |
| Paper / Purpur / Folia | `plugins/` |
| Fabric / Forge / NeoForge | `mods/` |

Use **Minecraft 26.2**, **Java 25**, and exactly one matching SniffRTP JAR.

Fabric also requires a compatible Fabric API. The Folia build requires Folia **26.2**; a 26.1.2 server cannot load its declared API version.

Stop the server before replacing a JAR. Existing configuration files are preserved.

## Commands

| Command | Description |
| --- | --- |
| `/rtp` | Start RTP in the current world |
| `/rtp overworld` | Target the Overworld |
| `/rtp nether` | Target the Nether |
| `/rtp end` | Target the End |
| `/rtp <world-name>` | Target a loaded world |
| `/rtp <biome>` | Request a biome, when permitted |
| `/rtp cancel` | Cancel a pending request |
| `/rtp reload` | Reload configuration; requires administrative access |

After config deletion, the console offers:

- `sniffrtp yes` — Restore the last valid configuration
- `sniffrtp nah` — Restore bundled defaults

## Permissions

| Permission | Purpose |
| --- | --- |
| `rtp.use` | Basic RTP access |
| `rtp.admin` | Administrative access and configured bypasses |
| `rtp.*` | Full RTP access |
| `rtp.overworld` | Overworld access when disabled by default |
| `rtp.nether` | Nether access when disabled by default |
| `rtp.end` | End access when disabled by default |
| `rtp.custom` | Custom-dimension access when disabled by default |
| `rtp.biomes` | Biome targeting when disabled by default |

Compatible permission managers, including LuckPerms, can manage these permissions. Explicitly disabled worlds remain excluded from RTP.

## Configuration

Configuration locations:

- Plugins: `plugins/SniffRTP/config.yml`
- Mods: `config/sniffrtp/config.yml`

Cooldowns appear near the top, followed by mid-RTP displays, cancellation, and effects. Inline comments explain each setting and identify retained legacy options.

Example:

```yaml
cooldown:
  default-cooldown: 20s # Wait after a successful normal-player RTP.
  default-failed-cooldown: 5s # Wait after a failed search or teleport.
  default-adm-cooldown: 0s # Admin cooldown when bypass is disabled.
  adm-bypass-cooldown: true # Allow admins to bypass cooldowns.

ui:
  midrtp-stand-still-enabled: true # Enable the normal-player countdown.
  midrtp-stand-still-time: 5s # Countdown duration.

cancel:
  midrtp-cancel-onmove: false # Enable to cancel RTP when the player moves.
```

Durations accept `ms`, `s`, `m`, and `h`. Bare numbers are interpreted as seconds.

Existing root-level settings remain supported and take precedence over their grouped equivalents.

## Countdown Behavior

Destination preparation starts during the countdown.

If preparation finishes early, the player waits for the countdown. If it finishes late, the display holds at **1** without repeating the countdown sound. The arrival display appears after teleporting.

A zero-second countdown still waits for destination preparation, but does not show a zero-second countdown message.

Movement cancellation is optional and must be enabled separately. Manual cancellation does not automatically apply the failed-search cooldown.

## Destination Selection

Each request selects candidates inside the configured radius and world border.

SniffRTP prepares a candidate chunk and checks for a full landing floor with two empty blocks above it. It can inspect up to 16 columns in that chunk before requesting another candidate.

Recent destinations are tracked per player and dimension. Separation relaxes when terrain or search time makes finding a destination difficult. Border, radius, biome, and landing checks remain required.

Nether destinations stay below the configured roof limit. End destinations require suitable terrain.

## Chunk Preparation

Paper, Purpur, and Folia use their asynchronous chunk APIs. Bukkit and Spigot use a Minecraft 26.2 native bridge, while the mod builds use native queued chunk preparation.

Preparation is bounded and tracked so a cancelled request cannot immediately start overlapping generation for the same player.

Chunk generation still consumes server resources. Performance depends on terrain, hardware, server load, and other installed software.

## Validation

Earlier live tests completed 161 teleports across seven platform variants. Later config, countdown, chat, and logging changes passed 88 automated assertions and verification of all eight JARs.

The logger update also passed a Paper startup test without the System.out/err warning.

Full Folia RTP validation remains pending. Automated tests do not replace real-client checks or production multiplayer testing.

## Building

1. Install JDK 25 and Python 3.12 or newer.
2. Set `JAVA_HOME`.
3. Run `bootstrap.ps1` to fetch the pinned build dependencies.
4. Run `python build.py`.

Platform JARs and the combined archive are written to `dist/`.

## License and Attribution

Created by **imsoback (deepslate/sniff/blacky/catty)**.

Distributed under the **SniffRTP Attribution and Preservation License v1.1**.

Required attribution and preservation notices must remain intact. Consult the full license before redistributing modified builds.
