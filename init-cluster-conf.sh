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
done

# copy akka.conf
cp ./conf/akka-1.1.1.1.conf ./karaf/target/assembly/configuration/initial/akka.conf
cp ./conf/akka-1.1.1.2.conf ./karaf/target/assembly1/configuration/initial/akka.conf
cp ./conf/akka-1.1.1.3.conf ./karaf/target/assembly2/configuration/initial/akka.conf
