#!/bin/bash
java -Xss512k -Xms50m -Xmx10G -XX:+UseParallelGC -XX:+UseParallelOldGC -XX:+AggressiveOpts -XX:+UseFastAccessorMethods -server -cp $(dirname $0)/Jane.jar edu.hmc.jane.$@