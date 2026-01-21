download-hytale-server:
    scripts/download_hytale_server.sh

start-hytale-server:
    cd hytale-server && java -jar Server/HytaleServer.jar -assets Assets.zip; cd -

update-mod:
    #!/usr/bin/env sh
    rm build/libs/HiddenTeleporters-*.jar
    gradle build
    rm hytale-server/mods/HiddenTeleporters-*.jar
    cp build/libs/HiddenTeleporters-*.jar hytale-server/mods/
