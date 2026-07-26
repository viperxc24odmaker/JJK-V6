package com.makeforge.jjk.client;

import com.makeforge.jjk.JJKMod;
import com.makeforge.jjk.net.AbilityPayload;
import com.makeforge.jjk.net.SyncPayload;
import com.makeforge.jjk.technique.Technique;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class JJKClient implements ClientModInitializer {

    // client-side mirror of the server state, fed by SyncPayload
    public static volatile Technique technique = Technique.NONE;
    public static volatile float ce = 0f;
    public static volatile float maxCe = 100f;
    // v2 awakening mirror
    public static volatile float awaken = 0f;      // 0..100
    public static volatile int   awakened = 0;     // ticks remaining
    public static volatile boolean ready = false;

    private static KeyMapping keyPrimary;
    private static KeyMapping keySecondary;
    private static KeyMapping keyUltimate;
    private static KeyMapping keyMove4;
    private static KeyMapping keyMove5;
    private static KeyMapping keyAwaken;
    private static KeyMapping keyDomain;

    @Override
    public void onInitializeClient() {
        KeyMapping.Category category =
                KeyMapping.Category.register(Identifier.fromNamespaceAndPath("jjk", "main"));

        keyPrimary   = reg("key.jjk.primary",   GLFW.GLFW_KEY_G, category);
        keySecondary = reg("key.jjk.secondary", GLFW.GLFW_KEY_H, category);
        keyUltimate  = reg("key.jjk.ultimate",  GLFW.GLFW_KEY_J, category);
        keyMove4     = reg("key.jjk.move4",      GLFW.GLFW_KEY_K, category);
        keyMove5     = reg("key.jjk.move5",      GLFW.GLFW_KEY_L, category);
        keyAwaken    = reg("key.jjk.awaken",     GLFW.GLFW_KEY_R, category);
        keyDomain    = reg("key.jjk.domain",     GLFW.GLFW_KEY_V, category);

        ClientPlayNetworking.registerGlobalReceiver(SyncPayload.TYPE, (payload, context) -> {
            technique = Technique.values()[Math.floorMod(payload.techniqueOrdinal(), Technique.values().length)];
            ce = payload.ce();
            maxCe = payload.maxCe();
            awaken = payload.awaken();
            awakened = payload.awakened();
            ready = payload.ready();
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            while (keyPrimary.consumeClick())   ClientPlayNetworking.send(new AbilityPayload(0));
            while (keySecondary.consumeClick()) ClientPlayNetworking.send(new AbilityPayload(1));
            while (keyUltimate.consumeClick())  ClientPlayNetworking.send(new AbilityPayload(2));
            while (keyMove4.consumeClick())     ClientPlayNetworking.send(new AbilityPayload(3));
            while (keyMove5.consumeClick())     ClientPlayNetworking.send(new AbilityPayload(4));
            while (keyAwaken.consumeClick())    ClientPlayNetworking.send(new AbilityPayload(JJKMod.AWAKEN_SLOT));
            while (keyDomain.consumeClick())    ClientPlayNetworking.send(
                    new AbilityPayload(com.makeforge.jjk.technique.TechniqueAbilities.DOMAIN_SLOT));
        });

        HudRenderCallback.EVENT.register(this::renderHud);
    }

    private static KeyMapping reg(String id, int code, KeyMapping.Category cat) {
        KeyMapping k = new KeyMapping(id, InputConstants.Type.KEYSYM, code, cat);
        KeyBindingHelper.registerKeyBinding(k);
        return k;
    }

    private void renderHud(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        if (technique == Technique.NONE) return;

        int x = 8;
        int y = g.guiHeight() - 46;
        int w = 120;
        int h = 8;

        // technique label
        g.drawString(mc.font, "§b" + technique.display, x, y - 11, 0xFFFFFF, true);

        // --- CE bar (blue) ---
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xAA101018);
        g.fill(x, y, x + w, y + h, 0xFF23252E);
        float pct = maxCe <= 0 ? 0f : Math.max(0f, Math.min(1f, ce / maxCe));
        int fill = (int) (w * pct);
        int color = technique == Technique.TOJI ? 0xFFE04545 : 0xFF3AA0FF;
        if (fill > 0) g.fill(x, y, x + fill, y + h, color);
        String txt = technique == Technique.TOJI
                ? "HEAVENLY RESTRICTION"
                : String.format("%.0f / %.0f", ce, maxCe);
        g.drawString(mc.font, txt, x + w + 6, y, 0xC8D0FF, true);

        // --- AWAKENING bar (purple -> gold), sits just under the CE bar ---
        int ay = y + h + 3;
        int ah = 6;
        g.fill(x - 1, ay - 1, x + w + 1, ay + ah + 1, 0xAA100818);
        g.fill(x, ay, x + w, ay + ah, 0xFF241A2E);

        boolean active = awakened > 0;
        float apct = active ? 1f : Math.max(0f, Math.min(1f, awaken / 100f));
        int afill = (int) (w * apct);
        // gold while active OR ready (with a slow pulse), purple while charging
        boolean pulse = (System.currentTimeMillis() / 300) % 2 == 0;
        int acolor;
        if (active)      acolor = 0xFFFFC24A;                 // solid gold
        else if (ready)  acolor = pulse ? 0xFFFFD860 : 0xFFB06BFF; // flashing
        else             acolor = 0xFF9B4DFF;                 // purple
        if (afill > 0) g.fill(x, ay, x + afill, ay + ah, acolor);

        String atxt;
        int atcol;
        if (active)      { atxt = "AWAKENED " + ((awakened + 19) / 20) + "s"; atcol = 0xFFE39A; }
        else if (ready)  { atxt = "AWAKENING READY §7[R]";                    atcol = 0xFFD860; }
        else             { atxt = String.format("Awaken %.0f%%", awaken);     atcol = 0xC9A8FF; }
        g.drawString(mc.font, atxt, x + w + 6, ay - 1, atcol, true);
    }
}
