download-hytale-server:
    scripts/download_hytale_server.sh

mvn-install-hytale-server:
    mvn install:install-file -Dfile=libs/HytaleServer.jar -DgroupId=com.hypixel.hytale -DartifactId=HytaleServer-parent -Dversion=1.0-SNAPSHOT -Dpackaging=jar

start-hytale-server:
    cd hytale-server && java -jar Server/HytaleServer.jar -assets Assets.zip --accept-early-plugins; cd -

update-mod:
    #!/usr/bin/env sh
    rm target/HiddenTeleporters-*.jar
    JAVA_HOME=/usr/lib/jvm/java-25-openjdk mvn package
    mkdir -p hytale-server/mods
    rm hytale-server/mods/HiddenTeleporters-*.jar
    cp target/HiddenTeleporters-*.jar hytale-server/mods/
