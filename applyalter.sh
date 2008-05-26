#!/bin/bash

xDEBUG="-Xdebug -Xnoagent -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5000"

# Platform dependent CLASSPATH separator
if [ "Windows_NT" = "$OS" ]
then
  CPS=\;
else
  CPS=:
fi

for i in `find . -name '*.jar'`; do 
  CP="$CP$CPS$i"
done

java $DEBUG -cp target/classes$CPS$CP ch.ips.g2.applyalter.ApplyAlter $*