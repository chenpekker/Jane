#!/bin/bash
#if we can't find the Jane jar file in the working directory, try the directory
#the script is located in
janepath="."
if [ ! -e "$janepath/lib/Jane.jar" ]
then
  janepath=$(dirname $0)
fi

#if we still can't find it, print an error message
if [ ! -f "$janepath/lib/Jane.jar" ]
then
  echo "Error: A folder named lib containing Jane.jar  must be located either in"
  echo "the directory you launch this script from or in the directory the script"
  echo "is actually located in."
  exit 1
fi

$janepath/lib/jc.sh gui.Design