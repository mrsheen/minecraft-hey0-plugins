#!/bin/bash

for plugin in `ls -d */`; do echo $plugin; cd $plugin; ant deploy; cd ..; done

