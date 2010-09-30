#!/bin/bash

MODS=.
PLUGINS=/minecraft/server/bin/plugins

cp -v $MODS/griefalert/export/GriefAlert.jar $PLUGINS/GriefAlert.jar
cp -v $MODS/jump/export/Jump.jar $PLUGINS/Jump.jar
cp -v $MODS/cuboid/export/CuboidPlugin.jar $PLUGINS/CuboidPlugin.jar
cp -v $MODS/mapper/export/mapper.jar $PLUGINS/mapper.jar

