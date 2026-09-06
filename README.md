# SniffRTP

A lightweight, configurable Random Teleport plugin for Paper Minecraft servers.

SniffRTP provides a straightforward `/rtp` system with configurable countdowns, cooldowns, movement cancellation, permission-based administrative bypasses, and LuckPerms compatibility.

## Features

* Random teleporting with `/rtp`
* Configurable pre-teleport countdown
* Title + subtitle countdown display
* Movement detection during countdown
* Cancelled RTP cooldown
* Normal RTP cooldown
* Separate administrative behavior
* Bukkit/LuckPerms permission support
* Lightweight configuration
* Supports time values such as `20s`
* Bare numbers such as `20` are interpreted as seconds

## Permissions

| Permission  | Default  | Description                      |
| ----------- | -------- | -------------------------------- |
| `rtp.use`   | Everyone | Allows `/rtp`                    |
| `rtp.admin` | OP       | Administrative/bypass permission |
| `rtp.*`     | None     | Grants all SniffRTP permissions  |

Administrative checks may also recognize OP and wildcard permissions such as `*`.

## Example

When a normal player executes `/rtp`, SniffRTP can display:

**RTP**

*Stand still for 5 seconds*

If the player moves before the timer finishes, the RTP is cancelled and the configured failed cooldown is applied.

If the countdown completes successfully, the player is randomly teleported.

## Configuration

Example:

```yaml
default-cooldown: 20s
default-failed-cooldown: 5s
```

Time suffixes may be used where supported.

For convenience:

```yaml
default-cooldown: 20
```

is interpreted as:

```yaml
default-cooldown: 20s
```

## License

This project is distributed under the **SniffRTP Attribution and Preservation License v1.1**.

Modification and commercial use are permitted under the license, but the required attribution and preservation notices must remain intact.

You may not redistribute SniffRTP or a modified version while falsely claiming the original work as your own.

Do not remove or intentionally alter the protected attribution/license notices included with the project.

**Original author:** deepslate / sniff / sussy — **imsoback**
