#!/bin/bash

javac -sourcepath src -d build/classes -cp /minecraft/src/my_mods/Minecraft-Server-Mod/src/*.java -cp /minecraft/server/bin/minecraft_server.jar src/Stats.java

jar cvf export/Stats.jar MANIFEST.MF -C build/classes Stats.class -C build/classes StatsHandler.class 
#jar cvf export/Stats.jar MANIFEST.MF -C build/classes Stats.class -C build/classes Stats\$1.class -C build/classes StatsHandler.class -C build/classes Stats\$2.class

