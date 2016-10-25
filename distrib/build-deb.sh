#!/bin/bash
echo "- merccc deb builder"
if [ -z "$(command -v fakeroot)" ]
then
  echo "- fakeroot not found"
  exit 1
fi
if [ ! -e ../store/MercControl.jar ]
then
	echo "- merccc static package not found, building"
	cd ..
	ant package-for-store
	cd distrib
fi
echo "- copying files to include in the package"
rm -rf ./deb/usr
mkdir ./deb/usr
mkdir ./deb/usr/bin
mkdir ./deb/usr/lib
mkdir ./deb/usr/lib/merccc
mkdir ./deb/usr/share
mkdir ./deb/usr/share/applications
cp -v ../store/MercControl.jar ./deb/usr/lib/merccc/merccc-`cat version`.jar
cp -v ../web/appicon.png ./deb/usr/lib/merccc
cp -v ./*.desktop ./deb/usr/share/applications
echo "- generating run script"
echo "#!/bin/sh" > ./deb/usr/bin/merccc
echo "java -jar /usr/lib/merccc/merccc-`cat version`.jar \$@" >> ./deb/usr/bin/merccc
chmod a+x ./deb/usr/bin/merccc
echo "- building deb"
fakeroot dpkg --build deb
mv -v deb.deb merccc-`cat version`.deb
echo "- done"
