#!/bin/bash
cd ..
ant clean
rm -vrf store
cd distrib
rm -v merccc-*`cat version`*
rm -vr deb/usr
rm -v deb/DEBIAN/control
