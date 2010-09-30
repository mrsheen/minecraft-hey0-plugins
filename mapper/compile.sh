#!/bin/bash

javac -sourcepath src -d build/classes -cp /minecraft/src/my_mods/Minecraft-Server-Mod/src/*.java -cp /minecraft/server/bin/minecraft_server.jar src/mapper.java

jar cvf export/mapper.jar MANIFEST.MF -C build/classes mapper.class -C build/classes/ Marker.class -C build/classes WorldMap.class

