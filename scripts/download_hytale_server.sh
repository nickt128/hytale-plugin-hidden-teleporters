#!/bin/sh
if [ -e "hytale-server/Server/HytaleServer.jar" ]; then
    mkdir -p libs
    cp hytale-server/Server/HytaleServer.jar libs/HytaleServer.jar
    echo "hytale server already downloaded. skipping..."
    exit 0
fi
if [ ! -e "hytale-downloader" ]; then
    curl -Lo hytale-downloader.zip https://downloader.hytale.com/hytale-downloader.zip
    unzip hytale-downloader.zip hytale-downloader-linux-amd64
    mv hytale-downloader-linux-amd64 hytale-downloader
    rm hytale-downloader.zip
fi
./hytale-downloader -download-path hytale-server.zip
mkdir -p hytale-server
unzip -o -d hytale-server hytale-server.zip
mkdir -p libs
cp hytale-server/Server/HytaleServer.jar ./libs/HytaleServer.jar
#rm hytale-server.zip
