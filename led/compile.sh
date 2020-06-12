#!/bin/sh

cc=${CC:-cc}
target=${TARGET:-main}

$cc -Wall -O2 -o $target main.c -lwiringPi -lwiringPiDev -lpthread -lm -lcrypt -lrt -lmosquitto
