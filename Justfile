download-hytale-server:
    scripts/download_hytale_server.sh

start-hytale-server:
    cd hytale-server && java -jar Server/HytaleServer.jar -assets Assets.zip; cd -

update-mod:
    #!/usr/bin/env sh
    gradle build
    cp build/libs/HiddenTeleportersPlugin-1.0-SNAPSHOT.jar hytale-server/mods/
