package org.zone404.plugins;

import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.OpenCustomUIInteraction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import org.zone404.plugins.events.MapEvents;
import org.zone404.plugins.pages.TeleporterSettingsPageSupplier;

import javax.annotation.Nonnull;

@SuppressWarnings("unused")
public class HiddenTeleportersPlugin extends JavaPlugin {
    public HiddenTeleportersPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        getEventRegistry().registerGlobal(PlayerReadyEvent.class, MapEvents::onPlayerReady);
        this.getCodecRegistry(OpenCustomUIInteraction.PAGE_CODEC).register("Teleporter", TeleporterSettingsPageSupplier.class, TeleporterSettingsPageSupplier.CODEC);
        MapEvents.applyToExisting();
    }

    @Override
    protected void shutdown() {
        MapEvents.reset();
    }
}
