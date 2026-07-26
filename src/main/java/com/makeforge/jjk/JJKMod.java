package com.makeforge.jjk;

import com.makeforge.jjk.ce.CursedEnergy;
import com.makeforge.jjk.net.AbilityPayload;
import com.makeforge.jjk.net.SyncPayload;
import com.makeforge.jjk.technique.Technique;
import com.makeforge.jjk.technique.TechniqueAbilities;
import com.makeforge.jjk.technique.CharacterMenu;
import com.makeforge.jjk.technique.CursedWeapons;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class JJKMod implements ModInitializer {

    /** slot value that means "trigger awakening" instead of an ability. */
    public static final int AWAKEN_SLOT = 100;

    // --- tiny server-thread scheduler for delayed / repeated effects ---
    private record Task(Runnable run, int[] delay) {}
    private static final List<Task> TASKS = new ArrayList<>();

    public static void schedule(int delayTicks, Runnable run) {
        synchronized (TASKS) { TASKS.add(new Task(run, new int[]{ delayTicks })); }
    }

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playC2S().register(AbilityPayload.TYPE, AbilityPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SyncPayload.TYPE, SyncPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(AbilityPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            int slot = payload.slot();
            ((ServerLevel) player.level()).getServer().execute(() -> {
                if (slot == AWAKEN_SLOT) TechniqueAbilities.tryAwaken(player);
                else                     TechniqueAbilities.execute(player, slot);
                // (DOMAIN_SLOT is handled inside execute)
            });
        });

        // CE regen + awakening bar + scheduled tasks each server tick
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            long tick = server.getTickCount();
            boolean sync = tick % 5 == 0;
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                CursedEnergy.Data d = CursedEnergy.server(p.getUUID());
                CursedEnergy.tick(d);

                // --- awakening bar / active state ---
                if (d.awakened > 0) {
                    d.awakened--;
                    TechniqueAbilities.awakenTick(p, d, tick);
                } else if (!d.awakenReady && d.technique != Technique.NONE
                        && !d.technique.isRestriction()) {
                    // passive trickle: +0.5% every 50 ticks.
                    // Toji & Maki have no cursed energy — they charge by fighting instead.
                    if (tick % CursedEnergy.AWAKEN_TICKRATE == 0) {
                        CursedEnergy.addAwaken(d, CursedEnergy.AWAKEN_FILL);
                    }
                }

                if (sync) {
                    ServerPlayNetworking.send(p, new SyncPayload(
                            d.technique.ordinal(), d.ce, d.technique.maxCE,
                            d.awaken, d.awakened, d.awakenReady));
                }
            }
            synchronized (TASKS) {
                Iterator<Task> it = TASKS.iterator();
                while (it.hasNext()) {
                    Task t = it.next();
                    if (t.delay()[0] <= 0) {
                        try { t.run().run(); } catch (Exception ignored) {}
                        it.remove();
                    } else {
                        t.delay()[0]--;
                    }
                }
            }
        });

        // The CE maps are static, so without these a technique picked in one world
        // would still be bound after quitting to menu and loading a different world.
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                CursedEnergy.remove(handler.player.getUUID()));
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED
                .register(server -> CursedEnergy.clearServer());

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.player;
            schedule(20, () -> CharacterMenu.send(player));
        });

        registerCommands();
    }

    private void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, env) -> {
            dispatcher.register(Commands.literal("technique")
                    .executes(ctx -> {
                        ServerPlayer p = ctx.getSource().getPlayerOrException();
                        CharacterMenu.send(p);
                        return 1;
                    })
                    .then(Commands.argument("name", StringArgumentType.word())
                            .executes(ctx -> {
                                ServerPlayer p = ctx.getSource().getPlayerOrException();
                                String arg = StringArgumentType.getString(ctx, "name");
                                Technique t = Technique.byName(arg);
                                if (t == null) {
                                    p.displayClientMessage(Component.literal(
                                            "§cUnknown. Try: gojo, sukuna, yuji, megumi, nobara, toji"), false);
                                    return 0;
                                }
                                CursedEnergy.Data d = CursedEnergy.server(p.getUUID());
                                // Survival: you lock in ONE technique. Creative: swap freely.
                                if (!p.isCreative() && d.technique != Technique.NONE) {
                                    p.displayClientMessage(Component.literal(
                                            "§cYou've already chosen §f" + d.technique.display
                                            + "§c. Switching is Creative-only."), false);
                                    return 0;
                                }
                                CursedEnergy.setTechnique(d, t);
                                p.displayClientMessage(Component.literal(
                                        "§bTechnique set: §f" + t.display), false);
                                CursedWeapons.giveSignature(p, t);
                                if (TechniqueAbilities.hasDomain(t)) {
                                    p.displayClientMessage(Component.literal(
                                            "§5Domain Expansion available: §f"
                                            + TechniqueAbilities.domainName(t) + " §8[V]"), false);
                                }
                                return 1;
                            })));

            dispatcher.register(Commands.literal("ce").executes(ctx -> {
                ServerPlayer p = ctx.getSource().getPlayerOrException();
                CursedEnergy.Data d = CursedEnergy.server(p.getUUID());
                p.displayClientMessage(Component.literal(String.format(
                        "§9CE: §f%.0f/%.0f  §7(%s)  §5Awaken: §f%.0f%%%s",
                        d.ce, d.technique.maxCE, d.technique.display,
                        d.awaken, d.awakenReady ? " §d[READY]" : "")), false);
                return 1;
            }));

            // /cursedweapon            -> list what's available
            // /cursedweapon <name>     -> receive it (Creative-only, like Arcane's wand)
            dispatcher.register(Commands.literal("cursedweapon")
                    .executes(ctx -> {
                        ServerPlayer p = ctx.getSource().getPlayerOrException();
                        p.displayClientMessage(Component.literal("§d§lCursed Tools:"), false);
                        for (String n : CursedWeapons.all()) {
                            p.displayClientMessage(Component.literal(
                                    "  §f" + n + "  " + CursedWeapons.blurb(n)), false);
                        }
                        p.displayClientMessage(Component.literal(
                                "§8Use §7/cursedweapon <name> §8(Creative only)"), false);
                        return 1;
                    })
                    .then(Commands.argument("name", StringArgumentType.greedyString())
                            .executes(ctx -> {
                                ServerPlayer p = ctx.getSource().getPlayerOrException();
                                if (!p.isCreative()) {
                                    p.displayClientMessage(Component.literal(
                                            "§cCursed tools are Creative-only."), false);
                                    return 0;
                                }
                                String arg = StringArgumentType.getString(ctx, "name");
                                for (String n : CursedWeapons.all()) {
                                    if (n.equalsIgnoreCase(arg)) {
                                        CursedWeapons.give(p, n);
                                        return 1;
                                    }
                                }
                                p.displayClientMessage(Component.literal(
                                        "§cUnknown tool. Run §f/cursedweapon §cto list them."), false);
                                return 0;
                            })));
            // /jjk -> quick reference for everything the mod does
            dispatcher.register(Commands.literal("jjk").executes(ctx -> {
                ServerPlayer p = ctx.getSource().getPlayerOrException();
                p.displayClientMessage(Component.literal("§5§l✦ JJK: CURSED AWAKENING ✦"), false);
                p.displayClientMessage(Component.literal(
                        "§7Abilities: §fG §7· §fH §7· §fJ §7· §fK §7· §fL"), false);
                p.displayClientMessage(Component.literal(
                        "§7Awaken: §d§lR §8(when the second bar reads READY)"), false);
                p.displayClientMessage(Component.literal(
                        "§7Domain Expansion: §5§lV §8(costs 100 CE, 10 characters have one)"), false);
                p.displayClientMessage(Component.literal(""), false);
                p.displayClientMessage(Component.literal("§8Commands:"), false);
                p.displayClientMessage(Component.literal(
                        "  §f/technique §8— open the character picker"), false);
                p.displayClientMessage(Component.literal(
                        "  §f/ce §8— your cursed energy + awakening %"), false);
                p.displayClientMessage(Component.literal(
                        "  §f/cursedweapon §8— list the cursed tools"), false);
                p.displayClientMessage(Component.literal(
                        "  §f/jjk §8— this help"), false);
                p.displayClientMessage(Component.literal(""), false);
                p.displayClientMessage(Component.literal(
                        "§8Survival locks one technique · Creative swaps freely."), false);
                return 1;
            }));
        });
    }
}
