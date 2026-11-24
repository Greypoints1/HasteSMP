# HasteSMP (Paper 1.21.11)

## Features
- Default attack speed: 4.0
- Minimum attack speed: 3.5
- Maximum attack speed: 4.5
- Player death: loses 0.1 attack speed (including non-player deaths)
- Player kill: drops one tagged Nether Star reward only if the victim had more than 3.5 attack speed; victims at 3.5 do not drop a star
- Right-click the tagged reward: gain 0.1 attack speed
- Attack speed is stored in the player's persistent data and restored on join/respawn

Ordinary Nether Stars do not grant attack speed.

## Build
Requirements:
- Java 21 JDK
- Maven
- Paper 1.21.11

From this folder, run:

```bash
mvn clean package
```

The plugin JAR will be created at:

`target/HasteSMP-1.0.0.jar`

Put that JAR in your Paper server's `plugins` folder, then restart the server.

## Notes
This is a Maven source project. The ZIP contains the source and build configuration; the compiled JAR is produced by running the build command.
