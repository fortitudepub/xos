#!/bin/bash

# patch configuration file
mkdir -p ./karaf/target/assembly/configuration/initial
cp ./conf/modules.conf ./karaf/target/assembly/configuration/initial/
cp ./conf/module-shards.conf ./karaf/target/assembly/configuration/initial/

# fix 3.0.1 issue, no need anymore since I have fixed the issue that we can not use 3.0.3.
# now we run 3.0.3 karaf.
#sed -i 's/\(.*URLHandlers\),.*/\1/;s/.*{karaf\.framework}.*//' ./karaf/target/assembly/etc/config.properties

# patch org.ops4j.pax.url.mvn.repositories to use local repo.
# note, if you adjust m2 repo, change this.
mylocalrepo=${HOME}/.m2/repository
# check whether patched, if patched, skip the patch process.
modified=`cat ./karaf/target/assembly/etc/org.ops4j.pax.url.mvn.cfg | grep mylocalrepo`
if [ "$?" -ne "0" ]; then
    # should enable snapshots since we currently work on dev.
    sed -i  "/system\.repository/afile:${mylocalrepo}@id=mylocalrepo@snapshots,\\\\" ./karaf/target/assembly/etc/org.ops4j.pax.url.mvn.cfg
fi

# patch features before startup, disable region feature which will cause exception and prevent log:display work properly.
sed -i "s/region,//" ./karaf/target/${dir}/etc/org.apache.karaf.features.cfg

cd ./karaf/target/assembly/bin
./karaf
