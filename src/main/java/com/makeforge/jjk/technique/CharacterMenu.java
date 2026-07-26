package com.makeforge.jjk.technique;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

/**
 * A clickable "choose your character" menu, rendered in chat (no GUI Screen).
 * Shown a moment after the player spawns in. Each entry runs /technique on click.
 *
 * Deliberately not a custom Screen: the 1.21.11 render pipeline rewrite makes a
 * bespoke Screen the highest-risk thing to add, and a chat menu can't crash the
 * client. A full GUI can come once the base build is confirmed green.
 */
public final class CharacterMenu {
    private CharacterMenu() {}

    public static void send(ServerPlayer player) {
        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("  §5§l✦ CURSED AWAKENING — CHOOSE YOUR TECHNIQUE ✦"));
        player.sendSystemMessage(Component.literal("  §7Click to bind. Moves: §fG H J K L §7· Awaken: §dR §7· Domain: §5V"));
        player.sendSystemMessage(Component.literal("  §8Survival: you lock in §7one §8technique · Creative: swap freely."));
        player.sendSystemMessage(Component.literal(""));

        for (Technique t : Technique.values()) {
            if (t == Technique.NONE) continue;
            player.sendSystemMessage(row(t));
        }
        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("  §8(Creative can swap anytime with §7/technique <name>§8)"));
    }

    private static MutableComponent row(Technique t) {
        ChatFormatting color = color(t);
        MutableComponent button = Component.literal("  ▶ " + t.display)
                .withStyle(style -> style
                        .withColor(color)
                        .withBold(true)
                        .withClickEvent(new ClickEvent.RunCommand("/technique " + t.name().toLowerCase()))
                        .withHoverEvent(new HoverEvent.ShowText(
                                Component.literal("§fPlay as " + t.display + "\n§7" + blurb(t)
                                        + "\n§8Click to select"))));
        return button.append(Component.literal("  §8— " + blurb(t)).withStyle(s -> s.withBold(false)));
    }

    private static ChatFormatting color(Technique t) {
        return switch (t) {
            case GOJO   -> ChatFormatting.AQUA;
            case SUKUNA -> ChatFormatting.DARK_RED;
            case YUJI   -> ChatFormatting.GOLD;
            case MEGUMI -> ChatFormatting.DARK_GRAY;
            case NOBARA -> ChatFormatting.YELLOW;
            case TOJI   -> ChatFormatting.WHITE;
            case GETO   -> ChatFormatting.DARK_GREEN;
            case NANAMI -> ChatFormatting.GOLD;
            case MAKI   -> ChatFormatting.GREEN;
            case MAI    -> ChatFormatting.LIGHT_PURPLE;
            case YUTA   -> ChatFormatting.AQUA;
            case CHOSO  -> ChatFormatting.DARK_RED;
            case MAHITO -> ChatFormatting.DARK_PURPLE;
            case TODO     -> ChatFormatting.RED;
            case HAKARI   -> ChatFormatting.GOLD;
            case KASHIMO  -> ChatFormatting.AQUA;
            case URAUME   -> ChatFormatting.BLUE;
            case HIGURUMA -> ChatFormatting.DARK_RED;
            case YUKI     -> ChatFormatting.LIGHT_PURPLE;
            case KENJAKU  -> ChatFormatting.DARK_GREEN;
            case INUMAKI  -> ChatFormatting.YELLOW;
            case JOGO     -> ChatFormatting.GOLD;
            case HANAMI   -> ChatFormatting.DARK_GREEN;
            case DAGON    -> ChatFormatting.DARK_AQUA;
            case YOROZU   -> ChatFormatting.LIGHT_PURPLE;
            default     -> ChatFormatting.GRAY;
        };
    }

    private static String blurb(Technique t) {
        return switch (t) {
            case GOJO   -> "Blue / Red / Hollow Purple";
            case SUKUNA -> "Dismantle / Cleave / Malevolent Shrine";
            case YUJI   -> "Divergent Fist / Black Flash — cheap, physical";
            case MEGUMI -> "Divine Dogs / Nue / Shadow Garden";
            case NOBARA -> "Hairpin / Resonance / Straw Doll";
            case TOJI   -> "Heavenly Restriction — zero CE, cursed tools";
            case GETO   -> "Curse spirits / Uzumaki — summoner";
            case NANAMI -> "Ratio technique — mark, then collapse";
            case MAKI   -> "Heavenly Restriction — pure weapon fighter";
            case MAI    -> "Construction — conjured shots, ranged";
            case YUTA   -> "Rika / Copy — huge reserves, bruiser";
            case CHOSO  -> "Blood manipulation — pierce and drain";
            case MAHITO -> "Idle Transfiguration — soul warping";
            case TODO     -> "Boogie Woogie — swap places, then punish";
            case HAKARI   -> "Jackpot — gamble for huge payoffs";
            case KASHIMO  -> "Lightning — chains between enemies";
            case URAUME   -> "Ice Formation — freeze and shatter";
            case HIGURUMA -> "Judgeman — executes wounded targets";
            case YUKI     -> "Star Rage — adds mass, huge knockback";
            case KENJAKU  -> "Stolen techniques — random every cast";
            case INUMAKI  -> "Cursed Speech — commands that obey";
            case JOGO     -> "Volcanic fire — burns everything";
            case HANAMI   -> "Wood — roots, drains, outlasts you";
            case DAGON    -> "Tides & shikigami — swarming pressure";
            case YOROZU   -> "Construction — forged blades and lances";
            default     -> "";
        };
    }
}
