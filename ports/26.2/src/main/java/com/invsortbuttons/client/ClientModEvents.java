package com.invsortbuttons.client;

import com.invsortbuttons.InvSortButtons;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

// RegisterKeyMappingsEvent is no longer a mod-bus event in Forge 26.2's
// EventBus 7; it fires on the default game bus
@Mod.EventBusSubscriber(modid = InvSortButtons.MOD_ID, value = Dist.CLIENT)
public final class ClientModEvents {
    public static final KeyMapping SORT_KEY = new KeyMapping(
            "key.invsortbuttons.sort", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R,
            KeyMapping.Category.INVENTORY);

    private ClientModEvents() {
    }

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(SORT_KEY);
    }
}
