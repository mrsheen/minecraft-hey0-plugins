#!/bin/bash

 javac -sourcepath src -d build/classes -cp /minecraft/src/my_mods/Minecraft-Server-Mod/src/*.java -cp /minecraft/server/bin/minecraft_server.jar src/GriefAlert.java src/GriefAlertData.java

jar cvfM export/GriefAlert.jar -C build/classes/ GriefAlert.class -C build/classes GriefAlertData.class -C src META-INF

#cp build/jar/GriefAlert.jar export/

#cd export

#tar zcvf mapper_plugin.1.0.tgz *
