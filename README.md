# SniffRTP

**SniffRTP** is a stupidly simple RTP system with a bunch of customizable stuff inside `config.yml`.

It is **specifically designed for servers that do not need a bunch of dumb, disconnected plugins** just to handle basic random teleportation.

## Features

* Simple `/rtp` random teleport
* Configurable RTP radius
* Configurable countdowns
* Configurable cooldowns
* Movement-cancelled RTPs
* Separate failed-RTP cooldown
* Admin countdown/cooldown bypass
* LuckPerms and standard permission support
* Minecraft title countdowns
* Lightweight configuration
* Supports time formats such as `20` and `20s`
* Designed to stay simple instead of turning RTP into an entire framework

## Preview

### Before RTP

![Pre RTP](https://cdn.modrinth.com/data/cached_images/8360e9587ac18943fa53a66eb66300f57ebe0727.jpeg)

### After RTP

![After RTP](https://cdn.modrinth.com/data/cached_images/0a59c7f6c939bc7c63212987663b8df185255056.jpeg)

## Commands

### `/rtp`

Starts a random teleport.

```text
/rtp
```

The player will be teleported to a randomly selected valid location after the configured countdown.

## Permissions

| Permission  | Description                       | Default   |
| ----------- | --------------------------------- | --------- |
| `rtp.use`   | Allows the player to use `/rtp`   | Everyone  |
| `rtp.admin` | Gives administrative RTP behavior | Operators |
| `rtp.*`     | Grants all SniffRTP permissions   | None      |

Players with any of the following receive administrative RTP behavior:

```text
OP
*
rtp.*
rtp.admin
```

Administrative players can bypass the normal RTP countdown/cooldown behavior.

## Countdown

Normal players receive a configurable countdown before teleporting.

By default, the title looks similar to:

```text
RTP
Stand still for 5 seconds
```

The countdown is displayed using Minecraft titles.

If the player moves during the countdown, the RTP is cancelled and the configured failed-RTP cooldown is applied.

## Configuration

SniffRTP is designed around a small and readable `config.yml`.

Example:

```yml
# SniffRTP Attribution and Preservation License v1.1

default-cooldown: 20s
default-failed-cooldown: 5s
rtp-radius: 1000
```

### Time Values

Time values may include `s`:

```yml
default-cooldown: 20s
```

Or just a number:

```yml
default-cooldown: 20
```

Both are interpreted as:

```text
20 seconds
```

### RTP Radius

The maximum RTP radius can also be changed in `config.yml`.

```yml
rtp-radius: 1000
```

The default maximum radius is:

```text
1000 blocks
```

## Permission Plugins

SniffRTP works with normal Bukkit permission handling and permission plugins such as **LuckPerms**.

Example:

```text
/lp user <player> permission set rtp.admin true
```

Or:

```text
/lp group <group> permission set rtp.use true
```

## Supported Server Software

SniffRTP is intended to support server-side platforms/builds including:

* Paper
* Purpur
* Spigot
* Bukkit
* Folia
* Sponge
* Fabric
* NeoForge
* Forge

Support may depend on the specific SniffRTP build being used.

## License

SniffRTP is distributed under the:

**SniffRTP Attribution and Preservation License v1.1**

You may modify and use SniffRTP, including on commercial servers, as long as you follow the license requirements.

The original SniffRTP attribution must be preserved.

Do not remove or falsely claim ownership of the original project attribution.

---

**SniffRTP — by imsoback / deepslate / sniff / sussy**
