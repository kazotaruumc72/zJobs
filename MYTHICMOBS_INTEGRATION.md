# MythicMobs Integration for zJobs

This update adds support for MythicMobs custom entities in the KILL_ENTITY job action type.

## Features

- **MythicMobs Entity Support**: You can now create job actions for killing MythicMobs custom entities
- **Simple Syntax**: Use the `mm:` prefix followed by the MythicMobs internal name
- **Backwards Compatible**: Vanilla entities continue to work exactly as before
- **Automatic Detection**: The plugin automatically detects when MythicMobs is installed and enables the integration

## Configuration

### Basic Example

```yaml
- type: KILL_ENTITY
  entity: "mm:SkeletonKing"
  experience: 50
  money: 10
```

### Full Job Example

See `src/main/resources/jobs/mythic_hunter_example.yml` for a complete example job configuration that includes both MythicMobs and vanilla entities.

### Configuration Fields

- `type`: Must be `KILL_ENTITY`
- `entity`: The entity identifier
  - For MythicMobs: `"mm:MobInternalName"` (e.g., `"mm:SkeletonKing"`)
  - For vanilla: The EntityType name (e.g., `ZOMBIE`, `SKELETON`)
- `experience`: Experience points awarded for killing the entity
- `money`: Money awarded for killing the entity

## How It Works

1. **Plugin Detection**: When zJobs starts, it checks if MythicMobs is installed
2. **Event Listening**: If MythicMobs is present, zJobs registers a listener for `MythicMobDeathEvent`
3. **Action Matching**: When a MythicMobs mob is killed, zJobs dispatches an action with the format `mm:MobInternalName`
4. **Reward Granting**: If a matching job action exists, the player receives the configured rewards

## Technical Details

### New Files

- `src/main/java/fr/maxlego08/jobs/hooks/MythicMobsHook.java`: Hook class for detecting MythicMobs entities
- `src/main/java/fr/maxlego08/jobs/hooks/MythicMobsListener.java`: Event listener for MythicMobDeathEvent

### Modified Files

- `build.gradle.kts`: Added MythicMobs dependency and repository
- `src/main/java/fr/maxlego08/jobs/JobLoader.java`: Updated to handle `mm:` prefix in entity configuration
- `src/main/java/fr/maxlego08/jobs/JobsPlugin.java`: Added MythicMobs hook initialization
- `src/main/java/fr/maxlego08/jobs/zcore/utils/plugins/Plugins.java`: Added MYTHICMOBS enum value

### Implementation Pattern

The MythicMobs integration follows the same pattern as the existing Nexo integration:

1. A hook class provides API methods for interacting with MythicMobs
2. A listener class handles MythicMobs-specific events
3. The job loader recognizes the `mm:` prefix and creates CustomAction instances
4. The plugin enables the integration only when MythicMobs is detected

## Requirements

- MythicMobs 5.0+ (tested with 5.7.2)
- The MythicMobs plugin must be installed and enabled on your server

## Examples

### Boss Hunting Job

```yaml
name: "ʙᴏss ʜᴜɴᴛᴇʀ"
base-experience: 200
max-levels: 50
actions:
  - type: KILL_ENTITY
    entity: "mm:SkeletonKing"
    experience: 100
    money: 50

  - type: KILL_ENTITY
    entity: "mm:AncientDragon"
    experience: 200
    money: 100

  - type: KILL_ENTITY
    entity: "mm:DemonLord"
    experience: 150
    money: 75
```

### Mixed Entity Job

```yaml
name: "ᴍᴏɴsᴛᴇʀ sʟᴀʏᴇʀ"
base-experience: 150
max-levels: 100
actions:
  # MythicMobs bosses
  - type: KILL_ENTITY
    entity: "mm:SkeletonKing"
    experience: 50
    money: 10

  # Regular vanilla mobs
  - type: KILL_ENTITY
    entity: ZOMBIE
    experience: 1
    money: 0.2

  - type: KILL_ENTITY
    entity: SKELETON
    experience: 1
    money: 0.2
```

## Troubleshooting

### MythicMobs entities not giving rewards

1. Check that MythicMobs is installed and enabled: `/mm info`
2. Verify the internal name matches your MythicMobs configuration
3. Check the zJobs console logs for "Using MythicMobs" message on startup
4. Ensure the entity name in your job config has the `mm:` prefix
5. Verify the player is enrolled in a job that has the MythicMobs entity configured

### Finding MythicMobs internal names

The internal name is defined in your MythicMobs mob configuration file. For example:

```yaml
SkeletonKing:
  Type: SKELETON
  Health: 200
  # ... other config
```

The internal name is `SkeletonKing`, so you would use `"mm:SkeletonKing"` in zJobs.

## Notes

- MythicMobs entity names are case-sensitive
- The `mm:` prefix is required to distinguish MythicMobs entities from vanilla entities
- If MythicMobs is not installed, job configs with `mm:` entities will be loaded but won't grant rewards
- You can mix MythicMobs and vanilla entities in the same job
