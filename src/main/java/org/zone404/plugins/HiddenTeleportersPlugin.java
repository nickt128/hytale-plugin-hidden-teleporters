package org.zone404.plugins;

import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.OpenCustomUIInteraction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import org.zone404.plugins.pages.TeleporterSettingsPageSupplier;

import javax.annotation.Nonnull;

@SuppressWarnings("unused")
public class HiddenTeleportersPlugin extends JavaPlugin {
    private static HiddenTeleportersPlugin instance;

    public HiddenTeleportersPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        instance = this;
        this.getCodecRegistry(OpenCustomUIInteraction.PAGE_CODEC).register("Teleporter", TeleporterSettingsPageSupplier.class, TeleporterSettingsPageSupplier.CODEC);
    }

    @Override
    protected void shutdown() {}

    @Nonnull
    public static HiddenTeleportersPlugin get() {
        return instance;
    }
}
