#!/bin/bash
#if we can't find the Jane jar file in the working directory, try the directory
#the script is located in
janepath="."
if [ ! -e "$janepath/Jane.app" ]
then
  janepath=$(dirname $0)
fi

#if we still can't find it, print an error message
if [ ! -e "$janepath/Jane.app" ]
then
  echo "Error: The Jane applciation bundle must be located either in the directory you"
  echo "launch this script from or in the directory the script is actually"
  echo "located in."
  exit 1
fi

java -Xms50m -Xmx10G -Xss512k -XX:+UseParallelGC -XX:+UseParallelOldGC -XX:+AggressiveOpts -XX:+UseFastAccessorMethods -server -cp $janepath/Jane.app/Contents/Resources/Java/Jane.jar edu.hmc.jane.CLI $@