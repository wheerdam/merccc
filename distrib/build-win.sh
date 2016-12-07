#!/bin/bash
LAUNCH4J=../../launch4j
echo "- merccc win installer builder"
if [ ! -e ../store/MercControl.jar ]
then
    echo "- merccc static package not found, building"
    cd ..
    ant package-for-store
    cd distrib
fi
# cp -v ../store/MercControl.jar ./merccc-`cat version`.jar
$LAUNCH4J/launch4j l4j.xml
cat merccc-win.nsi | sed "s/MERCCCVERSION/`cat version`/" > merccc-win-`cat version`.nsi
makensis merccc-win-`cat version`.nsi
echo - done
