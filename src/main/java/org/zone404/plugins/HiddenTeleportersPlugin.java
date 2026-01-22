package org.zone404.plugins;

import com.hypixel.hytale.builtin.teleport.TeleportPlugin;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.OpenCustomUIInteraction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.events.AllWorldsLoadedEvent;
import org.zone404.plugins.events.MapEvents;
import org.zone404.plugins.pages.TeleporterSettingsPageSupplier;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.util.logging.Level;

@SuppressWarnings("unused")
public class HiddenTeleportersPlugin extends JavaPlugin {
    private static HiddenTeleportersPlugin instance;
    public CustomTeleportPlugin myCustomTeleportPlugin;

    public HiddenTeleportersPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        myCustomTeleportPlugin = new CustomTeleportPlugin(init);
    }

    @Override
    protected void setup() {
        instance = this;
        // override TeleportPlugin instance
        try {
            Field instanceField = TeleportPlugin.class.getDeclaredField("instance");
            instanceField.setAccessible(true);
            instanceField.set(null, get().myCustomTeleportPlugin);
            this.getLogger().at(Level.INFO).log("TeleportPlugin instance successfully overridden.");
        } catch (NoSuchFieldException | IllegalAccessException e) {
            this.getLogger().at(Level.SEVERE).log("Failed to override TeleportPlugin instance: " + e);
            throw new RuntimeException(e);
        }

        getEventRegistry().registerGlobal(PlayerReadyEvent.class, MapEvents::onPlayerReady);
        this.getCodecRegistry(OpenCustomUIInteraction.PAGE_CODEC).register("Teleporter", TeleporterSettingsPageSupplier.class, TeleporterSettingsPageSupplier.CODEC);
        MapEvents.applyToExisting();
        // TeleportPlugin registers and calls loadWarps as well but not on the overridden instance
        this.getEventRegistry().registerGlobal(AllWorldsLoadedEvent.class, (event) -> TeleportPlugin.get().loadWarps());
    }

    @Override
    protected void shutdown() {
        MapEvents.reset();
    }

    @Nonnull
    public static HiddenTeleportersPlugin get() {
        return instance;
    }
}
