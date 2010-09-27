#!/bin/bash

# Usage: ./timed-run.sh
# Example: while true; do ./timed-run.sh; done

MCHOME=/minecraft
GMAP=$MCHOME/apps/Minecraft-Overviewer/gmap.py
SERVERWORLD=$MCHOME/world
WORLDSNAPSHOT=$MCHOME/world_snapshot
CACHE=$MCHOME/cache
OUTPUT=$MCHOME/maps



LOGPATH=$MCHOME/logs/$(date +%Y%m%d-%H%M).log

START=$(date +%s)

echo "Start at: $START" >> $LOGPATH

# Make sure we are in the right directory
cd $MCHOME

# Take snapshot of world
echo "Start snapshot at: $(date +%s)" >> $LOGPATH
rsync -a $SERVERWORLD/ $WORLD/
echo "End snapshot at: $(date +%s)" >> $LOGPATH

# Run incremental update
echo "Start incremental update at: $(date +%s)" >> $LOGPATH
echo "python $GMAP --cachedir=$CACHE $WORLDSNAPSHOT $OUTPUT" >> $LOGPATH
$PYTHON $GMAP --cachedir=$CACHE $WORLDSNAPSHOT $OUTPUT
RETURNVAL=$?
if [ $RETURNVAL -ne 0 ] ; then
 echo "Update failed"
 exit 1
fi
echo "End incremental update at: $(date +%s)" >> $LOGPATH

# Calculate end time
END=$(date +%s)
DIFF=$(( $END - $START))

echo "It took $DIFF seconds"
echo "It took $DIFF seconds" >> $LOGPATH
let "MINS=$DIFF / 60"
let "HOURS=$MINS / 60"
echo " or $MINS minutes"
echo " or $MINS minutes" >> $LOGPATH
echo " or $HOURS hours"
echo " or $HOURS hours" >> $LOGPATH

echo "End at: $END" >> $LOGPATH
echo "DIFF: $DIFF" >> $LOGPATH


