#!/bin/bash
if [ "$SERVER_TYPE" == "MCPI" ];then
  cd mcpi-scratch; python mcpi-scratch.py
else 
  java -Xmx1024M -Xms1024M -jar spigot*.jar nogui
fi
