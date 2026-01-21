#!/bin/sh
if [ -e "libs/HytaleServer.jar" ]; then
    echo "hytale server already downloaded. skipping..."
    exit 0
fi
if [ ! -e "hytale-downloader" ]; then
    curl -Lo hytale-downloader.zip https://downloader.hytale.com/hytale-downloader.zip
    unzip hytale-downloader.zip hytale-downloader-linux-amd64
    mv hytale-downloader-linux-amd64 hytale-downloader
    rm hytale-downloader.zip
fi
./hytale-downloader -download-path libs/hytale-server.zip
cd libs
mkdir -p hytale-server
unzip -o -d hytale-server hytale-server.zip
mv hytale-server/Server/HytaleServer.jar ./HytaleServer.jar
#rm hytale-server.zip
#rm -r hytale-server
cd -
