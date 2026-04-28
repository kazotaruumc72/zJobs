# zJobs

> A fully configurable Minecraft jobs plugin for Paper/Folia servers, built on top of [zMenu](https://groupez.dev).  
> Players earn experience and money by performing in-game actions. The plugin ships with levelling, prestige, rewards, a boost system, a forge system and a refine (rafine) system.

---

## Table of Contents

1. [Requirements](#requirements)
2. [Installation](#installation)
3. [Default Jobs](#default-jobs)
4. [Configuration – `config.yml`](#configuration--configyml)
5. [Job Configuration](#job-configuration)
   - [Action Types](#action-types)
   - [Action Fields](#action-fields)
   - [Reward Action Types](#reward-action-types)
6. [Forge System](#forge-system)
7. [Refine (Rafine) System](#refine-rafine-system)
8. [Commands](#commands)
9. [Permissions](#permissions)
10. [PlaceholderAPI Placeholders](#placeholderapi-placeholders)
11. [Storage](#storage)
12. [Economy / Currency](#economy--currency)
13. [Boost System](#boost-system)

---

## Requirements

| Dependency | Required | Notes |
|---|---|---|
| [Paper](https://papermc.io) / Folia | ✅ | API level 1.13+ |
| [zMenu](https://groupez.dev) | ✅ | Menu engine + expression evaluator |
| Vault | ⬜ Optional | Used when `default-economy: VAULT` |
| BlockTracker | ⬜ Optional | Prevents farming naturally-generated blocks |
| [Nexo](https://nexomc.com) | ⬜ Optional | Custom items support for Forge/Rafine |
| [zShop](https://github.com/Maxlego08/zShop) | ⬜ Optional | Enables the `ZSHOP_BUY` / `ZSHOP_SELL` zMenu button types |

---

## Installation

1. Drop `zJobs.jar` into your `plugins/` folder.
2. Make sure **zMenu** is installed and loaded first.
3. Start (or restart) your server.  
   The plugin creates `plugins/zJobs/` with all default configuration files.
4. Edit `config.yml`, the `jobs/` folder, and (optionally) `items.yml` to fit your server.
5. Run `/zjobs reload` to apply changes without a full restart.

---

## Default Jobs

| Job file | Name |
|---|---|
| `miner.yml` | Miner – breaks ores and stone-like blocks |
| `farmer.yml` | Farmer – harvests crops and tames animals |
| `lumberjack.yml` | Lumberjack – chops logs |
| `hunter.yml` | Hunter – kills mobs |
| `fisherman.yml` | Fisherman – catches fish |
| `enchanter.yml` | Enchanter – enchants items |
| `brewer.yml` | Brewer – brews potions |

---

## Configuration – `config.yml`

```yaml
# Show verbose console output
enable-debug: true

# Storage backend: SQLITE | MYSQL | MARIADB
storage-type: SQLITE

database-configuration:
  table-prefix: "zjobs_"
  host: 127.0.0.1
  port: 3306
  user: root
  password: 'secret'
  database: zjobs
  debug: false

# Jobs assigned to every new player automatically
default-jobs:
  - "miner"
  - "farmer"
  - "lumberjack"
  - "enchanter"
  - "brewer"
  - "hunter"

# Number format used for money/experience display
decimal-format: "#.##"

# Default economy provider (VAULT or a zMenu currency name)
default-economy: "VAULT"
currency-name: "money"
money-reason: "Job money"

# Permission-based job slot limits
jobs-limit-permissions:
  - permission: "zjobs.limit.player"
    limit: 1
  - permission: "zjobs.limit.vip"
    limit: 2
  - permission: "zjobs.limit.admin"
    limit: 3

# Worlds where jobs give no rewards
disabled-worlds:
  - "world_the_end"

# How often the plugin syncs player data (in ticks, 20 ticks = 1 second)
update-jobs-ticks: 600
```

---

## Job Configuration

Each file in `plugins/zJobs/jobs/` defines one job.

```yaml
# Display name shown in menus / messages
name: "ᴍɪɴᴇʀ"

# Base XP used in the level formula
base-experience: 100

# Highest reachable level
max-levels: 100

# Number of prestiges available (0 to disable)
max-prestiges: 10

# Math expression – variables: baseExperience, level, prestige, maxPrestiges
formula: "baseExperience * (1 + 0.05 * level + 0.005 * level^2) * (1 + 0.3 * (prestige / maxPrestiges))"

# Whether the player may leave this job via /zjobs leave
can-leave: true

# Whether the player may join this job via /zjobs join
# false → must be added to default-jobs in config.yml
can-join: true

actions: [ ... ]   # see below
rewards:  [ ... ]  # see below
```

### Action Types

| Type | Trigger |
|---|---|
| `BLOCK_BREAK` | Breaking a block (by `material` or `tag`) |
| `FARMING` | Harvesting a fully-grown crop |
| `KILL_ENTITY` | Killing an entity |
| `FISHING` | Catching an item while fishing |
| `TAME` | Taming an animal |
| `ENCHANT` | Enchanting an item on an enchanting table |
| `BREW` | Completing a brewing stand recipe |
| `FORGE` | Clicking the result slot in a Forge menu |
| `RAFINE` | Collecting a refined item from the Rafine menu |
| `NEXO` | Interacting with a Nexo custom item (custom item ID required) |

### Action Fields

| Field | Type | Description |
|---|---|---|
| `type` | string | Action type (see table above) |
| `material` | string | Vanilla Minecraft material name |
| `tag` | string | Bukkit tag (e.g. `LOGS`, `COAL_ORES`, `MINEABLE_PICKAXE`) |
| `display-material` | string | Material shown in the job info GUI |
| `display-name` | string | Override name shown in the GUI |
| `entity` | string | Entity type for `KILL_ENTITY` / `TAME` |
| `enchantment` | string | Enchantment name for `ENCHANT` |
| `minimum-cost` | int | Minimum level cost threshold for `ENCHANT` |
| `potion-type` | string | Potion effect type for `BREW` |
| `potion-material` | string | `POTION`, `SPLASH_POTION`, or `LINGERING_POTION` |
| `experience` | double | Fixed XP awarded |
| `money` | double | Fixed money awarded |
| `experience-formula` | string | Math/placeholder formula overriding `experience` |
| `money-formula` | string | Math/placeholder formula overriding `money` |

Both `experience-formula` and `money-formula` support **PlaceholderAPI** placeholders (resolved at the time of the action) and arbitrary math expressions (e.g. `"%zjobs_rafine_percent_inverse% * 5"`).

**Examples**

```yaml
# BLOCK_BREAK – using a tag
- type: BLOCK_BREAK
  display-material: DIAMOND_ORE
  tag: DIAMOND_ORES
  experience: 15
  money: 0.20

# BLOCK_BREAK – specific material
- type: BLOCK_BREAK
  material: STONE
  experience: 1
  money: 0.10

# KILL_ENTITY
- type: KILL_ENTITY
  entity: ENDER_DRAGON
  experience: 15
  money: 3.0

# FARMING
- type: FARMING
  material: WHEAT
  experience: 0.9
  money: 0.20

# FISHING
- type: FISHING
  material: COD
  experience: 5
  money: 2.0

# TAME
- type: TAME
  entity: WOLF
  experience: 2
  money: 0.20

# ENCHANT – any enchant costing ≥ 30 levels
- type: ENCHANT
  minimum-cost: 30
  experience: 50
  money: 5.0

# ENCHANT – specific enchantment
- type: ENCHANT
  enchantment: SHARPNESS
  experience: 20
  money: 2.0

# BREW – normal potion
- type: BREW
  potion-type: STRENGTH
  experience: 20
  money: 5.0

# BREW – splash potion
- type: BREW
  potion-type: REGENERATION
  potion-material: SPLASH_POTION
  experience: 25
  money: 5.0

# Dynamic formula using a placeholder
- type: RAFINE
  experience-formula: "%zjobs_rafine_percent_inverse% * 5"
  money: 1.0
```

### Reward Action Types

Rewards are fired when a player reaches a specific `level`/`prestige` combination.  
Use `level: -1` and/or `prestige: -1` to match **every** level-up or prestige-up.

| Reward action type | Effect |
|---|---|
| `sound` | Plays a Bukkit sound to the player |
| `message` | Sends one or more chat messages |
| `console_commands` | Runs commands from console (`%player%` placeholder supported) |
| `zjobs_add_points` | Gives the player job points |

```yaml
rewards:
  # Fires on every level-up regardless of prestige
  - level: -1
    prestige: -1
    actions:
      - type: sound
        sound: ENTITY_PLAYER_LEVELUP
      - type: message
        messages:
          - "&8[&aJobs&8] &fYou are now level &e%level%&f in &aMiner&f!"
      - type: zjobs_add_points
        points: 1

  # One-time reward at level 10, prestige 1
  - level: 10
    prestige: 1
    actions:
      - type: console_commands
        commands:
          - "eco give %player% 1000"
          - "broadcast %player% just hit level 10 Miner!"
```

---

## Forge System

The **Forge** system lets players craft custom items by depositing ingredients into a GUI slot, then clicking a result slot that may fail or succeed.

### `items.yml`

Defines what each recipe produces and its failure chance.

```yaml
items:
  "nexo:amethyst_sword_commun":          # Output item ID (Nexo or vanilla)
    recipe:
      - "nexo:amethyst_gem_commune:2"    # "item-id:amount"
      - "STICK:1"
    fail: 20                             # % chance the forge fails (0–100)
```

### `forge_items_whitelist.yml`

Controls which items can be placed in a Forge input slot.  
Remove the `whitelist` key entirely to allow any ingredient that appears in at least one recipe.

```yaml
whitelist:
  - nexo:amethyst_gem_commune
  - STICK
```

### `forge_tiers.yml`

Maps Nexo item ID suffixes to rarity tiers.  
The tier affects substitution rules and downgrade behaviour when a forge fails.

```yaml
tiers:
  COMMUN:
    suffixes:
      - value: "_commun"
        feminine: false
      - value: "_commune"
        feminine: true
  PEU_COMMUNE:
    suffixes:
      - value: "_peu_commun"
        feminine: false
      - value: "_peu_commune"
        feminine: true
  RARE:
    suffixes:
      - value: "_rare"
        feminine: false
  EPIQUE:
    suffixes:
      - value: "_epique"
        feminine: false
  LEGENDAIRE:
    suffixes:
      - value: "_legendaire"
        feminine: false
```

Accepted tier names (from lowest to highest rarity): `COMMUN` → `PEU_COMMUNE` → `RARE` → `EPIQUE` → `LEGENDAIRE`.

---

## Refine (Rafine) System

The **Rafine** system allows players to refine raw items (e.g. gems) into higher-quality versions through a timed process.

How it works:
1. Items in a player's inventory display a percentage in their name (e.g. `Amethyste (30%)`).
2. The player deposits an item into a Rafine GUI input slot.
3. A random timer between **60 s** and **180 s** begins.
4. When the timer expires, the player clicks the result slot:
   - `random(1–100) ≤ percent` → **success**: the player receives the refined item.
   - Otherwise → **failure**: the item is destroyed.

The `experience-formula` / `money-formula` fields in a `RAFINE` action can reference `%zjobs_rafine_percent_inverse%` to scale rewards based on how hard the refine was.

---

## Commands

The main command is `/zjobs` (alias: `/jobs`).

### Player Commands

| Command | Description | Permission |
|---|---|---|
| `/zjobs` | Opens the jobs GUI | `zjobs.use` |
| `/zjobs join <job>` | Join a job | `zjobs.join` |
| `/zjobs leave <job>` | Request to leave a job | `zjobs.leave` |
| `/zjobs leaveconfirm <job>` | Confirm leaving a job | `zjobs.leave.confirm` |
| `/zjobs reload` | Reload all plugin configuration files | `zjobs.reload` |

### Admin Commands

All admin sub-commands live under `/zjobs admin` (alias: `/zjobs a`).

#### Level

| Command | Description |
|---|---|
| `/zjobs admin level add <player> <job> <amount>` | Add levels to a player's job |
| `/zjobs admin level remove <player> <job> <amount>` | Remove levels from a player's job |
| `/zjobs admin level set <player> <job> <level>` | Set a player's job level |

#### Experience

| Command | Description |
|---|---|
| `/zjobs admin experience add <player> <job> <amount>` | Add experience |
| `/zjobs admin experience remove <player> <job> <amount>` | Remove experience |
| `/zjobs admin experience set <player> <job> <amount>` | Set experience |

#### Prestige

| Command | Description |
|---|---|
| `/zjobs admin prestige add <player> <job> <amount>` | Add prestiges |
| `/zjobs admin prestige remove <player> <job> <amount>` | Remove prestiges |
| `/zjobs admin prestige set <player> <job> <prestige>` | Set prestige |

#### Points

| Command | Description |
|---|---|
| `/zjobs admin points add <player> <amount>` | Add job points |
| `/zjobs admin points remove <player> <amount>` | Remove job points |
| `/zjobs admin points set <player> <amount>` | Set job points |
| `/zjobs admin points info <player>` | Show a player's points |

#### Boost

| Command | Description |
|---|---|
| `/zjobs admin boost create <player> <amount> <xp-boost> <money-boost> [jobs] [actions] [targets]` | Create a boost |
| `/zjobs admin boost remove <player> <boost-id>` | Remove a boost by ID |
| `/zjobs admin boost show <player>` | List a player's active boosts |

#### Other Admin Commands

| Command | Description |
|---|---|
| `/zjobs admin info` | Show block/action debug info (requires `zjobs.admin.block.info`) |
| `/zjobs admin show <player>` | Show a player's job data |
| `/zjobs admin reward set <player> <reward-id> <true\|false>` | Mark a reward as claimed / unclaimed |

---

## Permissions

| Permission | Description |
|---|---|
| `zjobs.use` | Open the jobs GUI (`/zjobs`) |
| `zjobs.reload` | Reload the plugin |
| `zjobs.join` | Join a job |
| `zjobs.leave` | Leave a job |
| `zjobs.leave.confirm` | Confirm leaving a job |
| `zjobs.admin.use` | Access `/zjobs admin` |
| `zjobs.admin.level` | Access level sub-commands |
| `zjobs.admin.level.add` | Add levels |
| `zjobs.admin.level.set` | Set levels |
| `zjobs.admin.level.remove` | Remove levels |
| `zjobs.admin.experience` | Access experience sub-commands |
| `zjobs.admin.experience.add` | Add experience |
| `zjobs.admin.experience.set` | Set experience |
| `zjobs.admin.experience.remove` | Remove experience |
| `zjobs.admin.prestige` | Access prestige sub-commands |
| `zjobs.admin.prestige.add` | Add prestiges |
| `zjobs.admin.prestige.set` | Set prestige |
| `zjobs.admin.prestige.remove` | Remove prestiges |
| `zjobs.admin.points` | Access points sub-commands |
| `zjobs.admin.points.add` | Add points |
| `zjobs.admin.points.set` | Set points |
| `zjobs.admin.points.remove` | Remove points |
| `zjobs.admin.points.info` | View a player's points |
| `zjobs.admin.boost` | Access boost sub-commands |
| `zjobs.admin.boost.create` | Create boosts |
| `zjobs.admin.boost.show` | View boosts |
| `zjobs.admin.boost.remove` | Remove boosts |
| `zjobs.admin.block.info` | Debug block/action info |
| `zjobs.admin.show` | View another player's job data |
| `zjobs.admin.reward.set` | Force-set a reward claim state |
| `zjobs.limit.player` | Job slot limit: 1 (default) |
| `zjobs.limit.vip` | Job slot limit: 2 |
| `zjobs.limit.admin` | Job slot limit: 3 |

---

## PlaceholderAPI Placeholders

All placeholders use the `%zjobs_<name>%` format.

### Player Placeholders

| Placeholder | Description |
|---|---|
| `%zjobs_has_<jobId>%` | `true`/`false` – whether the player has joined the job |
| `%zjobs_level_<jobId>%` | Player's current level in the given job |
| `%zjobs_prestige_<jobId>%` | Player's current prestige in the given job |
| `%zjobs_points%` | Total job points the player has |
| `%zjobs_reward_is_claim_<rewardId>%` | `true`/`false` – whether a reward has been claimed |

### Job Placeholders

| Placeholder | Description |
|---|---|
| `%zjobs_max_level_<jobId>%` | Maximum level configured for the job |

### Boost Placeholder

| Placeholder | Description |
|---|---|
| `%zjobs_boosts%` | Formatted list of the player's active boosts (format defined in `config.yml` under `placeholder-boosts`) |

### Rafine Placeholders

| Placeholder | Description |
|---|---|
| `%zjobs_rafine_status%` | `idle` / `refining` / `ready` |
| `%zjobs_rafine_time%` | Time remaining formatted as `m:ss` (empty when idle) |
| `%zjobs_rafine_seconds%` | Raw remaining seconds (`0` when idle) |
| `%zjobs_rafine_percent%` | Refining success chance of the current deposit |
| `%zjobs_rafine_percent_inverse%` | `100 - percent` (useful in money/experience formulas) |
| `%zjobs_rafine_count%` | Number of items currently in refine slots |
| `%zjobs_rafine_ready%` | `true` when at least one deposit is ready to collect |

**Example formula using rafine placeholder:**
```yaml
- type: RAFINE
  experience-formula: "%zjobs_rafine_percent_inverse% * 5"
  money-formula: "%zjobs_rafine_percent% * 0.1"
```

---

## Storage

| Backend | Notes |
|---|---|
| `SQLITE` | Default; suitable for testing only. Some features (e.g. offline data sync) are not fully supported. |
| `MYSQL` | Recommended for production servers. |
| `MARIADB` | Recommended for production servers. |

Configure the backend in `config.yml` under `storage-type` and `database-configuration`.

---

## Economy / Currency

The plugin supports multiple economy backends:

- **Vault** – set `default-economy: VAULT`.
- **zMenu Currencies** – set `default-economy: <currency-name>` matching a currency registered in zMenu (e.g. `"money"`).

Money can be awarded per-action through the `money` field or dynamically through `money-formula`.

---

## Boost System

Boosts temporarily multiply the experience and/or money a player earns.

Each boost has:
- An **amount** – the total number of times it can trigger.
- An **experience boost** multiplier.
- A **money boost** multiplier.
- Optional filters: specific **jobs**, **action types**, or **targets** (e.g. a specific block/entity).

**Create a boost example:**
```
/zjobs admin boost create Steve 100 1.5 1.2
```
> Gives Steve a boost that applies for 100 actions, with ×1.5 XP and ×1.2 money, on all jobs and actions.

**Scoped boost example:**
```
/zjobs admin boost create Steve 50 2.0 1.0 miner BLOCK_BREAK DIAMOND_ORE
```
> Gives Steve 50 double-XP actions, but only when mining diamond ore as the Miner.

---

> **Documentation:** https://zjobs.groupez.dev  
> **Author:** Maxlego08 – https://groupez.dev
