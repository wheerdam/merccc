#!/bin/bash
cd ..
ant clean
rm -vrf store
cd distrib
rm -v merccc-*`cat version`*
rm -v merccc.exe
rm -v md5sum*
rm -vr deb
