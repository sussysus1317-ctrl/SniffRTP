# ✧ SniffRTP ✧

Random teleport for Minecraft servers. Use `/rtp` to find a safe destination within the configured radius and world border.

## Before RTP

![Before RTP](https://cdn.modrinth.com/data/cached_images/8360e9587ac18943fa53a66eb66300f57ebe0727.jpeg)

## After RTP

![After RTP with blindness](https://cdn.modrinth.com/data/cached_images/0a59c7f6c939bc7c63212987663b8df185255056.jpeg)

## Installation

Download the build matching your Minecraft version and server platform. Check that release’s requirements.

- **Bukkit, Spigot, Paper, Purpur, Folia:** put the JAR in `plugins/`.
- **Fabric, Forge, NeoForge:** put the JAR in `mods/`.
- **Fabric:** also install the matching Fabric API.

Stop the server before updating. Remove the previous SniffRTP JAR and install only one build.

## How It Works

1. **Start a request.** `/rtp` starts the configured countdown and begins finding a destination.
2. **Pick coordinates.** SniffRTP chooses a random position between the minimum and maximum radius, inside the world border.
3. **Prepare the chunk.** The destination chunk is prepared before the player moves.
4. **Check the landing.** The position needs a full, suitable floor with two empty blocks above it. Hazardous and configured forbidden floor materials are rejected.
5. **Try another position if needed.** SniffRTP can check up to 16 columns in the prepared chunk before requesting another candidate.
6. **Teleport when ready.** Both preparation and the countdown must finish before teleporting. Arrival messages and effects then run.

The search remembers recent destinations for each player and dimension. It initially tries to keep successive teleports apart, then relaxes that spacing if finding a destination becomes difficult. Border, radius and landing checks still apply.

The Nether uses interior landing checks below the configured roof limit. The End requires suitable terrain beneath the player.

SniffRTP selects destinations directly rather than executing `/spreadplayers`.

## Countdown and Cancellation

The countdown stops at **1** while destination preparation is still running. It stays there without repeating the countdown sound, then switches to the arrival display after teleporting.

Setting the countdown to `0s` skips the delay but still waits for a safe destination.

Cancellation can be enabled for movement, damage, interaction or combat. Players can also use `/rtp cancel`. Administrators have separate bypass settings.

## Commands

| Command | Use |
| --- | --- |
| `/rtp` | Random teleport in the current world |
| `/rtp overworld` | Target the Overworld |
| `/rtp nether` | Target the Nether |
| `/rtp end` | Target the End |
| `/rtp <world-name>` | Target a loaded world |
| `/rtp <biome>` | Request a biome, when permitted |
| `/rtp cancel` | Cancel a pending request |
| `/rtp reload` | Reload the config; requires admin access |

## Permissions

| Permission | Use |
| --- | --- |
| `rtp.use` | Basic RTP access |
| `rtp.admin` | Administrative access and configured bypasses |
| `rtp.*` | Full RTP permissions |
| `rtp.overworld` | Overworld access when disabled by default |
| `rtp.nether` | Nether access when disabled by default |
| `rtp.end` | End access when disabled by default |
| `rtp.custom` | Custom-dimension access when disabled by default |
| `rtp.biomes` | Biome targeting when disabled by default |

Worlds explicitly listed as disabled remain excluded.

## Configuration

- **Plugins:** `plugins/SniffRTP/config.yml`
- **Mods:** `config/sniffrtp/config.yml`

Cooldowns appear near the top, followed by mid-RTP settings. Comments explain each option.

```yaml
cooldown:
  default-cooldown: 20s # Cooldown after successful normal-player RTP.
  default-failed-cooldown: 5s # Cooldown after a failed search or teleport.
  default-adm-cooldown: 0s # Admin cooldown when bypass is disabled.
  adm-bypass-cooldown: true # Allow admins to bypass cooldowns.
```

Durations accept `ms`, `s`, `m` and `h`. Numbers without a suffix mean seconds.

Other settings control teleport distance, allowed worlds, cancellation, messages, titles, actionbars, bossbars, sounds, particles and potion effects. Manual cancellation does not automatically apply the failed-search cooldown.

Existing configs are preserved when updating. Legacy root-level settings remain supported and take precedence over grouped settings.

If the config is deleted, use the server console:

- `sniffrtp yes` — Restore the last valid config.
- `sniffrtp nah` — Restore defaults.

## Performance

Chunk preparation starts during the countdown. Cancelled requests are tracked until their outstanding preparation finishes, preventing an immediate replacement request from overlapping it for the same player.

Generating new terrain still consumes server resources. Teleport speed depends on terrain, hardware, server load and other installed software.

Platform support and available features depend on the selected release.

## Reporting Issues

Include your Minecraft version, server platform, SniffRTP build, relevant console errors and steps to reproduce the problem.

## License

**SniffRTP Attribution and Preservation License v1.1**

Required attribution and preservation notices must remain intact.

**SniffRTP — imsoback (deepslate/sniff/blacky/catty)**
