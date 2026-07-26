package com.makeforge.jjk.technique;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Cursed tools for the Heavenly Restriction users (Toji & Maki), who have no
 * cursed energy and fight purely with weapons.
 *
 * DELIBERATE DESIGN CALL: these are *renamed vanilla items*, not registered
 * custom items. Custom items would need registry entries, model JSONs and
 * textures — three more things that can red the browser CI, and Divine would
 * have to draw art before it looked right. Renaming vanilla gear gets the
 * mechanic working today with zero asset work.
 *
 * Holding the right tool for your character grants a damage bonus that the
 * ability code reads via {@link #bonus(ServerPlayer)}.
 */
public final class CursedWeapons {
    private CursedWeapons() {}

    // display names double as the identity check (no custom components needed)
    public static final String INVERTED_SPEAR = "Inverted Spear of Heaven";
    public static final String PLAYFUL_CLOUD  = "Playful Cloud";
    public static final String SPLIT_SOUL     = "Split Soul Katana";
    public static final String DRAGON_BONE    = "Dragon-Bone Blade";
    public static final String EXECUTIONER    = "Executioner's Blade";
    public static final String SLAUGHTER      = "Slaughter Demon";
    public static final String GUN            = "Gun";

    /** Everything the /cursedweapon command can hand out. */
    public static String[] all() {
        return new String[]{ INVERTED_SPEAR, PLAYFUL_CLOUD, SPLIT_SOUL, DRAGON_BONE,
                             EXECUTIONER, SLAUGHTER, GUN };
    }

    private static Item base(String name) {
        return switch (name) {
            case INVERTED_SPEAR -> Items.NETHERITE_SWORD;
            case PLAYFUL_CLOUD  -> Items.NETHERITE_AXE;   // blunt, heavy
            case SPLIT_SOUL     -> Items.IRON_SWORD;      // katana silhouette
            case DRAGON_BONE    -> Items.TRIDENT;         // polearm
            case EXECUTIONER    -> Items.NETHERITE_AXE;   // headsman's blade
            case SLAUGHTER      -> Items.DIAMOND_SWORD;   // cursed tool
            case GUN            -> Items.MUSIC_DISC_11;   // Mai's construction
            default             -> Items.STICK;
        };
    }

    /** Flavour text shown when you receive it. */
    public static String blurb(String name) {
        return switch (name) {
            case INVERTED_SPEAR -> "§7Cancels cursed techniques on contact. §8(Toji)";
            case PLAYFUL_CLOUD  -> "§7A three-section staff. Blunt, brutal. §8(Toji / Maki)";
            case SPLIT_SOUL     -> "§7Cuts the soul as well as the body. §8(Maki)";
            case DRAGON_BONE    -> "§7Forged from a cursed dragon's remains. §8(Maki)";
            case EXECUTIONER    -> "§7Passes sentence on the guilty. §8(Higuruma)";
            case SLAUGHTER      -> "§7A cursed tool that drinks the fight. §8(Maki)";
            case GUN            -> "§7Constructed round by round. §8(Mai)";
            default             -> "";
        };
    }

    /** Build the stack. Only the CUSTOM_NAME component is touched — kept minimal on purpose. */
    public static ItemStack make(String name) {
        ItemStack stack = new ItemStack(base(name));
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("§d" + name));
        return stack;
    }

    public static void give(ServerPlayer p, String name) {
        p.addItem(make(name));
        p.displayClientMessage(Component.literal("§dReceived: §f" + name), false);
        p.displayClientMessage(Component.literal(blurb(name)), false);
    }

    /** Name of the cursed weapon currently held in the main hand, or null. */
    public static String held(ServerPlayer p) {
        ItemStack stack = p.getMainHandItem();
        if (stack.isEmpty()) return null;
        Component custom = stack.get(DataComponents.CUSTOM_NAME);
        if (custom == null) return null;
        String plain = custom.getString().replace("§d", "").trim();
        for (String n : all()) {
            if (n.equals(plain)) return n;
        }
        return null;
    }

    /**
     * Bonus physical damage for holding a cursed weapon.
     * Toji and Maki get the full benefit; anyone else gets a smaller cut,
     * since the tools aren't tuned to their body.
     */
    public static float bonus(ServerPlayer p) {
        String w = held(p);
        if (w == null) return 0f;
        float base = switch (w) {
            case INVERTED_SPEAR -> 6f;
            case PLAYFUL_CLOUD  -> 5f;
            case SPLIT_SOUL     -> 7f;
            case DRAGON_BONE    -> 5f;
            case EXECUTIONER    -> 8f;
            case SLAUGHTER      -> 6f;
            case GUN            -> 6f;
            default             -> 0f;
        };
        return base;
    }

    /** The tool a given character is meant to carry, or null. */
    public static String signatureFor(com.makeforge.jjk.technique.Technique t) {
        return switch (t) {
            case TOJI     -> INVERTED_SPEAR;
            case MAKI     -> SPLIT_SOUL;
            case HIGURUMA -> EXECUTIONER;
            case MAI      -> GUN;
            default       -> null;
        };
    }

    /** Give a character their signature tool, if they have one. */
    public static void giveSignature(ServerPlayer p, com.makeforge.jjk.technique.Technique t) {
        String w = signatureFor(t);
        if (w != null) give(p, w);
    }
}
