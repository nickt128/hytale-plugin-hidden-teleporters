package org.zone404.plugin;

import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import org.zone404.plugin.events.MapEvents;

import javax.annotation.Nonnull;

public class HiddenTeleportersPlugin extends JavaPlugin {
    public HiddenTeleportersPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        getEventRegistry().registerGlobal(PlayerReadyEvent.class, MapEvents::onPlayerReady);
        this.getCommandRegistry().registerCommand(new TeleportersCommand("teleporters", "An example command", false));
        MapEvents.applyToExisting();
    }

    @Override
    protected void shutdown() {
        MapEvents.reset();
    }
}
