# SniffRTP

**SniffRTP** is a lightweight and customizable random teleport system for Minecraft servers.

It is designed for servers that want a proper `/rtp` implementation without installing a giant general-purpose teleport plugin.

SniffRTP provides configurable countdowns, cooldowns, cancellation behavior, permissions, visual effects, sounds, world-border support, chunk preloading, and a custom optimized destination engine.

---

## Screenshots

### Before RTP

![Pre RTP](https://cdn.modrinth.com/data/cached_images/8360e9587ac18943fa53a66eb66300f57ebe0727.jpeg)

### After RTP

![After RTP](https://cdn.modrinth.com/data/cached_images/0a59c7f6c939bc7c63212987663b8df185255056.jpeg)

---
## Features

* `/rtp` random teleport
* Custom lightweight RTP location engine
* SpreadPlayers-inspired random position selection
* Does **not** simply execute vanilla `/spreadplayers`
* Destination chunk preloading
* Safe destination validation
* Overworld-specific destination handling
* Nether-specific destination handling
* End-specific destination handling
* World-border support
* Configurable RTP radius
* Configurable countdown
* Configurable cooldown
* Configurable failed/cancelled cooldown
* Cancel teleport when the player moves
* Admin countdown bypass
* Admin cooldown bypass
* LuckPerms support
* Configurable permissions
* Minecraft title countdowns
* Actionbars
* Bossbars
* Sounds
* Particles
* Potion effects
* Configurable messages
* Live configuration reload
* Time parsing such as `20` or `20s`
* Lightweight design

---

## RTP Engine

SniffRTP uses its own lightweight destination-selection system inspired by Minecraft's SpreadPlayers logic.

Instead of forwarding the player through the vanilla `/spreadplayers` command, SniffRTP directly:

1. Selects a random X/Z coordinate inside the configured RTP area.
2. Respects configured minimum/maximum distances and world borders.
3. Searches for an appropriate destination for the current dimension.
4. Rejects unsuitable locations.
5. Preloads the required destination chunks.
6. Teleports the player after the configured countdown.
7. Runs the configured messages, effects, sounds, particles, titles, actionbars, and/or bossbars.

The Nether and End use their own destination rules rather than blindly applying Overworld assumptions.

---

## Commands

### `/rtp`

Starts a random teleport.

Example:

```text
/rtp
```

---

## Permissions

| Permission  | Description                                   |
| ----------- | --------------------------------------------- |
| `rtp.use`   | Allows normal `/rtp` usage                    |
| `rtp.admin` | Administrative access and configured bypasses |
| `rtp.*`     | Full SniffRTP permissions                     |

`rtp.use` is intended to be available to regular players by default.

SniffRTP works with permission managers such as **LuckPerms**.

---

## Countdown

By default, normal players can be required to stand still before teleporting.

Example:

```text
RTP
Stand still for 5 seconds
```

Moving during the countdown can cancel the teleport and apply the configured failed RTP cooldown.

Administrators can be configured to bypass the countdown.

---

## Cooldowns

SniffRTP supports separate cooldown behavior for successful and cancelled RTP attempts.

Example configuration values can use:

```yaml
default-cooldown: 20s
default-failed-cooldown: 5s
```

Or simply:

```yaml
default-cooldown: 20
```

Values without a suffix are treated as seconds.

---

## Customization

SniffRTP can customize nearly every part of the teleport sequence.

This includes:

* Chat messages
* Titles
* Subtitles
* Actionbars
* Bossbars
* Sounds
* Particles
* Potion effects
* Countdown duration
* Cooldown duration
* Failed cooldown
* Movement cancellation
* Teleport radius
* World rules
* Destination behavior

The plugin can therefore be configured as either a completely minimal RTP system or a more polished server-style teleport experience.

---

## Chunk Preloading

SniffRTP loads the destination area before moving the player.

This helps avoid teleporting the player into a location whose chunks have not finished loading yet and reduces visible world-loading interruptions after RTP.

Destination loading is performed as part of the RTP process rather than after the player has already arrived.

---

## Platform Support

SniffRTP is distributed as platform-specific builds for supported Minecraft server ecosystems.

Always use the JAR intended for your server implementation.

---

## License

SniffRTP uses the **SniffRTP Attribution and Preservation License v1.1**.

You may modify and use SniffRTP, including commercially, subject to the license requirements.

Required attribution and preservation notices must remain intact.

Do not redistribute SniffRTP or modified versions while falsely claiming the original project as your own work.

---

**SniffRTP — (deepslate/sniff/sussy)imsoback**
