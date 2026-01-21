download-hytale-server:
    scripts/download_hytale_server.sh

start-hytale-server:
    cd hytale-server && java -jar Server/HytaleServer.jar -assets Assets.zip; cd -

update-mod:
    #!/usr/bin/env sh
    rm build/libs/HiddenTeleportersPlugin-*.jar
    gradle build
    rm hytale-server/mods/HiddenTeleportersPlugin-*.jar
    cp build/libs/HiddenTeleportersPlugin-*.jar hytale-server/mods/
