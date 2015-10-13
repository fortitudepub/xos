#!/bin/sh

# patch configuration file
mkdir -p ./karaf/target/assembly/configuration/initial
cp ./conf/modules.conf ./karaf/target/assembly/configuration/initial/
cp ./conf/module-shards.conf ./karaf/target/assembly/configuration/initial/

# patch org.ops4j.pax.url.mvn.repositories to use local repo.
# note, if you adjust m2 repo, change this.
mylocalrepo=${HOME}/.m2/repository
# check whether patched, if patched, skip the patch process.
modified=`cat ./karaf/target/assembly/etc/org.ops4j.pax.url.mvn.cfg | grep mylocalrepo`
if [ "$?" -ne "0" ]; then
    # should enable snapshots since we currently work on dev.
    sed -i  "/system\.repository/afile:${mylocalrepo}@id=mylocalrepo@snapshots,\\\\" ./karaf/target/assembly/etc/org.ops4j.pax.url.mvn.cfg
fi

cd ./karaf/target/assembly/bin
./karaf
