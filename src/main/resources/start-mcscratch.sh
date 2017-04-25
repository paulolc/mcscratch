#!/bin/bash
( cd mcpi-scratch; python mcpi-scratch.py ) & 
java -Xmx1024M -Xms1024M -jar *.jar nogui
