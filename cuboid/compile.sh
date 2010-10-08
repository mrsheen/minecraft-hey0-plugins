#!/bin/bash

javac -sourcepath src -d build/classes -cp /minecraft/src/my_mods/Minecraft-Server-Mod/src/*.java -cp /minecraft/server/bin/minecraft_server.jar src/*.java

jar cvfM export/CuboidPlugin.jar -C build/classes/ Cuboid.class -C build/classes/ CuboidPlugin.class -C build/classes ProtectedArea.class -C src META-INF

