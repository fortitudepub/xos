#!/bin/bash

echo "CALL ME AFTER YOU HAVE DO A FRESH COMPILE AND WANT TO SETUP A 3 NODE HA CLUSTER."

# init the base assembly configuration.
mkdir -p ./karaf/target/assembly/configuration/initial/
cp ./conf/modules.conf ./karaf/target/assembly/configuration/initial/

# Because we use namespace, we will need sudo to switch ns, so use sudo to ensure
# logs generated as root user be deleted.
sudo rm -rf ./karaf/target/assembly1
sudo rm -rf ./karaf/target/assembly2
cp -af ./karaf/target/assembly ./karaf/target/assembly1
cp -af ./karaf/target/assembly ./karaf/target/assembly2


# copy module-shards.conf
dirs="assembly assembly1 assembly2"
for dir in ${dirs}; do
    mkdir -p ./karaf/target/${dir}/configuration/initial/
    cp ./conf/module-shards-3-nodes.conf ./karaf/target/${dir}/configuration/initial/module-shards.conf
    
    # patch org.ops4j.pax.url.mvn.repositories to use local repo.
    # note, if you adjust m2 repo, change this.
    mylocalrepo=${HOME}/.m2/repository
    # check whether patched, if patched, skip the patch process.
    modified=`cat ./karaf/target/${dir}/etc/org.ops4j.pax.url.mvn.cfg | grep mylocalrepo`
    if [ "$?" -ne "0" ]; then
	# should enable snapshots since we currently work on dev.
	sed -i  "/system\.repository/afile:${mylocalrepo}@id=mylocalrepo@snapshots,\\\\" ./karaf/target/${dir}/etc/org.ops4j.pax.url.mvn.cfg
    fi

    # patch features before startup, disable region feature which will cause exception and prevent log:display work properly.
    sed -i "s/region,//" ./karaf/target/${dir}/etc/org.apache.karaf.features.cfg
done

# copy akka.conf
cp ./conf/akka-1.1.1.1.conf ./karaf/target/assembly/configuration/initial/akka.conf
cp ./conf/akka-1.1.1.2.conf ./karaf/target/assembly1/configuration/initial/akka.conf
cp ./conf/akka-1.1.1.3.conf ./karaf/target/assembly2/configuration/initial/akka.conf
