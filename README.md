## ⚠️ Disclaimer
I'm not a professional developer, just a regular person coding with the help of AI.

This project may not follow best practices and could contain bugs.

# Beacon Waypoints
### This plugin gives beacons extra functionality by letting players use beacons to fast travel between them! Install this plugin by putting the jar file into your plugins folder and starting/restarting the server.


Supports versions i dont know but it work perfectly in 1.21.11

This plugin uses bStats.

## Features
- Create public or private waypoints at beacons
- Configurable icon chooser for waypoints
- Discovery Mode: Players can discover public waypoints by interacting with the beacon and can only teleport between discovered waypoints
- Various payment options for teleportation
- language.yml file for custom translations


## Commands
- /waypoint <name> <public | private> - Create a public or private waypoint for the beacon the player is standing on
- /waypoints reload - Reloads the config
- /waypoint share <player> - share private waypoints with other players


## Demo
https://www.youtube.com/watch?v=-dx9NGfIUa0

[![](http://img.youtube.com/vi/-dx9NGfIUa0/0.jpg)](https://www.youtube.com/watch?v=-dx9NGfIUa0 "Beacon Waypoints Plugin")


Note: Teleportation between beacons requires the beacon to have no blocks inside it, including transparent blocks like glass. Bedrock is an exception, so this plugin will work in the Nether.



## Permissions
- BeaconWaypoints.createWaypoints: Allows players to create waypoints
- BeaconWaypoints.useWaypoints: Allows players to use waypoints
- BeaconWaypoints.usePrivateWaypoints: Allows players to create and teleport to private waypoints
- BeaconWaypoints.breakWaypointBeacons: Allows players to break beacons that have waypoints
- BeaconWaypoints.manageAllWaypoints: Allows players to edit, pin, or remove all public waypoints and corresponding beacons
- BeaconWaypoints.reload: Allows players to reload the config


## Configuration
- max-public-waypoints: The maximum amount of public waypoints that can exist at once on the server (default: 100)
- max-private-waypoints: The maximum amount of private waypoints that each player can have (default: 30)
- force-alphanumeric-names: Force waypoint names to be alphanumeric when they are created
- public-waypoint-menu-rows: The number of rows the public waypoint selection menu will show per page, not including the row for page navigation (default: 3, range: 1-5)
- private-waypoint-menu-rows: The number of rows the private waypoint selection menu will show per page, not including the row for page navigation (default: 2, range: 1-5)
- instant-teleport: Activate teleportation as soon as the destination is chosen without a warmup animation (default: false)
- disable-animations: Disable the particle animations when teleporting through a beacon (default: false)
- launch-player: Launch the player when teleporting through a beacon (default: true)
- launch-player-height: The y-level players will launch into the air before teleporting to the destination. Anti-cheat plugins may not allow players to go above a certain height, so adjust this as needed. The minimum value is the world height. (default: 576)
- disable-group-teleporting: By default, beacons teleport anyone standing on top of them. If you want to limit the teleportation to only the player who chooses the destination, set this to true. (default: false)
- allow-beacon-break-by-owner: If the BeaconWaypoints.breakWaypointBeacons permission is disabled for a player, this will still allow them to break a beacon if all waypoints attached to it are owned by them. The owner of a beacon is the player who placed it. (default: true)
- payment-mode: none, xp, or money (default: none)
- xp-cost-per-chunk: The cost per chunk between two waypoints using the XP payment mode (default: 0)
- xp-cost-dimension: The cost to teleport between dimensions using the XP payment mode (default: 0)
- money-cost-per-chunk: The cost per chunk between two waypoints using the money payment mode (default: 0)
- money-cost-dimension: The cost to teleport between dimensions using the money payment mode (default: 0)
- cost-multiplier: A multiplier that affects the cost based on distance. The formula used is cost*(distance^multiplier) (default: 0)
- required-items: a list of items with different properties regarding payment (default: empty)
- banned-items: a list of items that will not allow players to teleport if they are in their inventory (default: empty)
- discover-mode: Enable discovery mode which only allows players to teleport to waypoints they have discovered by interacting with their beacon (default: false)
- allow-all-worlds: Allow waypoints to be created in any world (default: true)
- allowed-worlds: List of worlds that allow waypoints based on folder name if allow-all-worlds is disabled (default: world, world_nether, world_the_end)
- waypoint-icons: List of items that can be used for waypoint icons, the order given here is the same order that will be in the icon picker menu (default includes 111 items)

Note: If WorldEdit is used to delete a beacon, the waypoint will not be deleted. You will need to manually place back the beacon and break it, or use the setblock and fill commands instead.
