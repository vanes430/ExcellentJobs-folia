#!/bin/bash

# Create libs directory
mkdir -p libs

# Download NightCore jar
echo "Downloading NightCore..."
curl -L -o libs/nightcore-2.7.15.jar https://github.com/vanes430/nightcore-folia/releases/download/latest/nightcore-2.7.15.jar

# Install to local maven repository so the pom can find it
# This fulfills the requirement of making the pom use this jar
echo "Installing NightCore to local Maven repository..."
mvn install:install-file -Dfile=libs/nightcore-2.7.15.jar -DgroupId=su.nightexpress.nightcore -DartifactId=main -Dversion=2.7.15 -Dpackaging=jar

# Download EconomyBridge jar
echo "Downloading EconomyBridge..."
curl -L -o libs/economy-bridge-1.2.1.jar https://github.com/vanes430/economy-bridge-folia/releases/download/latest/economy-bridge-1.2.1.jar

# Install EconomyBridge to local Maven repository
echo "Installing EconomyBridge to local Maven repository..."
mvn install:install-file -Dfile=libs/economy-bridge-1.2.1.jar -DgroupId=su.nightexpress.economybridge -DartifactId=economy-bridge -Dversion=1.2.1 -Dpackaging=jar

# Build the project
echo "Building project..."
mvn clean
mvn install
