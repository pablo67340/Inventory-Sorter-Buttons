package com.invsortbuttons.client;

import com.invsortbuttons.InvSortButtons;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = InvSortButtons.MOD_ID, value = Dist.CLIENT)
public final class ClientModEvents {
    public static final KeyMapping SORT_KEY = new KeyMapping(
            "key.invsortbuttons.sort", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R,
            "key.categories.invsortbuttons");

    private ClientModEvents() {
    }

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(SORT_KEY);
    }
}
