#!/bin/bash

# Usage: ./update-markers.sh
# Example: while true; do ./update-markers.sh; done

MCHOME=/minecraft
GMAP=$MCHOME/apps/Minecraft-Overviewer/gmap.py
SERVERWORLD=$MCHOME/world
OUTPUT=$MCHOME/maps

# Make sure we are in the right directory
cd $MCHOME

# Update markers 
$PYTHON $GMAP --markers $SERVERWORLD $OUTPUT
RETURNVAL=$?
if [ $RETURNVAL -ne 0 ] ; then
 echo "Update failed"
 exit 1
fi


