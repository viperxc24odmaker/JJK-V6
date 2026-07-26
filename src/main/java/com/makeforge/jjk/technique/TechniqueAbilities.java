package com.makeforge.jjk.technique;

import com.makeforge.jjk.JJKMod;
import com.makeforge.jjk.ce.CursedEnergy;
import com.makeforge.jjk.util.CombatUtil;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Server-side execution of every technique.
 * slot 0=G, 1=H, 2=J, 3=K, 4=L, 5 = AWAKEN trigger (R).
 *
 * v3: 13 characters, heavier VFX/SFX, longer awakenings, cursed weapons.
 */
public final class TechniqueAbilities {
    private TechniqueAbilities() {}

    // --- effect holders (rename here if CI complains) ---
    private static final Holder<MobEffect> SPEED      = MobEffects.SPEED;
    private static final Holder<MobEffect> STRENGTH   = MobEffects.STRENGTH;
    private static final Holder<MobEffect> RESISTANCE = MobEffects.RESISTANCE;
    private static final Holder<MobEffect> HASTE      = MobEffects.HASTE;
    private static final Holder<MobEffect> LEVITATION = MobEffects.LEVITATION;
    private static final Holder<MobEffect> DARKNESS   = MobEffects.DARKNESS;
    private static final Holder<MobEffect> WEAKNESS   = MobEffects.WEAKNESS;
    private static final Holder<MobEffect> GLOWING    = MobEffects.GLOWING;
    private static final Holder<MobEffect> SLOWNESS   = MobEffects.SLOWNESS;
    private static final Holder<MobEffect> REGEN      = MobEffects.REGENERATION;
    private static final Holder<MobEffect> ABSORPTION = MobEffects.ABSORPTION;
    private static final Holder<MobEffect> JUMP       = MobEffects.JUMP_BOOST;
    private static final Holder<MobEffect> FIRE_RES   = MobEffects.FIRE_RESISTANCE;
    private static final Holder<MobEffect> POISON     = MobEffects.POISON;
    private static final Holder<MobEffect> WITHER     = MobEffects.WITHER;
    private static final Holder<MobEffect> BLINDNESS  = MobEffects.BLINDNESS;
    private static final Holder<MobEffect> NAUSEA     = MobEffects.NAUSEA;

    // --- sounds (all long-standing vanilla ids) ---
    private static final SoundEvent S_BOOM    = SoundEvents.WARDEN_SONIC_BOOM;
    private static final SoundEvent S_EXPLODE = SoundEvents.GENERIC_EXPLODE.value();
    private static final SoundEvent S_CAST    = SoundEvents.EVOKER_CAST_SPELL;
    private static final SoundEvent S_ROAR    = SoundEvents.ENDER_DRAGON_GROWL;
    private static final SoundEvent S_CRIT    = SoundEvents.PLAYER_ATTACK_CRIT;
    private static final SoundEvent S_SWEEP   = SoundEvents.PLAYER_ATTACK_SWEEP;
    private static final SoundEvent S_ANVIL   = SoundEvents.ANVIL_LAND;
    private static final SoundEvent S_TOTEM   = SoundEvents.TOTEM_USE;
    private static final SoundEvent S_SHOOT   = SoundEvents.BLAZE_SHOOT;
    private static final SoundEvent S_WITHER  = SoundEvents.WITHER_SHOOT;
    private static final SoundEvent S_THUNDER = SoundEvents.LIGHTNING_BOLT_THUNDER;
    private static final SoundEvent S_PORTAL  = SoundEvents.PORTAL_TRIGGER;

    /** slot 6 = Domain Expansion (key V). */
    public static final int DOMAIN_SLOT = 6;

    /** Per-player, per-slot cooldown gate: uuid -> next allowed game tick. */
    private static final java.util.Map<java.util.UUID, long[]> COOLDOWNS =
            new java.util.concurrent.ConcurrentHashMap<>();
    private static final int  SLOT_COOLDOWN   = 8;    // ticks between casts of one slot
    private static final int  DOMAIN_COOLDOWN = 400;  // domains are a big deal
    /** Awakening earned per successful cast. */
    private static final float AWAKEN_PER_CAST = 3.0f;

    /** Set false by pay() when a cast can't be afforded, so it earns nothing. */
    private static boolean castPaid = true;

    public static void execute(ServerPlayer p, int slot) {
        ServerLevel level = (ServerLevel) p.level();
        CursedEnergy.Data d = CursedEnergy.server(p.getUUID());

        if (slot == 5) { tryAwaken(p, level, d); return; }

        // --- cooldown gate ---
        // Without this, holding a key down re-fires every tick and quietly
        // charges the awakening bar for free. One gate fixes both problems.
        long now = level.getGameTime();
        int idx = Math.max(0, Math.min(slot, 6));
        long[] cds = COOLDOWNS.computeIfAbsent(p.getUUID(), k -> new long[7]);
        int wait = (slot == DOMAIN_SLOT) ? DOMAIN_COOLDOWN : SLOT_COOLDOWN;
        if (now < cds[idx]) return;               // silently ignore -- no spam, no gain
        cds[idx] = now + wait;

        castPaid = true;

        if (slot == DOMAIN_SLOT) {
            domain(p, level, d);
            if (castPaid) CursedEnergy.addAwaken(d, AWAKEN_PER_CAST);
            return;
        }

        switch (d.technique) {
            case GOJO   -> gojo(p, level, d, slot);
            case SUKUNA -> sukuna(p, level, d, slot);
            case YUJI   -> yuji(p, level, d, slot);
            case MEGUMI -> megumi(p, level, d, slot);
            case NOBARA -> nobara(p, level, d, slot);
            case TOJI   -> toji(p, level, d, slot);
            case GETO   -> geto(p, level, d, slot);
            case NANAMI -> nanami(p, level, d, slot);
            case MAKI   -> maki(p, level, d, slot);
            case MAI    -> mai(p, level, d, slot);
            case YUTA   -> yuta(p, level, d, slot);
            case CHOSO  -> choso(p, level, d, slot);
            case MAHITO -> mahito(p, level, d, slot);
            case TODO     -> todo(p, level, d, slot);
            case HAKARI   -> hakari(p, level, d, slot);
            case KASHIMO  -> kashimo(p, level, d, slot);
            case URAUME   -> uraume(p, level, d, slot);
            case HIGURUMA -> higuruma(p, level, d, slot);
            case YUKI     -> yuki(p, level, d, slot);
            case KENJAKU  -> kenjaku(p, level, d, slot);
            case INUMAKI  -> inumaki(p, level, d, slot);
            case JOGO     -> jogo(p, level, d, slot);
            case HANAMI   -> hanami(p, level, d, slot);
            case DAGON    -> dagon(p, level, d, slot);
            case YOROZU   -> yorozu(p, level, d, slot);
            default     -> msg(p, "§7No technique. Use §f/technique <name>");
        }

        // Successful casts feed the awakening bar. Gated on castPaid so a cast you
        // couldn't afford earns nothing, and gated on cooldown above so holding
        // the key down can't farm it.
        if (castPaid) CursedEnergy.addAwaken(d, AWAKEN_PER_CAST);
    }

    /* ======================= AWAKENING ======================= */

    /**
     * Convenience overload: works out the level and data itself.
     * Kept public so JJKMod can trigger an awakening with just the player.
     */
    public static void tryAwaken(ServerPlayer p) {
        tryAwaken(p, (ServerLevel) p.level(), CursedEnergy.server(p.getUUID()));
    }

    public static void tryAwaken(ServerPlayer p, ServerLevel level, CursedEnergy.Data d) {
        if (d.technique == Technique.NONE) { msg(p, "§7Pick a technique first."); return; }
        if (d.awakened > 0) { msg(p, "§dAlready awakened."); return; }
        if (!d.awakenReady) {
            msg(p, String.format("§8Awakening charging… §7%.0f%%", d.awaken));
            return;
        }
        d.awakenReady = false;
        d.awaken = 0f;
        d.awakened = CursedEnergy.AWAKEN_DURATION;

        // big entrance: spiral column, ground ring, shockwave, roar
        Vec3 at = p.position();
        CombatUtil.spiral(level, ParticleTypes.SOUL_FIRE_FLAME, at, 1.2, 3.0, 60);
        CombatUtil.ring(level, ParticleTypes.END_ROD, at, 3.0, 48);
        CombatUtil.ring(level, ParticleTypes.WITCH, at, 4.5, 56);
        CombatUtil.burst(level, ParticleTypes.EXPLOSION, at, 6, 1.0);
        CombatUtil.sound(level, at, S_ROAR, 1.0f, 0.7f);
        CombatUtil.sound(level, at, S_TOTEM, 1.0f, 0.8f);

        onAwaken(p, level, d);
    }

    /** Fired once when awakening starts. */
    private static void onAwaken(ServerPlayer p, ServerLevel level, CursedEnergy.Data d) {
        int T = CursedEnergy.AWAKEN_DURATION;
        Vec3 at = p.position();
        switch (d.technique) {
            case GOJO -> {
                domainText(p, "§5§lLIMITLESS — INFINITY");
                CombatUtil.effect(p, RESISTANCE, T, 4);
                CombatUtil.effect(p, SPEED, T, 2);
                CombatUtil.sound(level, at, S_BOOM, 1.0f, 1.2f);
            }
            case SUKUNA -> {
                domainText(p, "§4§lKING OF CURSES");
                CombatUtil.effect(p, STRENGTH, T, 2);
                CombatUtil.effect(p, RESISTANCE, T, 1);
                CombatUtil.sound(level, at, S_ROAR, 1.0f, 0.5f);
            }
            case YUJI -> {
                domainText(p, "§c§lVESSEL — FULL POWER");
                CombatUtil.effect(p, STRENGTH, T, 2);
                CombatUtil.effect(p, SPEED, T, 2);
                CombatUtil.effect(p, REGEN, T, 1);
            }
            case MEGUMI -> {
                domainText(p, "§8§lMAHORAGA");
                CombatUtil.effect(p, RESISTANCE, T, 2);
                CombatUtil.effect(p, STRENGTH, T, 1);
                CombatUtil.summonWolf(level, p, at.add(p.getViewVector(1f).scale(2)), null);
                CombatUtil.sound(level, at, S_ROAR, 1.0f, 0.6f);
            }
            case NOBARA -> {
                domainText(p, "§e§lRESONANCE OVERLOAD");
                CombatUtil.effect(p, HASTE, T, 2);
                CombatUtil.effect(p, SPEED, T, 1);
            }
            case TOJI -> {
                domainText(p, "§f§lHEAVENLY RESTRICTION: ZERO");
                CombatUtil.effect(p, SPEED, T, 3);
                CombatUtil.effect(p, STRENGTH, T, 2);
                CombatUtil.effect(p, RESISTANCE, T, 2);
                CombatUtil.effect(p, HASTE, T, 3);
                CombatUtil.effect(p, JUMP, T, 2);
            }
            case GETO -> {
                domainText(p, "§2§lUZUMAKI — CURSE SWARM");
                CombatUtil.effect(p, RESISTANCE, T, 1);
                for (int i = 0; i < 4; i++) {
                    double a = (Math.PI * 2 / 4) * i;
                    CombatUtil.summonWolf(level, p, at.add(Math.cos(a) * 2, 0, Math.sin(a) * 2), null);
                }
                CombatUtil.sound(level, at, S_CAST, 1.0f, 0.6f);
            }
            case NANAMI -> {
                domainText(p, "§6§lOVERTIME");
                CombatUtil.effect(p, STRENGTH, T, 2);
                CombatUtil.effect(p, HASTE, T, 2);
                CombatUtil.sound(level, at, S_ANVIL, 1.0f, 1.4f);
            }
            case MAKI -> {
                domainText(p, "§a§lAWAKENED — ZENIN MAKI");
                CombatUtil.effect(p, STRENGTH, T, 3);
                CombatUtil.effect(p, SPEED, T, 3);
                CombatUtil.effect(p, RESISTANCE, T, 2);
                CombatUtil.effect(p, JUMP, T, 2);
                CombatUtil.sound(level, at, S_SWEEP, 1.0f, 0.8f);
            }
            case MAI -> {
                domainText(p, "§d§lCONSTRUCTION — FULL ROUND");
                CombatUtil.effect(p, HASTE, T, 3);
                CombatUtil.effect(p, SPEED, T, 1);
                CombatUtil.sound(level, at, S_SHOOT, 1.0f, 0.8f);
            }
            case YUTA -> {
                domainText(p, "§b§lRIKA — MANIFEST");
                CombatUtil.effect(p, STRENGTH, T, 2);
                CombatUtil.effect(p, RESISTANCE, T, 3);
                CombatUtil.effect(p, REGEN, T, 1);
                CombatUtil.effect(p, ABSORPTION, T, 2);
                CombatUtil.sound(level, at, S_ROAR, 1.0f, 1.4f);
            }
            case CHOSO -> {
                domainText(p, "§4§lFLOWING RED SCALE");
                CombatUtil.effect(p, STRENGTH, T, 2);
                CombatUtil.effect(p, REGEN, T, 2);
                CombatUtil.effect(p, FIRE_RES, T, 0);
            }
            case MAHITO -> {
                domainText(p, "§5§lIDLE TRANSFIGURATION — TRUE FORM");
                CombatUtil.effect(p, RESISTANCE, T, 2);
                CombatUtil.effect(p, SPEED, T, 2);
                CombatUtil.effect(p, REGEN, T, 1);
                CombatUtil.sound(level, at, S_WITHER, 1.0f, 0.6f);
            }
            case TODO -> {
                domainText(p, "§c§lBOOGIE WOOGIE — UNLIMITED");
                CombatUtil.effect(p, STRENGTH, T, 2);
                CombatUtil.effect(p, SPEED, T, 3);
                CombatUtil.sound(level, at, S_TOTEM, 1.0f, 1.1f);
            }
            case HAKARI -> {
                domainText(p, "§6§lJACKPOT!!");
                CombatUtil.effect(p, REGEN, T, 3);
                CombatUtil.effect(p, RESISTANCE, T, 3);
                CombatUtil.effect(p, STRENGTH, T, 2);
                CombatUtil.effect(p, ABSORPTION, T, 3);
                CombatUtil.sound(level, at, S_TOTEM, 1.0f, 1.6f);
            }
            case KASHIMO -> {
                domainText(p, "§b§lMYTHICAL BEAST AMBER");
                CombatUtil.effect(p, SPEED, T, 3);
                CombatUtil.effect(p, STRENGTH, T, 2);
                CombatUtil.sound(level, at, S_THUNDER, 1.0f, 1.4f);
            }
            case URAUME -> {
                domainText(p, "§b§lICE FORMATION — FROST CALM");
                CombatUtil.effect(p, RESISTANCE, T, 2);
                CombatUtil.effect(p, HASTE, T, 2);
                CombatUtil.sound(level, at, S_CAST, 1.0f, 1.5f);
            }
            case HIGURUMA -> {
                domainText(p, "§4§lDEATH SENTENCE");
                CombatUtil.effect(p, STRENGTH, T, 3);
                CombatUtil.effect(p, HASTE, T, 2);
                CombatUtil.sound(level, at, S_ANVIL, 1.0f, 0.6f);
            }
            case YUKI -> {
                domainText(p, "§d§lSTAR RAGE — BOMB CORE");
                CombatUtil.effect(p, STRENGTH, T, 3);
                CombatUtil.effect(p, RESISTANCE, T, 2);
                CombatUtil.effect(p, JUMP, T, 2);
                CombatUtil.sound(level, at, S_EXPLODE, 1.0f, 0.7f);
            }
            case KENJAKU -> {
                domainText(p, "§2§lA THOUSAND TECHNIQUES");
                CombatUtil.effect(p, RESISTANCE, T, 2);
                CombatUtil.effect(p, SPEED, T, 2);
                CombatUtil.effect(p, REGEN, T, 1);
                CombatUtil.sound(level, at, S_ROAR, 1.0f, 1.1f);
            }
            case INUMAKI -> {
                domainText(p, "§e§lCURSED SPEECH — UNBOUND");
                CombatUtil.effect(p, HASTE, T, 2);
                CombatUtil.effect(p, SPEED, T, 2);
                CombatUtil.sound(level, at, S_BOOM, 1.0f, 1.5f);
            }
            case JOGO -> {
                domainText(p, "§6§lCOFFIN OF THE IRON MOUNTAIN");
                CombatUtil.effect(p, FIRE_RES, T, 0);
                CombatUtil.effect(p, STRENGTH, T, 2);
                CombatUtil.sound(level, at, S_ROAR, 1.0f, 0.9f);
            }
            case HANAMI -> {
                domainText(p, "§2§lWOODEN CORE");
                CombatUtil.effect(p, RESISTANCE, T, 3);
                CombatUtil.effect(p, REGEN, T, 2);
                CombatUtil.sound(level, at, S_CAST, 1.0f, 0.5f);
            }
            case DAGON -> {
                domainText(p, "§3§lHORIZON OF THE CAPTIVATING SKANDHA");
                CombatUtil.effect(p, RESISTANCE, T, 2);
                CombatUtil.effect(p, SPEED, T, 2);
                CombatUtil.effect(p, REGEN, T, 1);
                CombatUtil.sound(level, at, S_PORTAL, 1.0f, 0.6f);
            }
            case YOROZU -> {
                domainText(p, "§d§lPERFECT CONSTRUCTION");
                CombatUtil.effect(p, STRENGTH, T, 2);
                CombatUtil.effect(p, HASTE, T, 3);
                CombatUtil.effect(p, RESISTANCE, T, 1);
                CombatUtil.sound(level, at, S_ANVIL, 1.0f, 0.7f);
            }
            default -> {}
        }
    }

    /** Fired every tick while awakened (d.awakened counts down). */
    public static void awakenTick(ServerPlayer p, CursedEnergy.Data d, long serverTick) {
        ServerLevel level = (ServerLevel) p.level();
        Vec3 at = p.position();
        int t = d.awakened;

        // shared aura so every awakening reads as "powered up"
        if (t % 4 == 0) CombatUtil.ring(level, ParticleTypes.SOUL_FIRE_FLAME, at, 1.1, 8);

        switch (d.technique) {
            case GOJO -> {
                if (t % 6 == 0) CombatUtil.burst(level, ParticleTypes.PORTAL, at, 8, 0.6);
            }
            case SUKUNA -> {
                // auto-Dismantle: slash pulses around the King
                if (t % 10 == 0) {
                    slashParticles(level, p, 8);
                    for (LivingEntity e : CombatUtil.around(level, p, at, 5)) {
                        CombatUtil.magicDamage(level, p, e, 5f);
                    }
                }
            }
            case YUJI -> {
                if (t % 8 == 0) CombatUtil.burst(level, ParticleTypes.CRIT, at, 6, 0.5);
            }
            case MEGUMI -> {
                if (t % 20 == 0) CombatUtil.burst(level, ParticleTypes.SMOKE, at, 8, 0.5);
            }
            case NOBARA -> {
                // resonance overload: auto AoE pulse in front
                if (t % 8 == 0) {
                    Vec3 aim = p.getEyePosition().add(p.getViewVector(1f).scale(4));
                    CombatUtil.burst(level, ParticleTypes.CRIT, aim, 6, 0.5);
                    for (LivingEntity e : CombatUtil.around(level, p, aim, 4)) {
                        CombatUtil.magicDamage(level, p, e, 4f);
                    }
                }
            }
            case GETO -> {
                if (t % 30 == 0) CombatUtil.ring(level, ParticleTypes.SCULK_SOUL, at, 2.0, 16);
            }
            case NANAMI -> {
                if (t % 12 == 0) CombatUtil.burst(level, ParticleTypes.ENCHANTED_HIT, at, 5, 0.5);
            }
            case MAKI -> {
                if (t % 10 == 0) CombatUtil.ring(level, ParticleTypes.SWEEP_ATTACK, at, 1.6, 6);
            }
            case MAI -> {
                if (t % 14 == 0) CombatUtil.burst(level, ParticleTypes.END_ROD, at, 5, 0.5);
            }
            case YUTA -> {
                // Rika lashes out at whatever is close
                if (t % 15 == 0) {
                    CombatUtil.ring(level, ParticleTypes.SOUL, at, 2.4, 20);
                    for (LivingEntity e : CombatUtil.around(level, p, at, 6)) {
                        CombatUtil.magicDamage(level, p, e, 6f);
                    }
                }
            }
            case CHOSO -> {
                if (t % 10 == 0) CombatUtil.burst(level, ParticleTypes.FALLING_LAVA, at, 4, 0.5);
            }
            case MAHITO -> {
                // souls warp around him and rot anything near
                if (t % 20 == 0) {
                    CombatUtil.ring(level, ParticleTypes.SOUL, at, 3.0, 24);
                    for (LivingEntity e : CombatUtil.around(level, p, at, 5)) {
                        CombatUtil.effect(e, WITHER, 40, 0);
                    }
                }
            }
            case TODO -> {
                if (t % 10 == 0) CombatUtil.burst(level, ParticleTypes.NOTE, at, 6, 0.6);
            }
            case HAKARI -> {
                // the jackpot keeps paying out
                if (t % 20 == 0) {
                    CombatUtil.ring(level, ParticleTypes.HAPPY_VILLAGER, at, 2.0, 18);
                    CombatUtil.effect(p, REGEN, 60, 2);
                }
            }
            case KASHIMO -> {
                // arcing current damages anything close
                if (t % 15 == 0) {
                    CombatUtil.ring(level, ParticleTypes.ELECTRIC_SPARK, at, 3.0, 24);
                    for (LivingEntity e : CombatUtil.around(level, p, at, 4)) {
                        CombatUtil.magicDamage(level, p, e, 5f);
                    }
                }
            }
            case URAUME -> {
                // frost aura slows and freezes
                if (t % 20 == 0) {
                    CombatUtil.ring(level, ParticleTypes.SNOWFLAKE, at, 3.5, 26);
                    for (LivingEntity e : CombatUtil.around(level, p, at, 5)) {
                        CombatUtil.effect(e, SLOWNESS, 60, 1);
                        e.setTicksFrozen(140);
                    }
                }
            }
            case HIGURUMA -> {
                if (t % 12 == 0) CombatUtil.burst(level, ParticleTypes.SOUL_FIRE_FLAME, at, 6, 0.5);
            }
            case YUKI -> {
                if (t % 12 == 0) CombatUtil.burst(level, ParticleTypes.END_ROD, at, 8, 0.7);
            }
            case KENJAKU -> {
                if (t % 25 == 0) CombatUtil.ring(level, ParticleTypes.SCULK_SOUL, at, 2.5, 20);
            }
            case INUMAKI -> {
                if (t % 18 == 0) {
                    CombatUtil.ring(level, ParticleTypes.NOTE, at, 2.2, 16);
                    for (LivingEntity e : CombatUtil.around(level, p, at, 5)) {
                        CombatUtil.effect(e, SLOWNESS, 40, 0);
                    }
                }
            }
            case JOGO -> {
                // the ground around him burns
                if (t % 12 == 0) {
                    CombatUtil.ring(level, ParticleTypes.FLAME, at, 3.0, 24);
                    for (LivingEntity e : CombatUtil.around(level, p, at, 4)) {
                        CombatUtil.magicDamage(level, p, e, 4f);
                        e.setRemainingFireTicks(80);
                    }
                }
            }
            case HANAMI -> {
                // roots drain the living around him
                if (t % 20 == 0) {
                    CombatUtil.ring(level, ParticleTypes.COMPOSTER, at, 3.5, 26);
                    for (LivingEntity e : CombatUtil.around(level, p, at, 5)) {
                        CombatUtil.effect(e, SLOWNESS, 60, 1);
                        CombatUtil.magicDamage(level, p, e, 3f);
                    }
                    CombatUtil.effect(p, REGEN, 60, 1);
                }
            }
            case DAGON -> {
                if (t % 14 == 0) CombatUtil.ring(level, ParticleTypes.SPLASH, at, 2.8, 22);
            }
            case YOROZU -> {
                if (t % 12 == 0) CombatUtil.burst(level, ParticleTypes.CRIT, at, 8, 0.6);
            }
            default -> {}
        }
    }

    /* ============================ GOJO ============================ */
    private static void gojo(ServerPlayer p, ServerLevel level, CursedEnergy.Data d, int slot) {
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getViewVector(1f).normalize();
        switch (slot) {
            case 0 -> { // BLUE — implode
                if (!pay(p, d, 20)) return;
                Vec3 focus = eye.add(look.scale(8));
                CombatUtil.particles(level, ParticleTypes.SONIC_BOOM, focus, 1, 0, 0);
                CombatUtil.burst(level, ParticleTypes.PORTAL, focus, 40, 1.2);
                CombatUtil.ring(level, ParticleTypes.END_ROD, focus, 2.5, 24);
                CombatUtil.sound(level, focus, S_PORTAL, 0.8f, 1.6f);
                for (LivingEntity e : CombatUtil.around(level, p, focus, 6)) {
                    CombatUtil.pullToward(e, focus, 1.1);
                    CombatUtil.magicDamage(level, p, e, 4f);
                }
            }
            case 1 -> { // RED — repel blast
                if (!pay(p, d, 25)) return;
                Vec3 focus = eye.add(look.scale(4));
                CombatUtil.burst(level, ParticleTypes.EXPLOSION, focus, 4, 0.5);
                CombatUtil.burst(level, ParticleTypes.FLAME, focus, 40, 1.0);
                CombatUtil.sound(level, focus, S_EXPLODE, 0.9f, 1.3f);
                for (LivingEntity e : CombatUtil.around(level, p, focus, 6)) {
                    CombatUtil.knockAway(e, focus, 2.2, 0.6);
                    CombatUtil.magicDamage(level, p, e, 8f);
                }
            }
            case 2 -> { // HOLLOW PURPLE — piercing beam
                if (!pay(p, d, 90)) return;
                domainText(p, "§5§lHOLLOW PURPLE");
                CombatUtil.sound(level, eye, S_BOOM, 1.0f, 0.8f);
                CombatUtil.beam(level, ParticleTypes.WITCH, eye, look, 30, 3);
                CombatUtil.beam(level, ParticleTypes.SOUL_FIRE_FLAME, eye, look, 30, 2);
                for (int i = 1; i <= 30; i++) {
                    Vec3 point = eye.add(look.scale(i));
                    for (LivingEntity e : CombatUtil.around(level, p, point, 2.2)) {
                        CombatUtil.magicDamage(level, p, e, 14f);
                        CombatUtil.knockAway(e, eye, 1.5, 0.3);
                    }
                }
            }
            case 3 -> { // BLUE MAXIMUM
                if (!pay(p, d, 30)) return;
                Vec3 focus = eye.add(look.scale(7));
                CombatUtil.burst(level, ParticleTypes.PORTAL, focus, 60, 1.6);
                CombatUtil.ring(level, ParticleTypes.END_ROD, focus, 3.5, 32);
                CombatUtil.particles(level, ParticleTypes.SONIC_BOOM, focus, 1, 0, 0);
                CombatUtil.sound(level, focus, S_BOOM, 0.9f, 1.4f);
                for (LivingEntity e : CombatUtil.around(level, p, focus, 7)) {
                    CombatUtil.pullToward(e, focus, 1.4);
                    CombatUtil.magicDamage(level, p, e, 6f);
                }
            }
            case 4 -> { // RED MAXIMUM
                if (!pay(p, d, 35)) return;
                Vec3 focus = eye.add(look.scale(5));
                CombatUtil.burst(level, ParticleTypes.EXPLOSION, focus, 8, 0.7);
                CombatUtil.burst(level, ParticleTypes.FLAME, focus, 60, 1.4);
                CombatUtil.sound(level, focus, S_EXPLODE, 1.0f, 0.9f);
                for (LivingEntity e : CombatUtil.cone(p, 8, 45)) {
                    CombatUtil.knockAway(e, p.position(), 3.0, 0.8);
                    CombatUtil.magicDamage(level, p, e, 10f);
                }
            }
        }
    }

    /* ============================ SUKUNA ============================ */
    private static void sukuna(ServerPlayer p, ServerLevel level, CursedEnergy.Data d, int slot) {
        switch (slot) {
            case 0 -> {
                if (!pay(p, d, 15)) return;
                slashParticles(level, p, 5);
                CombatUtil.sound(level, p.position(), S_SWEEP, 0.8f, 1.2f);
                for (LivingEntity e : CombatUtil.cone(p, 6, 40)) CombatUtil.magicDamage(level, p, e, 7f);
            }
            case 1 -> {
                if (!pay(p, d, 25)) return;
                LivingEntity t = CombatUtil.raycastLiving(p, 6);
                if (t == null) { msg(p, "§cNo target."); return; }
                slashParticles(level, p, 8);
                CombatUtil.sound(level, t.position(), S_CRIT, 1.0f, 0.8f);
                float bonus = t.getMaxHealth() > 40 ? 6f : 0f;
                CombatUtil.magicDamage(level, p, t, 12f + bonus);
                CombatUtil.knockAway(t, p.position(), 0.8, 0.3);
            }
            case 2 -> {
                if (!pay(p, d, 80)) return;
                domainText(p, "§4§lMALEVOLENT SHRINE");
                CombatUtil.effect(p, RESISTANCE, 120, 1);
                CombatUtil.sound(level, p.position(), S_ROAR, 1.0f, 0.5f);
                Vec3 center = p.position();
                CombatUtil.ring(level, ParticleTypes.SOUL_FIRE_FLAME, center, 8, 60);
                for (int wave = 0; wave < 6; wave++) {
                    final int w = wave;
                    JJKMod.schedule(w * 10, () -> {
                        slashParticles(level, p, 12);
                        CombatUtil.ring(level, ParticleTypes.SWEEP_ATTACK, center, 6, 24);
                        for (LivingEntity e : CombatUtil.around(level, p, center, 8)) {
                            CombatUtil.magicDamage(level, p, e, 6f);
                        }
                    });
                }
            }
            case 3 -> { // WIDE CLEAVE
                if (!pay(p, d, 20)) return;
                slashParticles(level, p, 10);
                CombatUtil.sound(level, p.position(), S_SWEEP, 1.0f, 0.7f);
                for (LivingEntity e : CombatUtil.cone(p, 8, 60)) CombatUtil.magicDamage(level, p, e, 9f);
            }
            case 4 -> { // FIRE ARROW
                if (!pay(p, d, 25)) return;
                Vec3 eye = p.getEyePosition();
                Vec3 look = p.getViewVector(1f).normalize();
                CombatUtil.beam(level, ParticleTypes.FLAME, eye, look, 22, 3);
                CombatUtil.sound(level, eye, S_SHOOT, 1.0f, 0.7f);
                LivingEntity t = CombatUtil.raycastLiving(p, 22);
                if (t == null) { msg(p, "§cNo target."); return; }
                CombatUtil.burst(level, ParticleTypes.LAVA, t.position(), 12, 0.4);
                CombatUtil.magicDamage(level, p, t, 13f);
            }
        }
    }

    /* ============================ YUJI ============================ */
    private static void yuji(ServerPlayer p, ServerLevel level, CursedEnergy.Data d, int slot) {
        switch (slot) {
            case 0 -> { // DIVERGENT FIST
                if (!pay(p, d, 8)) return;
                LivingEntity t = CombatUtil.raycastLiving(p, 5);
                if (t == null) { msg(p, "§cNo target."); return; }
                CombatUtil.physicalDamage(level, p, t, 6f);
                CombatUtil.burst(level, ParticleTypes.CRIT, t.position(), 12, 0.4);
                CombatUtil.sound(level, t.position(), S_CRIT, 0.9f, 1.0f);
                JJKMod.schedule(6, () -> {
                    if (t.isAlive()) {
                        CombatUtil.physicalDamage(level, p, t, 5f);
                        CombatUtil.burst(level, ParticleTypes.ENCHANTED_HIT, t.position(), 14, 0.4);
                        CombatUtil.sound(level, t.position(), S_CRIT, 0.9f, 0.7f);
                    }
                });
            }
            case 1 -> { // HEAVY STRIKE
                if (!pay(p, d, 12)) return;
                LivingEntity t = CombatUtil.raycastLiving(p, 5);
                if (t == null) { msg(p, "§cNo target."); return; }
                CombatUtil.physicalDamage(level, p, t, 13f);
                CombatUtil.knockAway(t, p.position(), 1.2, 0.35);
                CombatUtil.burst(level, ParticleTypes.CRIT, t.position(), 20, 0.5);
                CombatUtil.sound(level, t.position(), S_ANVIL, 0.7f, 1.5f);
            }
            case 2 -> { // MANJI KICK
                if (!pay(p, d, 10)) return;
                CombatUtil.effect(p, SPEED, 100, 1);
                CombatUtil.sound(level, p.position(), S_SWEEP, 0.9f, 1.3f);
                for (LivingEntity e : CombatUtil.cone(p, 4, 50)) {
                    CombatUtil.physicalDamage(level, p, e, 6f);
                    CombatUtil.knockAway(e, p.position(), 1.8, 0.5);
                }
            }
            case 3 -> { // RATIO — 3 rapid strikes
                if (!pay(p, d, 12)) return;
                LivingEntity t = CombatUtil.raycastLiving(p, 5);
                if (t == null) { msg(p, "§cNo target."); return; }
                for (int i = 0; i < 3; i++) {
                    JJKMod.schedule(i * 4, () -> {
                        if (t.isAlive()) {
                            CombatUtil.physicalDamage(level, p, t, 4f);
                            CombatUtil.burst(level, ParticleTypes.CRIT, t.position(), 8, 0.3);
                            CombatUtil.sound(level, t.position(), S_CRIT, 0.7f, 1.4f);
                        }
                    });
                }
            }
            case 4 -> { // DIVERGENT BARRAGE
                if (!pay(p, d, 14)) return;
                CombatUtil.effect(p, SPEED, 60, 0);
                slashParticles(level, p, 6);
                CombatUtil.sound(level, p.position(), S_SWEEP, 1.0f, 1.0f);
                for (LivingEntity e : CombatUtil.cone(p, 5, 45)) {
                    CombatUtil.physicalDamage(level, p, e, 7f);
                    CombatUtil.knockAway(e, p.position(), 1.2, 0.35);
                }
            }
        }
    }

    /* ============================ MEGUMI ============================ */
    private static void megumi(ServerPlayer p, ServerLevel level, CursedEnergy.Data d, int slot) {
        switch (slot) {
            case 0 -> {
                if (!pay(p, d, 30)) return;
                LivingEntity t = CombatUtil.raycastLiving(p, 20);
                Vec3 at = p.position().add(p.getViewVector(1f).scale(2));
                CombatUtil.summonWolf(level, p, at.add(0.6, 0, 0), t);
                CombatUtil.summonWolf(level, p, at.add(-0.6, 0, 0), t);
                CombatUtil.burst(level, ParticleTypes.SMOKE, at, 30, 0.7);
                CombatUtil.ring(level, ParticleTypes.SCULK_SOUL, at, 1.5, 14);
                CombatUtil.sound(level, at, S_CAST, 0.9f, 0.9f);
            }
            case 1 -> {
                if (!pay(p, d, 25)) return;
                LivingEntity t = CombatUtil.raycastLiving(p, 24);
                Vec3 strike = (t != null) ? t.position()
                        : p.getEyePosition().add(p.getViewVector(1f).scale(16));
                CombatUtil.lightning(level, strike);
                CombatUtil.sound(level, strike, S_THUNDER, 1.0f, 1.2f);
                for (LivingEntity e : CombatUtil.around(level, p, strike, 4)) {
                    CombatUtil.effect(e, LEVITATION, 40, 1);
                    CombatUtil.magicDamage(level, p, e, 6f);
                }
            }
            case 2 -> {
                if (!pay(p, d, 90)) return;
                domainText(p, "§8§lCHIMERA SHADOW GARDEN");
                CombatUtil.sound(level, p.position(), S_ROAR, 1.0f, 0.6f);
                Vec3 base = p.position();
                CombatUtil.ring(level, ParticleTypes.SCULK_SOUL, base, 10, 72);
                for (int i = 0; i < 5; i++) {
                    double a = (Math.PI * 2 / 5) * i;
                    Vec3 at = base.add(Math.cos(a) * 3, 0, Math.sin(a) * 3);
                    CombatUtil.summonWolf(level, p, at, null);
                    CombatUtil.burst(level, ParticleTypes.SMOKE, at, 15, 0.5);
                }
                for (LivingEntity e : CombatUtil.around(level, p, base, 10)) {
                    CombatUtil.effect(e, DARKNESS, 120, 0);
                    CombatUtil.effect(e, SLOWNESS, 120, 1);
                }
            }
            case 3 -> { // GREAT SERPENT
                if (!pay(p, d, 20)) return;
                Vec3 eye = p.getEyePosition();
                Vec3 look = p.getViewVector(1f).normalize();
                CombatUtil.beam(level, ParticleTypes.SCULK_SOUL, eye, look, 16, 3);
                CombatUtil.sound(level, eye, S_CAST, 0.9f, 0.6f);
                for (int i = 1; i <= 16; i++) {
                    Vec3 point = eye.add(look.scale(i));
                    for (LivingEntity e : CombatUtil.around(level, p, point, 1.8)) {
                        CombatUtil.magicDamage(level, p, e, 7f);
                    }
                }
            }
            case 4 -> { // TOAD
                if (!pay(p, d, 18)) return;
                LivingEntity t = CombatUtil.raycastLiving(p, 20);
                if (t == null) { msg(p, "§cNo target."); return; }
                CombatUtil.pullToward(t, p.position(), 1.6);
                CombatUtil.magicDamage(level, p, t, 5f);
                CombatUtil.burst(level, ParticleTypes.SMOKE, t.position(), 24, 0.5);
                CombatUtil.sound(level, t.position(), S_CAST, 0.8f, 1.3f);
            }
        }
    }

    /* ============================ NOBARA ============================ */
    private static void nobara(ServerPlayer p, ServerLevel level, CursedEnergy.Data d, int slot) {
        switch (slot) {
            case 0 -> {
                if (!pay(p, d, 10)) return;
                LivingEntity t = CombatUtil.raycastLiving(p, 18);
                if (t == null) { msg(p, "§cNo target."); return; }
                CombatUtil.burst(level, ParticleTypes.EXPLOSION, t.position(), 3, 0.3);
                CombatUtil.sound(level, t.position(), S_EXPLODE, 0.6f, 1.7f);
                CombatUtil.magicDamage(level, p, t, 8f);
                for (LivingEntity e : CombatUtil.around(level, p, t.position(), 2.5)) {
                    if (e != t) CombatUtil.magicDamage(level, p, e, 3f);
                }
            }
            case 1 -> {
                if (!pay(p, d, 12)) return;
                LivingEntity t = CombatUtil.raycastLiving(p, 18);
                if (t == null) { msg(p, "§cNo target."); return; }
                CombatUtil.effect(t, GLOWING, 100, 0);
                CombatUtil.effect(t, WEAKNESS, 100, 1);
                CombatUtil.sound(level, t.position(), S_CAST, 0.8f, 1.5f);
                for (int i = 1; i <= 5; i++) {
                    JJKMod.schedule(i * 15, () -> {
                        if (t.isAlive()) {
                            CombatUtil.magicDamage(level, p, t, 3f);
                            CombatUtil.burst(level, ParticleTypes.CRIT, t.position(), 8, 0.25);
                        }
                    });
                }
            }
            case 2 -> {
                if (!pay(p, d, 40)) return;
                domainText(p, "§e§lRESONANCE — HAIRPIN");
                Vec3 aim = p.getEyePosition().add(p.getViewVector(1f).scale(6));
                CombatUtil.burst(level, ParticleTypes.EXPLOSION, aim, 8, 0.7);
                CombatUtil.ring(level, ParticleTypes.CRIT, aim, 6, 40);
                CombatUtil.sound(level, aim, S_EXPLODE, 1.0f, 1.1f);
                for (LivingEntity e : CombatUtil.around(level, p, aim, 6)) {
                    CombatUtil.magicDamage(level, p, e, 12f);
                    CombatUtil.knockAway(e, aim, 1.2, 0.4);
                }
            }
            case 3 -> { // NAIL BARRAGE
                if (!pay(p, d, 15)) return;
                LivingEntity t = CombatUtil.raycastLiving(p, 18);
                if (t == null) { msg(p, "§cNo target."); return; }
                for (int i = 0; i < 3; i++) {
                    JJKMod.schedule(i * 5, () -> {
                        if (t.isAlive()) {
                            CombatUtil.magicDamage(level, p, t, 4f);
                            CombatUtil.burst(level, ParticleTypes.CRIT, t.position(), 8, 0.25);
                            CombatUtil.sound(level, t.position(), S_SHOOT, 0.5f, 1.8f);
                        }
                    });
                }
            }
            case 4 -> { // HAMMER
                if (!pay(p, d, 18)) return;
                LivingEntity t = CombatUtil.raycastLiving(p, 6);
                if (t == null) { msg(p, "§cNo target."); return; }
                CombatUtil.magicDamage(level, p, t, 14f);
                CombatUtil.knockAway(t, p.position(), 1.6, 0.6);
                CombatUtil.burst(level, ParticleTypes.EXPLOSION, t.position(), 4, 0.3);
                CombatUtil.sound(level, t.position(), S_ANVIL, 0.9f, 1.0f);
            }
        }
    }

    /* ==================== TOJI (Heavenly Restriction) ==================== */
    private static void toji(ServerPlayer p, ServerLevel level, CursedEnergy.Data d, int slot) {
        float w = CursedWeapons.bonus(p);   // cursed weapon damage bonus
        switch (slot) {
            case 0 -> { // INVERTED SPEAR
                LivingEntity t = CombatUtil.raycastLiving(p, 5);
                if (t == null) { msg(p, "§cNo target."); return; }
                CombatUtil.physicalDamage(level, p, t, 16f + w);
                CombatUtil.knockAway(t, p.position(), 1.0, 0.2);
                CombatUtil.burst(level, ParticleTypes.SWEEP_ATTACK, t.position(), 4, 0.25);
                CombatUtil.sound(level, t.position(), S_CRIT, 1.0f, 0.7f);
                CursedEnergy.addAwaken(d, 10f);
            }
            case 1 -> { // PLAYFUL CLOUD — wide sweep
                slashParticles(level, p, 6);
                CombatUtil.sound(level, p.position(), S_SWEEP, 1.0f, 0.8f);
                for (LivingEntity e : CombatUtil.cone(p, 5, 60)) {
                    CombatUtil.physicalDamage(level, p, e, 10f + w);
                    CombatUtil.knockAway(e, p.position(), 1.4, 0.4);
                }
                CursedEnergy.addAwaken(d, 10f);
            }
            case 2 -> { // HEAVENLY RESTRICTION burst
                CombatUtil.effect(p, SPEED, 200, 2);
                CombatUtil.effect(p, STRENGTH, 200, 1);
                CombatUtil.effect(p, RESISTANCE, 200, 1);
                CombatUtil.effect(p, HASTE, 200, 2);
                domainText(p, "§f§lHEAVENLY RESTRICTION");
                CombatUtil.spiral(level, ParticleTypes.END_ROD, p.position(), 1.0, 2.5, 40);
                CombatUtil.sound(level, p.position(), S_TOTEM, 1.0f, 1.0f);
                CursedEnergy.addAwaken(d, 8f);
            }
            case 3 -> { // SPEAR THROW
                LivingEntity t = CombatUtil.raycastLiving(p, 18);
                if (t == null) { msg(p, "§cNo target."); return; }
                CombatUtil.beam(level, ParticleTypes.CRIT, p.getEyePosition(),
                        p.getViewVector(1f), 18, 2);
                CombatUtil.physicalDamage(level, p, t, 12f + w);
                CombatUtil.knockAway(t, p.position(), 1.2, 0.3);
                CombatUtil.sound(level, t.position(), S_SHOOT, 0.9f, 1.1f);
                CursedEnergy.addAwaken(d, 12f);
            }
            case 4 -> { // CHAIN PULL
                LivingEntity t = CombatUtil.raycastLiving(p, 16);
                if (t == null) { msg(p, "§cNo target."); return; }
                CombatUtil.beam(level, ParticleTypes.SMOKE, p.getEyePosition(),
                        p.getViewVector(1f), 16, 2);
                CombatUtil.pullToward(t, p.position(), 1.8);
                CombatUtil.physicalDamage(level, p, t, 6f + w);
                CursedEnergy.addAwaken(d, 10f);
            }
        }
    }

    /* ============================ GETO ============================ */
    private static void geto(ServerPlayer p, ServerLevel level, CursedEnergy.Data d, int slot) {
        switch (slot) {
            case 0 -> { // CURSE SPIRIT — summon an attacker
                if (!pay(p, d, 25)) return;
                LivingEntity t = CombatUtil.raycastLiving(p, 20);
                Vec3 at = p.position().add(p.getViewVector(1f).scale(2));
                CombatUtil.summonWolf(level, p, at, t);
                CombatUtil.burst(level, ParticleTypes.SCULK_SOUL, at, 25, 0.6);
                CombatUtil.sound(level, at, S_CAST, 0.9f, 0.7f);
            }
            case 1 -> { // CURSE BOLT — ranged spirit shot
                if (!pay(p, d, 18)) return;
                Vec3 eye = p.getEyePosition();
                Vec3 look = p.getViewVector(1f).normalize();
                CombatUtil.beam(level, ParticleTypes.SOUL, eye, look, 20, 3);
                CombatUtil.sound(level, eye, S_WITHER, 0.8f, 1.2f);
                LivingEntity t = CombatUtil.raycastLiving(p, 20);
                if (t == null) { msg(p, "§cNo target."); return; }
                CombatUtil.magicDamage(level, p, t, 10f);
                CombatUtil.effect(t, WITHER, 60, 0);
            }
            case 2 -> { // UZUMAKI — massed curse detonation
                if (!pay(p, d, 85)) return;
                domainText(p, "§2§lUZUMAKI");
                Vec3 aim = p.getEyePosition().add(p.getViewVector(1f).scale(6));
                CombatUtil.sound(level, aim, S_ROAR, 1.0f, 0.8f);
                CombatUtil.ring(level, ParticleTypes.SCULK_SOUL, aim, 5, 40);
                for (int i = 0; i < 4; i++) {
                    final int w = i;
                    JJKMod.schedule(w * 8, () -> {
                        CombatUtil.burst(level, ParticleTypes.SOUL_FIRE_FLAME, aim, 30, 1.0);
                        for (LivingEntity e : CombatUtil.around(level, p, aim, 7)) {
                            CombatUtil.magicDamage(level, p, e, 9f);
                            CombatUtil.knockAway(e, aim, 0.8, 0.3);
                        }
                    });
                }
            }
            case 3 -> { // SWARM — three spirits at once
                if (!pay(p, d, 40)) return;
                Vec3 base = p.position();
                for (int i = 0; i < 3; i++) {
                    double a = (Math.PI * 2 / 3) * i;
                    Vec3 at = base.add(Math.cos(a) * 2, 0, Math.sin(a) * 2);
                    CombatUtil.summonWolf(level, p, at, null);
                    CombatUtil.burst(level, ParticleTypes.SCULK_SOUL, at, 15, 0.5);
                }
                CombatUtil.sound(level, base, S_CAST, 1.0f, 0.6f);
            }
            case 4 -> { // ROT FIELD — lingering decay around you
                if (!pay(p, d, 30)) return;
                Vec3 base = p.position();
                CombatUtil.ring(level, ParticleTypes.SOUL, base, 6, 44);
                CombatUtil.sound(level, base, S_WITHER, 0.9f, 0.7f);
                for (LivingEntity e : CombatUtil.around(level, p, base, 6)) {
                    CombatUtil.effect(e, POISON, 100, 1);
                    CombatUtil.effect(e, SLOWNESS, 100, 1);
                    CombatUtil.magicDamage(level, p, e, 5f);
                }
            }
        }
    }

    /* ============================ NANAMI ============================ */
    private static void nanami(ServerPlayer p, ServerLevel level, CursedEnergy.Data d, int slot) {
        switch (slot) {
            case 0 -> { // RATIO — strike the weak point (7:3)
                if (!pay(p, d, 14)) return;
                LivingEntity t = CombatUtil.raycastLiving(p, 5);
                if (t == null) { msg(p, "§cNo target."); return; }
                CombatUtil.physicalDamage(level, p, t, 11f);
                CombatUtil.effect(t, GLOWING, 120, 0);   // weak point marked
                CombatUtil.burst(level, ParticleTypes.ENCHANTED_HIT, t.position(), 16, 0.35);
                CombatUtil.sound(level, t.position(), S_CRIT, 1.0f, 1.2f);
            }
            case 1 -> { // COLLAPSE — big damage on a marked (glowing) target
                if (!pay(p, d, 22)) return;
                LivingEntity t = CombatUtil.raycastLiving(p, 6);
                if (t == null) { msg(p, "§cNo target."); return; }
                boolean marked = t.hasEffect(GLOWING);
                float dmg = marked ? 22f : 11f;
                if (marked) domainText(p, "§6§lRATIO — COLLAPSE");
                CombatUtil.physicalDamage(level, p, t, dmg);
                CombatUtil.burst(level, ParticleTypes.CRIT, t.position(), marked ? 40 : 16, 0.5);
                CombatUtil.sound(level, t.position(), S_ANVIL, 1.0f, marked ? 0.7f : 1.3f);
            }
            case 2 -> { // OVERTIME — surgical burst
                if (!pay(p, d, 45)) return;
                domainText(p, "§6§lOVERTIME");
                CombatUtil.effect(p, STRENGTH, 200, 1);
                CombatUtil.effect(p, HASTE, 200, 2);
                CombatUtil.spiral(level, ParticleTypes.ENCHANTED_HIT, p.position(), 1.0, 2.5, 40);
                CombatUtil.sound(level, p.position(), S_TOTEM, 1.0f, 1.2f);
                for (LivingEntity e : CombatUtil.cone(p, 6, 60)) {
                    CombatUtil.physicalDamage(level, p, e, 12f);
                }
            }
            case 3 -> { // CLEAVER SWING
                if (!pay(p, d, 16)) return;
                slashParticles(level, p, 7);
                CombatUtil.sound(level, p.position(), S_SWEEP, 0.9f, 0.9f);
                for (LivingEntity e : CombatUtil.cone(p, 6, 50)) {
                    CombatUtil.physicalDamage(level, p, e, 9f);
                    CombatUtil.knockAway(e, p.position(), 1.0, 0.3);
                }
            }
            case 4 -> { // CLOCK OUT — self heal + guard
                if (!pay(p, d, 25)) return;
                CombatUtil.effect(p, REGEN, 120, 1);
                CombatUtil.effect(p, RESISTANCE, 120, 1);
                CombatUtil.effect(p, ABSORPTION, 200, 1);
                CombatUtil.ring(level, ParticleTypes.END_ROD, p.position(), 1.6, 16);
                CombatUtil.sound(level, p.position(), S_TOTEM, 0.8f, 1.5f);
            }
        }
    }

    /* ==================== MAKI (Heavenly Restriction) ==================== */
    private static void maki(ServerPlayer p, ServerLevel level, CursedEnergy.Data d, int slot) {
        float w = CursedWeapons.bonus(p);
        switch (slot) {
            case 0 -> { // SPLIT SOUL SLASH
                LivingEntity t = CombatUtil.raycastLiving(p, 5);
                if (t == null) { msg(p, "§cNo target."); return; }
                CombatUtil.physicalDamage(level, p, t, 15f + w);
                CombatUtil.burst(level, ParticleTypes.SWEEP_ATTACK, t.position(), 5, 0.3);
                CombatUtil.sound(level, t.position(), S_CRIT, 1.0f, 0.9f);
                CursedEnergy.addAwaken(d, 10f);
            }
            case 1 -> { // WHIRLWIND — spin attack all around
                slashParticles(level, p, 8);
                CombatUtil.ring(level, ParticleTypes.SWEEP_ATTACK, p.position(), 3.5, 26);
                CombatUtil.sound(level, p.position(), S_SWEEP, 1.0f, 0.75f);
                for (LivingEntity e : CombatUtil.around(level, p, p.position(), 4)) {
                    CombatUtil.physicalDamage(level, p, e, 11f + w);
                    CombatUtil.knockAway(e, p.position(), 1.5, 0.4);
                }
                CursedEnergy.addAwaken(d, 12f);
            }
            case 2 -> { // ZENIN FURY — sustained assault buff
                CombatUtil.effect(p, STRENGTH, 200, 2);
                CombatUtil.effect(p, SPEED, 200, 2);
                CombatUtil.effect(p, RESISTANCE, 200, 1);
                domainText(p, "§a§lZENIN FURY");
                CombatUtil.spiral(level, ParticleTypes.SWEEP_ATTACK, p.position(), 1.0, 2.5, 36);
                CombatUtil.sound(level, p.position(), S_TOTEM, 1.0f, 0.9f);
                CursedEnergy.addAwaken(d, 8f);
            }
            case 3 -> { // POLEARM THRUST — long reach
                LivingEntity t = CombatUtil.raycastLiving(p, 8);
                if (t == null) { msg(p, "§cNo target."); return; }
                CombatUtil.beam(level, ParticleTypes.CRIT, p.getEyePosition(),
                        p.getViewVector(1f), 8, 3);
                CombatUtil.physicalDamage(level, p, t, 14f + w);
                CombatUtil.knockAway(t, p.position(), 1.1, 0.25);
                CombatUtil.sound(level, t.position(), S_CRIT, 0.9f, 0.8f);
                CursedEnergy.addAwaken(d, 10f);
            }
            case 4 -> { // LEAP SLAM — jump then crash down
                CombatUtil.effect(p, JUMP, 60, 3);
                CombatUtil.effect(p, SPEED, 60, 1);
                Vec3 at = p.position();
                CombatUtil.ring(level, ParticleTypes.EXPLOSION, at, 3.0, 20);
                CombatUtil.sound(level, at, S_ANVIL, 1.0f, 0.8f);
                for (LivingEntity e : CombatUtil.around(level, p, at, 4.5)) {
                    CombatUtil.physicalDamage(level, p, e, 12f + w);
                    CombatUtil.knockAway(e, at, 1.6, 0.7);
                }
                CursedEnergy.addAwaken(d, 12f);
            }
        }
    }

    /* ============================ MAI ============================ */
    private static void mai(ServerPlayer p, ServerLevel level, CursedEnergy.Data d, int slot) {
        float w = CursedWeapons.bonus(p);   // her constructed Gun
        switch (slot) {
            case 0 -> { // CONSTRUCTED SHOT — sniper round
                if (!pay(p, d, 14)) return;
                Vec3 eye = p.getEyePosition();
                Vec3 look = p.getViewVector(1f).normalize();
                CombatUtil.beam(level, ParticleTypes.END_ROD, eye, look, 28, 4);
                CombatUtil.sound(level, eye, S_SHOOT, 1.0f, 1.6f);
                LivingEntity t = CombatUtil.raycastLiving(p, 28);
                if (t == null) { msg(p, "§cNo target."); return; }
                CombatUtil.magicDamage(level, p, t, 13f + w);
                CombatUtil.burst(level, ParticleTypes.CRIT, t.position(), 14, 0.3);
            }
            case 1 -> { // DOUBLE TAP — two quick rounds
                if (!pay(p, d, 18)) return;
                LivingEntity t = CombatUtil.raycastLiving(p, 26);
                if (t == null) { msg(p, "§cNo target."); return; }
                for (int i = 0; i < 2; i++) {
                    JJKMod.schedule(i * 5, () -> {
                        if (t.isAlive()) {
                            CombatUtil.magicDamage(level, p, t, 8f);
                            CombatUtil.burst(level, ParticleTypes.END_ROD, t.position(), 10, 0.3);
                            CombatUtil.sound(level, t.position(), S_SHOOT, 0.8f, 1.8f);
                        }
                    });
                }
            }
            case 2 -> { // FULL ROUND — costly heavy slug
                if (!pay(p, d, 55)) return;
                domainText(p, "§d§lCONSTRUCTION — FULL ROUND");
                Vec3 eye = p.getEyePosition();
                Vec3 look = p.getViewVector(1f).normalize();
                CombatUtil.beam(level, ParticleTypes.END_ROD, eye, look, 32, 5);
                CombatUtil.beam(level, ParticleTypes.FLAME, eye, look, 32, 2);
                CombatUtil.sound(level, eye, S_EXPLODE, 1.0f, 1.4f);
                for (int i = 1; i <= 32; i++) {
                    Vec3 point = eye.add(look.scale(i));
                    for (LivingEntity e : CombatUtil.around(level, p, point, 2.0)) {
                        CombatUtil.magicDamage(level, p, e, 16f + w);
                        CombatUtil.knockAway(e, eye, 1.2, 0.3);
                    }
                }
            }
            case 3 -> { // SCATTER — shotgun cone
                if (!pay(p, d, 20)) return;
                CombatUtil.sound(level, p.position(), S_SHOOT, 1.0f, 0.9f);
                for (int i = 0; i < 12; i++) {
                    Vec3 dir = p.getViewVector(1f).add(
                            (Math.random() - 0.5) * 0.5, (Math.random() - 0.5) * 0.5,
                            (Math.random() - 0.5) * 0.5);
                    CombatUtil.beam(level, ParticleTypes.END_ROD, p.getEyePosition(), dir, 8, 2);
                }
                for (LivingEntity e : CombatUtil.cone(p, 9, 30)) {
                    CombatUtil.magicDamage(level, p, e, 9f);
                    CombatUtil.knockAway(e, p.position(), 1.0, 0.3);
                }
            }
            case 4 -> { // CONSTRUCT GUARD — conjured cover
                if (!pay(p, d, 22)) return;
                CombatUtil.effect(p, RESISTANCE, 160, 2);
                CombatUtil.effect(p, ABSORPTION, 200, 1);
                CombatUtil.ring(level, ParticleTypes.END_ROD, p.position(), 2.0, 20);
                CombatUtil.sound(level, p.position(), S_ANVIL, 0.8f, 1.6f);
            }
        }
    }

    /* ============================ YUTA ============================ */
    private static void yuta(ServerPlayer p, ServerLevel level, CursedEnergy.Data d, int slot) {
        switch (slot) {
            case 0 -> { // RIKA STRIKE
                if (!pay(p, d, 18)) return;
                LivingEntity t = CombatUtil.raycastLiving(p, 7);
                if (t == null) { msg(p, "§cNo target."); return; }
                CombatUtil.magicDamage(level, p, t, 14f);
                CombatUtil.knockAway(t, p.position(), 1.2, 0.3);
                CombatUtil.burst(level, ParticleTypes.SOUL, t.position(), 20, 0.4);
                CombatUtil.sound(level, t.position(), S_CRIT, 1.0f, 0.6f);
            }
            case 1 -> { // COPY — mimic a cheap cursed technique (ranged bolt)
                if (!pay(p, d, 22)) return;
                Vec3 eye = p.getEyePosition();
                Vec3 look = p.getViewVector(1f).normalize();
                CombatUtil.beam(level, ParticleTypes.WITCH, eye, look, 22, 3);
                CombatUtil.sound(level, eye, S_CAST, 0.9f, 1.4f);
                LivingEntity t = CombatUtil.raycastLiving(p, 22);
                if (t == null) { msg(p, "§cNo target."); return; }
                CombatUtil.magicDamage(level, p, t, 12f);
                CombatUtil.effect(t, SLOWNESS, 80, 1);
            }
            case 2 -> { // TRUE LOVE — massive AoE with self-buff
                if (!pay(p, d, 95)) return;
                domainText(p, "§b§lTRUE LOVE — RIKA");
                Vec3 base = p.position();
                CombatUtil.ring(level, ParticleTypes.SOUL, base, 9, 64);
                CombatUtil.spiral(level, ParticleTypes.SOUL_FIRE_FLAME, base, 2.0, 4.0, 70);
                CombatUtil.sound(level, base, S_ROAR, 1.0f, 1.3f);
                CombatUtil.effect(p, REGEN, 160, 1);
                CombatUtil.effect(p, ABSORPTION, 200, 2);
                for (LivingEntity e : CombatUtil.around(level, p, base, 9)) {
                    CombatUtil.magicDamage(level, p, e, 18f);
                    CombatUtil.knockAway(e, base, 1.6, 0.5);
                }
            }
            case 3 -> { // RIKA GUARD — she shields him
                if (!pay(p, d, 25)) return;
                CombatUtil.effect(p, RESISTANCE, 160, 2);
                CombatUtil.effect(p, ABSORPTION, 200, 2);
                CombatUtil.effect(p, REGEN, 100, 0);
                CombatUtil.ring(level, ParticleTypes.SOUL, p.position(), 1.8, 18);
                CombatUtil.sound(level, p.position(), S_TOTEM, 0.9f, 0.8f);
            }
            case 4 -> { // PURE LOVE — chained slashes on one target
                if (!pay(p, d, 30)) return;
                LivingEntity t = CombatUtil.raycastLiving(p, 8);
                if (t == null) { msg(p, "§cNo target."); return; }
                for (int i = 0; i < 4; i++) {
                    JJKMod.schedule(i * 5, () -> {
                        if (t.isAlive()) {
                            CombatUtil.magicDamage(level, p, t, 6f);
                            CombatUtil.burst(level, ParticleTypes.SOUL, t.position(), 10, 0.3);
                            CombatUtil.sound(level, t.position(), S_SWEEP, 0.7f, 1.3f);
                        }
                    });
                }
            }
        }
    }

    /* ============================ CHOSO ============================ */
    private static void choso(ServerPlayer p, ServerLevel level, CursedEnergy.Data d, int slot) {
        switch (slot) {
            case 0 -> { // PIERCING BLOOD — thin lethal jet
                if (!pay(p, d, 20)) return;
                Vec3 eye = p.getEyePosition();
                Vec3 look = p.getViewVector(1f).normalize();
                CombatUtil.beam(level, ParticleTypes.FALLING_LAVA, eye, look, 26, 4);
                CombatUtil.sound(level, eye, S_SHOOT, 0.9f, 0.6f);
                for (int i = 1; i <= 26; i++) {
                    Vec3 point = eye.add(look.scale(i));
                    for (LivingEntity e : CombatUtil.around(level, p, point, 1.6)) {
                        CombatUtil.magicDamage(level, p, e, 12f);
                    }
                }
            }
            case 1 -> { // BLOOD EDGE — close-range slash
                if (!pay(p, d, 15)) return;
                slashParticles(level, p, 6);
                CombatUtil.burst(level, ParticleTypes.FALLING_LAVA, p.position(), 12, 0.6);
                CombatUtil.sound(level, p.position(), S_SWEEP, 0.9f, 0.7f);
                for (LivingEntity e : CombatUtil.cone(p, 6, 45)) {
                    CombatUtil.magicDamage(level, p, e, 9f);
                }
            }
            case 2 -> { // SUPERNOVA — blood detonation
                if (!pay(p, d, 75)) return;
                domainText(p, "§4§lBLOOD METEORITE");
                Vec3 aim = p.getEyePosition().add(p.getViewVector(1f).scale(7));
                CombatUtil.burst(level, ParticleTypes.EXPLOSION, aim, 10, 1.0);
                CombatUtil.ring(level, ParticleTypes.FALLING_LAVA, aim, 6, 44);
                CombatUtil.sound(level, aim, S_EXPLODE, 1.0f, 0.6f);
                for (LivingEntity e : CombatUtil.around(level, p, aim, 7)) {
                    CombatUtil.magicDamage(level, p, e, 17f);
                    CombatUtil.knockAway(e, aim, 1.5, 0.5);
                }
            }
            case 3 -> { // CONVERGENCE — drain life from a target
                if (!pay(p, d, 24)) return;
                LivingEntity t = CombatUtil.raycastLiving(p, 14);
                if (t == null) { msg(p, "§cNo target."); return; }
                CombatUtil.beam(level, ParticleTypes.FALLING_LAVA, t.getEyePosition(),
                        p.getEyePosition().subtract(t.getEyePosition()), 6, 3);
                CombatUtil.magicDamage(level, p, t, 8f);
                CombatUtil.effect(p, REGEN, 80, 1);
                CombatUtil.sound(level, t.position(), S_WITHER, 0.8f, 1.3f);
            }
            case 4 -> { // FLOWING SCALE — self buff
                if (!pay(p, d, 20)) return;
                CombatUtil.effect(p, STRENGTH, 160, 1);
                CombatUtil.effect(p, RESISTANCE, 160, 1);
                CombatUtil.effect(p, FIRE_RES, 200, 0);
                CombatUtil.ring(level, ParticleTypes.FALLING_LAVA, p.position(), 1.6, 16);
                CombatUtil.sound(level, p.position(), S_TOTEM, 0.8f, 0.7f);
            }
        }
    }

    /* ============================ MAHITO ============================ */
    private static void mahito(ServerPlayer p, ServerLevel level, CursedEnergy.Data d, int slot) {
        switch (slot) {
            case 0 -> { // SOUL TOUCH — warp a target's body
                if (!pay(p, d, 16)) return;
                LivingEntity t = CombatUtil.raycastLiving(p, 5);
                if (t == null) { msg(p, "§cNo target."); return; }
                CombatUtil.magicDamage(level, p, t, 10f);
                CombatUtil.effect(t, NAUSEA, 120, 0);
                CombatUtil.effect(t, WEAKNESS, 120, 1);
                CombatUtil.burst(level, ParticleTypes.SOUL, t.position(), 20, 0.4);
                CombatUtil.sound(level, t.position(), S_WITHER, 0.8f, 1.5f);
            }
            case 1 -> { // BODY REPEL — transfigured shockwave
                if (!pay(p, d, 22)) return;
                CombatUtil.ring(level, ParticleTypes.SOUL, p.position(), 4.0, 32);
                CombatUtil.sound(level, p.position(), S_EXPLODE, 0.8f, 1.5f);
                for (LivingEntity e : CombatUtil.around(level, p, p.position(), 5)) {
                    CombatUtil.magicDamage(level, p, e, 9f);
                    CombatUtil.knockAway(e, p.position(), 2.0, 0.6);
                    CombatUtil.effect(e, NAUSEA, 80, 0);
                }
            }
            case 2 -> { // SELF-EMBODIMENT — the big one
                if (!pay(p, d, 85)) return;
                domainText(p, "§5§lSELF-EMBODIMENT OF PERFECTION");
                Vec3 base = p.position();
                CombatUtil.ring(level, ParticleTypes.SOUL, base, 8, 60);
                CombatUtil.spiral(level, ParticleTypes.SCULK_SOUL, base, 1.8, 3.5, 60);
                CombatUtil.sound(level, base, S_ROAR, 1.0f, 1.5f);
                CombatUtil.effect(p, RESISTANCE, 160, 2);
                for (LivingEntity e : CombatUtil.around(level, p, base, 8)) {
                    CombatUtil.magicDamage(level, p, e, 15f);
                    CombatUtil.effect(e, BLINDNESS, 100, 0);
                    CombatUtil.effect(e, WITHER, 100, 1);
                    CombatUtil.effect(e, SLOWNESS, 100, 1);
                }
            }
            case 3 -> { // TRANSFIGURED SOULS — summon warped attackers
                if (!pay(p, d, 35)) return;
                Vec3 base = p.position();
                for (int i = 0; i < 2; i++) {
                    double a = Math.PI * i;
                    Vec3 at = base.add(Math.cos(a) * 2, 0, Math.sin(a) * 2);
                    CombatUtil.summonWolf(level, p, at, null);
                    CombatUtil.burst(level, ParticleTypes.SOUL, at, 18, 0.5);
                }
                CombatUtil.sound(level, base, S_CAST, 0.9f, 0.5f);
            }
            case 4 -> { // POLYMORPH — reshape himself, regenerate
                if (!pay(p, d, 26)) return;
                CombatUtil.effect(p, REGEN, 140, 1);
                CombatUtil.effect(p, RESISTANCE, 140, 1);
                CombatUtil.effect(p, SPEED, 140, 1);
                CombatUtil.spiral(level, ParticleTypes.SOUL, p.position(), 1.0, 2.2, 30);
                CombatUtil.sound(level, p.position(), S_TOTEM, 0.9f, 1.3f);
            }
        }
    }


    /* ============================ TODO ============================ */
    private static void todo(ServerPlayer p, ServerLevel level, CursedEnergy.Data d, int slot) {
        switch (slot) {
            case 0 -> { // CLAP — heavy palm strike
                if (!pay(p, d, 14)) return;
                LivingEntity t = CombatUtil.raycastLiving(p, 5);
                if (t == null) { msg(p, "\u00a7cNo target."); return; }
                CombatUtil.physicalDamage(level, p, t, 14f);
                CombatUtil.knockAway(t, p.position(), 1.6, 0.4);
                CombatUtil.burst(level, ParticleTypes.CRIT, t.position(), 18, 0.4);
                CombatUtil.sound(level, t.position(), S_ANVIL, 0.9f, 1.2f);
            }
            case 1 -> { // BOOGIE WOOGIE — swap places with your target
                if (!pay(p, d, 20)) return;
                LivingEntity t = CombatUtil.raycastLiving(p, 22);
                if (t == null) { msg(p, "\u00a7cNo target."); return; }
                Vec3 mine = p.position();
                Vec3 theirs = t.position();
                CombatUtil.burst(level, ParticleTypes.NOTE, mine, 20, 0.5);
                CombatUtil.burst(level, ParticleTypes.NOTE, theirs, 20, 0.5);
                CombatUtil.tp(p, theirs);
                CombatUtil.tp(t, mine);
                CombatUtil.sound(level, theirs, S_PORTAL, 0.9f, 1.5f);
                domainText(p, "\u00a7c\u00a7lBOOGIE WOOGIE");
            }
            case 2 -> { // BLACK FLASH COMBO — swap then hammer
                if (!pay(p, d, 45)) return;
                LivingEntity t = CombatUtil.raycastLiving(p, 20);
                if (t == null) { msg(p, "\u00a7cNo target."); return; }
                Vec3 theirs = t.position();
                CombatUtil.tp(p, theirs);
                domainText(p, "\u00a7c\u00a7lTHE STRONGEST DUO");
                CombatUtil.sound(level, theirs, S_ROAR, 1.0f, 1.2f);
                for (int i = 0; i < 4; i++) {
                    JJKMod.schedule(i * 5, () -> {
                        if (t.isAlive()) {
                            CombatUtil.physicalDamage(level, p, t, 8f);
                            CombatUtil.burst(level, ParticleTypes.CRIT, t.position(), 16, 0.4);
                            CombatUtil.sound(level, t.position(), S_CRIT, 0.8f, 1.0f);
                        }
                    });
                }
            }
            case 3 -> { // SHOULDER CHARGE — cone slam
                if (!pay(p, d, 18)) return;
                CombatUtil.effect(p, SPEED, 60, 2);
                slashParticles(level, p, 6);
                CombatUtil.sound(level, p.position(), S_SWEEP, 1.0f, 0.8f);
                for (LivingEntity e : CombatUtil.cone(p, 6, 50)) {
                    CombatUtil.physicalDamage(level, p, e, 11f);
                    CombatUtil.knockAway(e, p.position(), 2.0, 0.5);
                }
            }
            case 4 -> { // SWAP GUARD — trade places to escape
                if (!pay(p, d, 16)) return;
                LivingEntity t = CombatUtil.raycastLiving(p, 25);
                if (t == null) { msg(p, "\u00a7cNo target."); return; }
                Vec3 mine = p.position();
                Vec3 theirs = t.position();
                CombatUtil.tp(p, theirs);
                CombatUtil.tp(t, mine);
                CombatUtil.effect(p, RESISTANCE, 80, 1);
                CombatUtil.burst(level, ParticleTypes.NOTE, theirs, 16, 0.5);
                CombatUtil.sound(level, theirs, S_PORTAL, 0.8f, 1.7f);
            }
        }
    }

    /* ============================ HAKARI ============================ */
    private static void hakari(ServerPlayer p, ServerLevel level, CursedEnergy.Data d, int slot) {
        switch (slot) {
            case 0 -> { // SPIN — melee with a chance to crit big
                if (!pay(p, d, 12)) return;
                LivingEntity t = CombatUtil.raycastLiving(p, 5);
                if (t == null) { msg(p, "\u00a7cNo target."); return; }
                boolean lucky = Math.random() < 0.25;
                float dmg = lucky ? 24f : 9f;
                if (lucky) {
                    domainText(p, "\u00a76\u00a7lJACKPOT!");
                    CombatUtil.sound(level, t.position(), S_TOTEM, 1.0f, 1.7f);
                    CombatUtil.burst(level, ParticleTypes.HAPPY_VILLAGER, t.position(), 30, 0.6);
                }
                CombatUtil.physicalDamage(level, p, t, dmg);
                CombatUtil.burst(level, ParticleTypes.CRIT, t.position(), lucky ? 30 : 12, 0.4);
            }
            case 1 -> { // PAYOUT — gamble CE for a random buff
                if (!pay(p, d, 25)) return;
                double roll = Math.random();
                if (roll < 0.34) {
                    CombatUtil.effect(p, REGEN, 160, 2);
                    msg(p, "\u00a7aPayout: \u00a7fRegeneration III");
                } else if (roll < 0.67) {
                    CombatUtil.effect(p, STRENGTH, 160, 2);
                    msg(p, "\u00a7aPayout: \u00a7fStrength III");
                } else {
                    CombatUtil.effect(p, RESISTANCE, 160, 2);
                    CombatUtil.effect(p, ABSORPTION, 200, 2);
                    msg(p, "\u00a7aPayout: \u00a7fResistance + Absorption");
                }
                CombatUtil.spiral(level, ParticleTypes.HAPPY_VILLAGER, p.position(), 1.0, 2.2, 30);
                CombatUtil.sound(level, p.position(), S_TOTEM, 0.9f, 1.3f);
            }
            case 2 -> { // IDLE DEATH GAMBLE — huge sustain window
                if (!pay(p, d, 80)) return;
                domainText(p, "\u00a76\u00a7lIDLE DEATH GAMBLE");
                CombatUtil.effect(p, REGEN, 240, 3);
                CombatUtil.effect(p, RESISTANCE, 240, 2);
                CombatUtil.effect(p, STRENGTH, 240, 2);
                CombatUtil.effect(p, ABSORPTION, 300, 3);
                CombatUtil.spiral(level, ParticleTypes.HAPPY_VILLAGER, p.position(), 2.0, 4.0, 70);
                CombatUtil.ring(level, ParticleTypes.END_ROD, p.position(), 5, 40);
                CombatUtil.sound(level, p.position(), S_TOTEM, 1.0f, 1.5f);
            }
            case 3 -> { // ALL IN — hurt yourself for a burst of damage
                if (!pay(p, d, 20)) return;
                CombatUtil.effect(p, STRENGTH, 100, 2);
                CombatUtil.sound(level, p.position(), S_EXPLODE, 0.9f, 1.4f);
                for (LivingEntity e : CombatUtil.around(level, p, p.position(), 5)) {
                    CombatUtil.magicDamage(level, p, e, 13f);
                    CombatUtil.knockAway(e, p.position(), 1.5, 0.5);
                }
                CombatUtil.ring(level, ParticleTypes.EXPLOSION, p.position(), 4, 26);
            }
            case 4 -> { // REROLL — cleanse and re-buff
                if (!pay(p, d, 18)) return;
                CombatUtil.effect(p, REGEN, 120, 1);
                CombatUtil.effect(p, SPEED, 120, 1);
                CombatUtil.effect(p, HASTE, 120, 1);
                CombatUtil.ring(level, ParticleTypes.HAPPY_VILLAGER, p.position(), 1.8, 18);
                CombatUtil.sound(level, p.position(), S_CAST, 0.8f, 1.6f);
            }
        }
    }

    /* ============================ KASHIMO ============================ */
    private static void kashimo(ServerPlayer p, ServerLevel level, CursedEnergy.Data d, int slot) {
        switch (slot) {
            case 0 -> { // ARC — short lightning jab
                if (!pay(p, d, 12)) return;
                LivingEntity t = CombatUtil.raycastLiving(p, 6);
                if (t == null) { msg(p, "\u00a7cNo target."); return; }
                CombatUtil.magicDamage(level, p, t, 11f);
                CombatUtil.burst(level, ParticleTypes.ELECTRIC_SPARK, t.position(), 20, 0.4);
                CombatUtil.sound(level, t.position(), S_CRIT, 0.9f, 1.6f);
            }
            case 1 -> { // CHAIN LIGHTNING — jumps between nearby foes
                if (!pay(p, d, 24)) return;
                LivingEntity t = CombatUtil.raycastLiving(p, 20);
                if (t == null) { msg(p, "\u00a7cNo target."); return; }
                CombatUtil.magicDamage(level, p, t, 12f);
                CombatUtil.burst(level, ParticleTypes.ELECTRIC_SPARK, t.position(), 24, 0.5);
                for (LivingEntity e : CombatUtil.around(level, p, t.position(), 5)) {
                    if (e == t) continue;
                    CombatUtil.magicDamage(level, p, e, 7f);
                    CombatUtil.burst(level, ParticleTypes.ELECTRIC_SPARK, e.position(), 12, 0.4);
                }
                CombatUtil.sound(level, t.position(), S_THUNDER, 0.8f, 1.5f);
            }
            case 2 -> { // MYTHICAL BEAST AMBER — storm around you
                if (!pay(p, d, 80)) return;
                domainText(p, "\u00a7b\u00a7lMYTHICAL BEAST AMBER");
                Vec3 base = p.position();
                CombatUtil.effect(p, SPEED, 200, 2);
                CombatUtil.sound(level, base, S_THUNDER, 1.0f, 0.9f);
                for (int i = 0; i < 5; i++) {
                    final int w = i;
                    JJKMod.schedule(w * 8, () -> {
                        CombatUtil.ring(level, ParticleTypes.ELECTRIC_SPARK, p.position(), 6, 40);
                        for (LivingEntity e : CombatUtil.around(level, p, p.position(), 7)) {
                            CombatUtil.magicDamage(level, p, e, 8f);
                        }
                    });
                }
            }
            case 3 -> { // BOLT — call lightning on your aim point
                if (!pay(p, d, 26)) return;
                LivingEntity t = CombatUtil.raycastLiving(p, 26);
                Vec3 strike = (t != null) ? t.position()
                        : p.getEyePosition().add(p.getViewVector(1f).scale(18));
                CombatUtil.lightning(level, strike);
                CombatUtil.sound(level, strike, S_THUNDER, 1.0f, 1.1f);
                for (LivingEntity e : CombatUtil.around(level, p, strike, 4)) {
                    CombatUtil.magicDamage(level, p, e, 12f);
                }
            }
            case 4 -> { // DISCHARGE — burst outward
                if (!pay(p, d, 20)) return;
                CombatUtil.ring(level, ParticleTypes.ELECTRIC_SPARK, p.position(), 4.5, 34);
                CombatUtil.sound(level, p.position(), S_BOOM, 0.8f, 1.7f);
                for (LivingEntity e : CombatUtil.around(level, p, p.position(), 5)) {
                    CombatUtil.magicDamage(level, p, e, 10f);
                    CombatUtil.knockAway(e, p.position(), 1.8, 0.5);
                }
            }
        }
    }

    /* ============================ URAUME ============================ */
    private static void uraume(ServerPlayer p, ServerLevel level, CursedEnergy.Data d, int slot) {
        switch (slot) {
            case 0 -> { // ICE SHARD
                if (!pay(p, d, 12)) return;
                Vec3 eye = p.getEyePosition();
                Vec3 look = p.getViewVector(1f).normalize();
                CombatUtil.beam(level, ParticleTypes.SNOWFLAKE, eye, look, 20, 3);
                CombatUtil.sound(level, eye, S_SHOOT, 0.8f, 1.7f);
                LivingEntity t = CombatUtil.raycastLiving(p, 20);
                if (t == null) { msg(p, "\u00a7cNo target."); return; }
                CombatUtil.magicDamage(level, p, t, 10f);
                CombatUtil.effect(t, SLOWNESS, 80, 1);
                t.setTicksFrozen(160);
            }
            case 1 -> { // FROST FIELD — freeze everything close
                if (!pay(p, d, 22)) return;
                CombatUtil.ring(level, ParticleTypes.SNOWFLAKE, p.position(), 5, 36);
                CombatUtil.sound(level, p.position(), S_CAST, 0.9f, 1.6f);
                for (LivingEntity e : CombatUtil.around(level, p, p.position(), 5)) {
                    CombatUtil.magicDamage(level, p, e, 7f);
                    CombatUtil.effect(e, SLOWNESS, 140, 2);
                    e.setTicksFrozen(220);
                }
            }
            case 2 -> { // ICE FORMATION — the big freeze
                if (!pay(p, d, 75)) return;
                domainText(p, "\u00a7b\u00a7lICE FORMATION");
                Vec3 base = p.position();
                CombatUtil.ring(level, ParticleTypes.SNOWFLAKE, base, 8, 60);
                CombatUtil.spiral(level, ParticleTypes.ITEM_SNOWBALL, base, 1.8, 3.5, 50);
                CombatUtil.sound(level, base, S_ROAR, 1.0f, 1.6f);
                for (LivingEntity e : CombatUtil.around(level, p, base, 9)) {
                    CombatUtil.magicDamage(level, p, e, 15f);
                    CombatUtil.effect(e, SLOWNESS, 200, 3);
                    CombatUtil.effect(e, WEAKNESS, 200, 1);
                    e.setTicksFrozen(400);
                }
            }
            case 3 -> { // ICICLE VOLLEY — cone of shards
                if (!pay(p, d, 20)) return;
                CombatUtil.sound(level, p.position(), S_SHOOT, 0.9f, 1.4f);
                for (int i = 0; i < 8; i++) {
                    Vec3 dir = p.getViewVector(1f).add(
                            (Math.random() - 0.5) * 0.4, (Math.random() - 0.5) * 0.4,
                            (Math.random() - 0.5) * 0.4);
                    CombatUtil.beam(level, ParticleTypes.SNOWFLAKE, p.getEyePosition(), dir, 10, 2);
                }
                for (LivingEntity e : CombatUtil.cone(p, 10, 35)) {
                    CombatUtil.magicDamage(level, p, e, 9f);
                    e.setTicksFrozen(140);
                }
            }
            case 4 -> { // FROZEN GUARD — armour of ice
                if (!pay(p, d, 20)) return;
                CombatUtil.effect(p, RESISTANCE, 160, 2);
                CombatUtil.effect(p, ABSORPTION, 200, 1);
                CombatUtil.effect(p, FIRE_RES, 200, 0);
                CombatUtil.ring(level, ParticleTypes.SNOWFLAKE, p.position(), 1.8, 20);
                CombatUtil.sound(level, p.position(), S_ANVIL, 0.7f, 1.8f);
            }
        }
    }

    /* ============================ HIGURUMA ============================ */
    private static void higuruma(ServerPlayer p, ServerLevel level, CursedEnergy.Data d, int slot) {
        float w = CursedWeapons.bonus(p);
        switch (slot) {
            case 0 -> { // GAVEL STRIKE
                if (!pay(p, d, 14)) return;
                LivingEntity t = CombatUtil.raycastLiving(p, 5);
                if (t == null) { msg(p, "\u00a7cNo target."); return; }
                CombatUtil.physicalDamage(level, p, t, 12f + w);
                CombatUtil.burst(level, ParticleTypes.SOUL_FIRE_FLAME, t.position(), 16, 0.4);
                CombatUtil.sound(level, t.position(), S_ANVIL, 0.9f, 1.0f);
            }
            case 1 -> { // EXECUTION — scales with how hurt the target already is
                if (!pay(p, d, 26)) return;
                LivingEntity t = CombatUtil.raycastLiving(p, 6);
                if (t == null) { msg(p, "\u00a7cNo target."); return; }
                float missing = t.getMaxHealth() - t.getHealth();
                float dmg = 10f + w + Math.min(20f, missing * 0.5f);
                CombatUtil.physicalDamage(level, p, t, dmg);
                CombatUtil.burst(level, ParticleTypes.SOUL, t.position(), 26, 0.5);
                CombatUtil.sound(level, t.position(), S_CRIT, 1.0f, 0.6f);
                domainText(p, "\u00a74\u00a7lEXECUTION");
            }
            case 2 -> { // DEATH SENTENCE — judgement on everything in front
                if (!pay(p, d, 70)) return;
                domainText(p, "\u00a74\u00a7lDEADLY SENTENCING");
                CombatUtil.sound(level, p.position(), S_ROAR, 1.0f, 0.8f);
                CombatUtil.ring(level, ParticleTypes.SOUL_FIRE_FLAME, p.position(), 7, 50);
                for (LivingEntity e : CombatUtil.cone(p, 9, 70)) {
                    float missing = e.getMaxHealth() - e.getHealth();
                    CombatUtil.magicDamage(level, p, e, 14f + Math.min(16f, missing * 0.4f));
                    CombatUtil.effect(e, WEAKNESS, 160, 2);
                    CombatUtil.effect(e, SLOWNESS, 160, 1);
                }
            }
            case 3 -> { // CONFISCATION — strip the target's strength
                if (!pay(p, d, 22)) return;
                LivingEntity t = CombatUtil.raycastLiving(p, 14);
                if (t == null) { msg(p, "\u00a7cNo target."); return; }
                CombatUtil.effect(t, WEAKNESS, 200, 3);
                CombatUtil.effect(t, SLOWNESS, 200, 2);
                CombatUtil.effect(t, GLOWING, 200, 0);
                CombatUtil.magicDamage(level, p, t, 6f);
                CombatUtil.burst(level, ParticleTypes.SOUL, t.position(), 20, 0.4);
                CombatUtil.sound(level, t.position(), S_WITHER, 0.8f, 1.2f);
            }
            case 4 -> { // CLOSING ARGUMENT — wide cleave
                if (!pay(p, d, 18)) return;
                slashParticles(level, p, 8);
                CombatUtil.sound(level, p.position(), S_SWEEP, 1.0f, 0.7f);
                for (LivingEntity e : CombatUtil.cone(p, 7, 60)) {
                    CombatUtil.physicalDamage(level, p, e, 11f + w);
                    CombatUtil.knockAway(e, p.position(), 1.2, 0.35);
                }
            }
        }
    }

    /* ============================ YUKI ============================ */
    private static void yuki(ServerPlayer p, ServerLevel level, CursedEnergy.Data d, int slot) {
        switch (slot) {
            case 0 -> { // MASS PUNCH — adds weight to the hit
                if (!pay(p, d, 15)) return;
                LivingEntity t = CombatUtil.raycastLiving(p, 5);
                if (t == null) { msg(p, "\u00a7cNo target."); return; }
                CombatUtil.physicalDamage(level, p, t, 15f);
                CombatUtil.knockAway(t, p.position(), 2.4, 0.6);
                CombatUtil.burst(level, ParticleTypes.EXPLOSION, t.position(), 5, 0.4);
                CombatUtil.sound(level, t.position(), S_ANVIL, 1.0f, 0.7f);
            }
            case 1 -> { // GARUDA — send a heavy shikigami blast
                if (!pay(p, d, 28)) return;
                Vec3 eye = p.getEyePosition();
                Vec3 look = p.getViewVector(1f).normalize();
                CombatUtil.beam(level, ParticleTypes.END_ROD, eye, look, 24, 3);
                CombatUtil.sound(level, eye, S_EXPLODE, 0.9f, 1.0f);
                for (int i = 1; i <= 24; i++) {
                    Vec3 point = eye.add(look.scale(i));
                    for (LivingEntity e : CombatUtil.around(level, p, point, 2.0)) {
                        CombatUtil.magicDamage(level, p, e, 13f);
                        CombatUtil.knockAway(e, eye, 1.4, 0.4);
                    }
                }
            }
            case 2 -> { // STAR RAGE — bomb core detonation
                if (!pay(p, d, 85)) return;
                domainText(p, "\u00a7d\u00a7lSTAR RAGE");
                Vec3 aim = p.getEyePosition().add(p.getViewVector(1f).scale(7));
                CombatUtil.burst(level, ParticleTypes.EXPLOSION, aim, 14, 1.2);
                CombatUtil.ring(level, ParticleTypes.END_ROD, aim, 7, 50);
                CombatUtil.sound(level, aim, S_EXPLODE, 1.0f, 0.5f);
                for (LivingEntity e : CombatUtil.around(level, p, aim, 8)) {
                    CombatUtil.magicDamage(level, p, e, 20f);
                    CombatUtil.knockAway(e, aim, 2.4, 0.8);
                }
            }
            case 3 -> { // GRAVITY SLAM — crush everything around
                if (!pay(p, d, 24)) return;
                CombatUtil.ring(level, ParticleTypes.EXPLOSION, p.position(), 4, 26);
                CombatUtil.sound(level, p.position(), S_ANVIL, 1.0f, 0.6f);
                for (LivingEntity e : CombatUtil.around(level, p, p.position(), 5)) {
                    CombatUtil.magicDamage(level, p, e, 13f);
                    CombatUtil.effect(e, SLOWNESS, 100, 2);
                }
            }
            case 4 -> { // LIGHTEN — drop your own mass for speed
                if (!pay(p, d, 16)) return;
                CombatUtil.effect(p, SPEED, 160, 2);
                CombatUtil.effect(p, JUMP, 160, 3);
                CombatUtil.spiral(level, ParticleTypes.END_ROD, p.position(), 0.9, 2.0, 26);
                CombatUtil.sound(level, p.position(), S_TOTEM, 0.7f, 1.8f);
            }
        }
    }

    /* ============================ KENJAKU ============================ */
    private static void kenjaku(ServerPlayer p, ServerLevel level, CursedEnergy.Data d, int slot) {
        switch (slot) {
            case 0 -> { // STOLEN TECHNIQUE — random element each cast
                if (!pay(p, d, 18)) return;
                LivingEntity t = CombatUtil.raycastLiving(p, 16);
                if (t == null) { msg(p, "\u00a7cNo target."); return; }
                double roll = Math.random();
                if (roll < 0.34) {
                    CombatUtil.magicDamage(level, p, t, 12f);
                    CombatUtil.effect(t, WITHER, 80, 0);
                    CombatUtil.burst(level, ParticleTypes.SOUL, t.position(), 18, 0.4);
                } else if (roll < 0.67) {
                    CombatUtil.magicDamage(level, p, t, 11f);
                    CombatUtil.effect(t, SLOWNESS, 100, 2);
                    CombatUtil.burst(level, ParticleTypes.SNOWFLAKE, t.position(), 18, 0.4);
                } else {
                    CombatUtil.magicDamage(level, p, t, 13f);
                    CombatUtil.burst(level, ParticleTypes.FLAME, t.position(), 18, 0.4);
                }
                CombatUtil.sound(level, t.position(), S_CAST, 0.9f, 1.0f);
            }
            case 1 -> { // CURSE WOMB — summon two servants
                if (!pay(p, d, 35)) return;
                Vec3 base = p.position();
                for (int i = 0; i < 2; i++) {
                    double a = Math.PI * i;
                    Vec3 at = base.add(Math.cos(a) * 2, 0, Math.sin(a) * 2);
                    CombatUtil.summonWolf(level, p, at, null);
                    CombatUtil.burst(level, ParticleTypes.SCULK_SOUL, at, 18, 0.5);
                }
                CombatUtil.sound(level, base, S_CAST, 0.9f, 0.6f);
            }
            case 2 -> { // ANTI-GRAVITY — the grand plan
                if (!pay(p, d, 85)) return;
                domainText(p, "\u00a72\u00a7lTHOUSAND YEAR PLAN");
                Vec3 base = p.position();
                CombatUtil.ring(level, ParticleTypes.SCULK_SOUL, base, 8, 56);
                CombatUtil.spiral(level, ParticleTypes.SOUL, base, 2.0, 4.0, 66);
                CombatUtil.sound(level, base, S_ROAR, 1.0f, 1.0f);
                for (LivingEntity e : CombatUtil.around(level, p, base, 9)) {
                    CombatUtil.magicDamage(level, p, e, 16f);
                    CombatUtil.effect(e, LEVITATION, 60, 2);
                    CombatUtil.effect(e, WITHER, 120, 1);
                    CombatUtil.effect(e, BLINDNESS, 100, 0);
                }
            }
            case 3 -> { // BODY HOP — reposition behind your aim
                if (!pay(p, d, 20)) return;
                Vec3 dest = p.getEyePosition().add(p.getViewVector(1f).scale(10));
                CombatUtil.burst(level, ParticleTypes.SCULK_SOUL, p.position(), 20, 0.5);
                CombatUtil.tp(p, dest);
                CombatUtil.burst(level, ParticleTypes.SCULK_SOUL, dest, 20, 0.5);
                CombatUtil.sound(level, dest, S_PORTAL, 0.9f, 0.8f);
            }
            case 4 -> { // MALEVOLENT SHRINE MIMIC — borrowed slashes
                if (!pay(p, d, 30)) return;
                Vec3 center = p.position();
                for (int wave = 0; wave < 3; wave++) {
                    final int w = wave;
                    JJKMod.schedule(w * 10, () -> {
                        slashParticles(level, p, 10);
                        CombatUtil.ring(level, ParticleTypes.SWEEP_ATTACK, center, 5, 20);
                        for (LivingEntity e : CombatUtil.around(level, p, center, 6)) {
                            CombatUtil.magicDamage(level, p, e, 7f);
                        }
                    });
                }
                CombatUtil.sound(level, center, S_SWEEP, 0.9f, 0.7f);
            }
        }
    }

    /* ============================ INUMAKI ============================ */
    private static void inumaki(ServerPlayer p, ServerLevel level, CursedEnergy.Data d, int slot) {
        switch (slot) {
            case 0 -> { // "BLAST AWAY" — forceful shout
                if (!pay(p, d, 16)) return;
                domainText(p, "\u00a7e\u00a7l\"BLAST AWAY\"");
                CombatUtil.ring(level, ParticleTypes.NOTE, p.position(), 4, 28);
                CombatUtil.sound(level, p.position(), S_BOOM, 0.9f, 1.4f);
                for (LivingEntity e : CombatUtil.cone(p, 8, 55)) {
                    CombatUtil.magicDamage(level, p, e, 10f);
                    CombatUtil.knockAway(e, p.position(), 2.6, 0.7);
                }
            }
            case 1 -> { // "DON'T MOVE" — root the target
                if (!pay(p, d, 18)) return;
                LivingEntity t = CombatUtil.raycastLiving(p, 18);
                if (t == null) { msg(p, "\u00a7cNo target."); return; }
                domainText(p, "\u00a7e\u00a7l\"DON'T MOVE\"");
                CombatUtil.effect(t, SLOWNESS, 140, 5);
                CombatUtil.effect(t, WEAKNESS, 140, 2);
                CombatUtil.effect(t, GLOWING, 140, 0);
                CombatUtil.burst(level, ParticleTypes.NOTE, t.position(), 20, 0.4);
                CombatUtil.sound(level, t.position(), S_CAST, 0.9f, 1.2f);
            }
            case 2 -> { // "EXPLODE" — the throat-shredder
                if (!pay(p, d, 70)) return;
                domainText(p, "\u00a7e\u00a7l\"EXPLODE\"");
                LivingEntity t = CombatUtil.raycastLiving(p, 20);
                Vec3 aim = (t != null) ? t.position()
                        : p.getEyePosition().add(p.getViewVector(1f).scale(8));
                CombatUtil.burst(level, ParticleTypes.EXPLOSION, aim, 12, 1.0);
                CombatUtil.ring(level, ParticleTypes.NOTE, aim, 6, 44);
                CombatUtil.sound(level, aim, S_EXPLODE, 1.0f, 0.8f);
                for (LivingEntity e : CombatUtil.around(level, p, aim, 7)) {
                    CombatUtil.magicDamage(level, p, e, 19f);
                    CombatUtil.knockAway(e, aim, 1.8, 0.6);
                }
                // using cursed speech costs the user their voice
                CombatUtil.effect(p, WEAKNESS, 120, 0);
            }
            case 3 -> { // "SLEEP" — disorient everything nearby
                if (!pay(p, d, 22)) return;
                domainText(p, "\u00a7e\u00a7l\"SLEEP\"");
                CombatUtil.ring(level, ParticleTypes.NOTE, p.position(), 5, 34);
                CombatUtil.sound(level, p.position(), S_CAST, 0.8f, 0.7f);
                for (LivingEntity e : CombatUtil.around(level, p, p.position(), 6)) {
                    CombatUtil.effect(e, BLINDNESS, 120, 0);
                    CombatUtil.effect(e, NAUSEA, 120, 0);
                    CombatUtil.effect(e, SLOWNESS, 120, 2);
                }
            }
            case 4 -> { // "GET CRUSHED" — slam a single target down
                if (!pay(p, d, 20)) return;
                LivingEntity t = CombatUtil.raycastLiving(p, 18);
                if (t == null) { msg(p, "\u00a7cNo target."); return; }
                domainText(p, "\u00a7e\u00a7l\"GET CRUSHED\"");
                CombatUtil.magicDamage(level, p, t, 14f);
                t.setDeltaMovement(0, -1.6, 0);
                t.hurtMarked = true;
                CombatUtil.burst(level, ParticleTypes.NOTE, t.position(), 22, 0.5);
                CombatUtil.sound(level, t.position(), S_ANVIL, 1.0f, 0.8f);
            }
        }
    }


    /* ============================ JOGO ============================ */
    private static void jogo(ServerPlayer p, ServerLevel level, CursedEnergy.Data d, int slot) {
        switch (slot) {
            case 0 -> { // EMBER — quick fireball
                if (!pay(p, d, 13)) return;
                Vec3 eye = p.getEyePosition();
                Vec3 look = p.getViewVector(1f).normalize();
                CombatUtil.beam(level, ParticleTypes.FLAME, eye, look, 20, 3);
                CombatUtil.sound(level, eye, S_SHOOT, 0.9f, 1.0f);
                LivingEntity t = CombatUtil.raycastLiving(p, 20);
                if (t == null) { msg(p, "\u00a7cNo target."); return; }
                CombatUtil.magicDamage(level, p, t, 11f);
                t.setRemainingFireTicks(120);
                CombatUtil.burst(level, ParticleTypes.LAVA, t.position(), 14, 0.4);
            }
            case 1 -> { // ERUPTION — the ground bursts under your aim
                if (!pay(p, d, 26)) return;
                LivingEntity t = CombatUtil.raycastLiving(p, 22);
                Vec3 aim = (t != null) ? t.position()
                        : p.getEyePosition().add(p.getViewVector(1f).scale(10));
                CombatUtil.burst(level, ParticleTypes.EXPLOSION, aim, 8, 0.8);
                CombatUtil.ring(level, ParticleTypes.LAVA, aim, 4, 28);
                CombatUtil.sound(level, aim, S_EXPLODE, 1.0f, 0.8f);
                for (LivingEntity e : CombatUtil.around(level, p, aim, 5)) {
                    CombatUtil.magicDamage(level, p, e, 13f);
                    e.setRemainingFireTicks(140);
                    CombatUtil.knockAway(e, aim, 1.2, 0.6);
                }
            }
            case 2 -> { // MAXIMUM: METEOR
                if (!pay(p, d, 85)) return;
                domainText(p, "\u00a76\u00a7lMAXIMUM \u2014 METEOR");
                LivingEntity t = CombatUtil.raycastLiving(p, 26);
                Vec3 aim = (t != null) ? t.position()
                        : p.getEyePosition().add(p.getViewVector(1f).scale(12));
                CombatUtil.sound(level, aim, S_ROAR, 1.0f, 0.6f);
                for (int i = 0; i < 4; i++) {
                    final int w = i;
                    JJKMod.schedule(w * 7, () -> {
                        CombatUtil.burst(level, ParticleTypes.EXPLOSION, aim, 12, 1.2);
                        CombatUtil.ring(level, ParticleTypes.FLAME, aim, 7, 44);
                        for (LivingEntity e : CombatUtil.around(level, p, aim, 8)) {
                            CombatUtil.magicDamage(level, p, e, 12f);
                            e.setRemainingFireTicks(200);
                        }
                    });
                }
            }
            case 3 -> { // FLAME CONE
                if (!pay(p, d, 20)) return;
                CombatUtil.sound(level, p.position(), S_SHOOT, 1.0f, 0.7f);
                for (int i = 0; i < 8; i++) {
                    Vec3 dir = p.getViewVector(1f).add(
                            (Math.random() - 0.5) * 0.45, (Math.random() - 0.5) * 0.45,
                            (Math.random() - 0.5) * 0.45);
                    CombatUtil.beam(level, ParticleTypes.FLAME, p.getEyePosition(), dir, 9, 2);
                }
                for (LivingEntity e : CombatUtil.cone(p, 9, 40)) {
                    CombatUtil.magicDamage(level, p, e, 10f);
                    e.setRemainingFireTicks(120);
                }
            }
            case 4 -> { // MOLTEN SHELL — burn anyone who closes in
                if (!pay(p, d, 18)) return;
                CombatUtil.effect(p, FIRE_RES, 200, 0);
                CombatUtil.effect(p, RESISTANCE, 140, 1);
                CombatUtil.ring(level, ParticleTypes.FLAME, p.position(), 2.5, 22);
                CombatUtil.sound(level, p.position(), S_TOTEM, 0.8f, 0.8f);
                for (LivingEntity e : CombatUtil.around(level, p, p.position(), 3.5)) {
                    e.setRemainingFireTicks(100);
                    CombatUtil.magicDamage(level, p, e, 6f);
                }
            }
        }
    }

    /* ============================ HANAMI ============================ */
    private static void hanami(ServerPlayer p, ServerLevel level, CursedEnergy.Data d, int slot) {
        switch (slot) {
            case 0 -> { // BRANCH LASH
                if (!pay(p, d, 12)) return;
                LivingEntity t = CombatUtil.raycastLiving(p, 7);
                if (t == null) { msg(p, "\u00a7cNo target."); return; }
                CombatUtil.magicDamage(level, p, t, 11f);
                CombatUtil.effect(t, SLOWNESS, 80, 1);
                CombatUtil.burst(level, ParticleTypes.COMPOSTER, t.position(), 18, 0.4);
                CombatUtil.sound(level, t.position(), S_SWEEP, 0.8f, 0.9f);
            }
            case 1 -> { // ENTANGLE — root and drain a target to heal yourself
                if (!pay(p, d, 24)) return;
                LivingEntity t = CombatUtil.raycastLiving(p, 16);
                if (t == null) { msg(p, "\u00a7cNo target."); return; }
                CombatUtil.effect(t, SLOWNESS, 160, 4);
                CombatUtil.effect(t, WEAKNESS, 160, 1);
                CombatUtil.effect(p, REGEN, 120, 1);
                CombatUtil.burst(level, ParticleTypes.COMPOSTER, t.position(), 26, 0.5);
                CombatUtil.sound(level, t.position(), S_CAST, 0.9f, 0.6f);
                for (int i = 1; i <= 4; i++) {
                    JJKMod.schedule(i * 15, () -> {
                        if (t.isAlive()) {
                            CombatUtil.magicDamage(level, p, t, 4f);
                            CombatUtil.burst(level, ParticleTypes.COMPOSTER, t.position(), 8, 0.3);
                        }
                    });
                }
            }
            case 2 -> { // WOODEN CORE — grove erupts around you
                if (!pay(p, d, 78)) return;
                domainText(p, "\u00a72\u00a7lWOODEN CORE");
                Vec3 base = p.position();
                CombatUtil.ring(level, ParticleTypes.COMPOSTER, base, 8, 56);
                CombatUtil.spiral(level, ParticleTypes.HAPPY_VILLAGER, base, 1.8, 3.5, 50);
                CombatUtil.sound(level, base, S_ROAR, 1.0f, 0.5f);
                CombatUtil.effect(p, REGEN, 200, 2);
                CombatUtil.effect(p, RESISTANCE, 200, 2);
                for (LivingEntity e : CombatUtil.around(level, p, base, 9)) {
                    CombatUtil.magicDamage(level, p, e, 15f);
                    CombatUtil.effect(e, SLOWNESS, 200, 3);
                    CombatUtil.effect(e, POISON, 160, 1);
                }
            }
            case 3 -> { // SEED BURST — ranged pod that poisons
                if (!pay(p, d, 20)) return;
                LivingEntity t = CombatUtil.raycastLiving(p, 20);
                Vec3 aim = (t != null) ? t.position()
                        : p.getEyePosition().add(p.getViewVector(1f).scale(9));
                CombatUtil.burst(level, ParticleTypes.COMPOSTER, aim, 24, 0.7);
                CombatUtil.sound(level, aim, S_EXPLODE, 0.7f, 1.4f);
                for (LivingEntity e : CombatUtil.around(level, p, aim, 5)) {
                    CombatUtil.magicDamage(level, p, e, 10f);
                    CombatUtil.effect(e, POISON, 140, 1);
                }
            }
            case 4 -> { // BARK SKIN — heavy defensive layer
                if (!pay(p, d, 18)) return;
                CombatUtil.effect(p, RESISTANCE, 200, 2);
                CombatUtil.effect(p, ABSORPTION, 240, 2);
                CombatUtil.effect(p, REGEN, 120, 0);
                CombatUtil.ring(level, ParticleTypes.COMPOSTER, p.position(), 1.8, 18);
                CombatUtil.sound(level, p.position(), S_TOTEM, 0.8f, 0.6f);
            }
        }
    }

    /* ============================ DAGON ============================ */
    private static void dagon(ServerPlayer p, ServerLevel level, CursedEnergy.Data d, int slot) {
        switch (slot) {
            case 0 -> { // TIDE SLAP
                if (!pay(p, d, 12)) return;
                CombatUtil.ring(level, ParticleTypes.SPLASH, p.position(), 3.5, 26);
                CombatUtil.sound(level, p.position(), S_SWEEP, 0.9f, 1.2f);
                for (LivingEntity e : CombatUtil.cone(p, 6, 55)) {
                    CombatUtil.magicDamage(level, p, e, 10f);
                    CombatUtil.knockAway(e, p.position(), 1.8, 0.5);
                }
            }
            case 1 -> { // SHIKIGAMI SWARM — summon tide-beasts
                if (!pay(p, d, 32)) return;
                Vec3 base = p.position();
                for (int i = 0; i < 3; i++) {
                    double a = (Math.PI * 2 / 3) * i;
                    Vec3 at = base.add(Math.cos(a) * 2, 0, Math.sin(a) * 2);
                    CombatUtil.summonWolf(level, p, at, null);
                    CombatUtil.burst(level, ParticleTypes.SPLASH, at, 16, 0.5);
                }
                CombatUtil.sound(level, base, S_CAST, 0.9f, 1.1f);
            }
            case 2 -> { // DEATH SWARM — the full tide
                if (!pay(p, d, 82)) return;
                domainText(p, "\u00a73\u00a7lDEATH SWARM");
                Vec3 base = p.position();
                CombatUtil.ring(level, ParticleTypes.SPLASH, base, 8, 56);
                CombatUtil.sound(level, base, S_ROAR, 1.0f, 1.2f);
                for (int i = 0; i < 4; i++) {
                    final int w = i;
                    JJKMod.schedule(w * 8, () -> {
                        CombatUtil.ring(level, ParticleTypes.SPLASH, p.position(), 6, 40);
                        for (LivingEntity e : CombatUtil.around(level, p, p.position(), 8)) {
                            CombatUtil.magicDamage(level, p, e, 10f);
                            CombatUtil.knockAway(e, p.position(), 1.0, 0.4);
                        }
                    });
                }
            }
            case 3 -> { // UNDERTOW — drag everything toward you
                if (!pay(p, d, 22)) return;
                Vec3 base = p.position();
                CombatUtil.ring(level, ParticleTypes.SPLASH, base, 7, 44);
                CombatUtil.sound(level, base, S_PORTAL, 0.9f, 0.7f);
                for (LivingEntity e : CombatUtil.around(level, p, base, 8)) {
                    CombatUtil.pullToward(e, base, 1.5);
                    CombatUtil.magicDamage(level, p, e, 7f);
                    CombatUtil.effect(e, SLOWNESS, 80, 1);
                }
            }
            case 4 -> { // TIDE GUARD
                if (!pay(p, d, 18)) return;
                CombatUtil.effect(p, RESISTANCE, 160, 2);
                CombatUtil.effect(p, REGEN, 120, 1);
                CombatUtil.effect(p, SPEED, 160, 1);
                CombatUtil.ring(level, ParticleTypes.SPLASH, p.position(), 2.0, 20);
                CombatUtil.sound(level, p.position(), S_TOTEM, 0.8f, 1.1f);
            }
        }
    }

    /* ============================ YOROZU ============================ */
    private static void yorozu(ServerPlayer p, ServerLevel level, CursedEnergy.Data d, int slot) {
        switch (slot) {
            case 0 -> { // FORGED SPIKE
                if (!pay(p, d, 13)) return;
                LivingEntity t = CombatUtil.raycastLiving(p, 6);
                if (t == null) { msg(p, "\u00a7cNo target."); return; }
                CombatUtil.physicalDamage(level, p, t, 13f);
                CombatUtil.burst(level, ParticleTypes.CRIT, t.position(), 18, 0.4);
                CombatUtil.sound(level, t.position(), S_ANVIL, 0.9f, 1.3f);
            }
            case 1 -> { // MOLTEN LANCE — long metal spear
                if (!pay(p, d, 24)) return;
                Vec3 eye = p.getEyePosition();
                Vec3 look = p.getViewVector(1f).normalize();
                CombatUtil.beam(level, ParticleTypes.CRIT, eye, look, 22, 4);
                CombatUtil.beam(level, ParticleTypes.LAVA, eye, look, 22, 2);
                CombatUtil.sound(level, eye, S_SHOOT, 0.9f, 0.9f);
                for (int i = 1; i <= 22; i++) {
                    Vec3 point = eye.add(look.scale(i));
                    for (LivingEntity e : CombatUtil.around(level, p, point, 1.8)) {
                        CombatUtil.magicDamage(level, p, e, 12f);
                    }
                }
            }
            case 2 -> { // PERFECT CONSTRUCTION — a field of blades
                if (!pay(p, d, 80)) return;
                domainText(p, "\u00a7d\u00a7lPERFECT CONSTRUCTION");
                Vec3 base = p.position();
                CombatUtil.ring(level, ParticleTypes.CRIT, base, 8, 56);
                CombatUtil.spiral(level, ParticleTypes.END_ROD, base, 1.8, 3.5, 50);
                CombatUtil.sound(level, base, S_ANVIL, 1.0f, 0.5f);
                for (int i = 0; i < 3; i++) {
                    final int w = i;
                    JJKMod.schedule(w * 10, () -> {
                        CombatUtil.ring(level, ParticleTypes.SWEEP_ATTACK, p.position(), 6, 30);
                        for (LivingEntity e : CombatUtil.around(level, p, p.position(), 8)) {
                            CombatUtil.magicDamage(level, p, e, 13f);
                            CombatUtil.knockAway(e, p.position(), 1.0, 0.4);
                        }
                    });
                }
            }
            case 3 -> { // SHRAPNEL — scatter of forged fragments
                if (!pay(p, d, 20)) return;
                CombatUtil.sound(level, p.position(), S_SHOOT, 1.0f, 1.5f);
                for (int i = 0; i < 10; i++) {
                    Vec3 dir = p.getViewVector(1f).add(
                            (Math.random() - 0.5) * 0.5, (Math.random() - 0.5) * 0.5,
                            (Math.random() - 0.5) * 0.5);
                    CombatUtil.beam(level, ParticleTypes.CRIT, p.getEyePosition(), dir, 8, 2);
                }
                for (LivingEntity e : CombatUtil.cone(p, 9, 35)) {
                    CombatUtil.magicDamage(level, p, e, 9f);
                    CombatUtil.knockAway(e, p.position(), 1.0, 0.3);
                }
            }
            case 4 -> { // STEEL PLATING
                if (!pay(p, d, 18)) return;
                CombatUtil.effect(p, RESISTANCE, 180, 2);
                CombatUtil.effect(p, ABSORPTION, 220, 2);
                CombatUtil.effect(p, HASTE, 180, 2);
                CombatUtil.ring(level, ParticleTypes.CRIT, p.position(), 1.8, 18);
                CombatUtil.sound(level, p.position(), S_ANVIL, 0.8f, 1.6f);
            }
        }
    }


    /* ======================= DOMAIN EXPANSION (key V) =======================
     * Only the characters who canonically have one. A domain is a barrier that
     * traps everything nearby and lands SURE HITS -- damage pulses that keep
     * landing for the duration, with a signature debuff per user.
     */
    private static final int DOMAIN_TICKS = 200;   // 10 seconds of pulses
    private static final int DOMAIN_COST  = 100;

    /** Which techniques actually have a domain. */
    public static boolean hasDomain(Technique t) {
        return switch (t) {
            case GOJO, SUKUNA, MEGUMI, MAHITO, JOGO, DAGON,
                 KENJAKU, HAKARI, HIGURUMA, YUTA -> true;
            default -> false;
        };
    }

    public static String domainName(Technique t) {
        return switch (t) {
            case GOJO     -> "UNLIMITED VOID";
            case SUKUNA   -> "MALEVOLENT SHRINE";
            case MEGUMI   -> "CHIMERA SHADOW GARDEN";
            case MAHITO   -> "SELF-EMBODIMENT OF PERFECTION";
            case JOGO     -> "COFFIN OF THE IRON MOUNTAIN";
            case DAGON    -> "HORIZON OF THE CAPTIVATING SKANDHA";
            case KENJAKU  -> "WOMB PROFUSION";
            case HAKARI   -> "IDLE DEATH GAMBLE";
            case HIGURUMA -> "DEADLY SENTENCING";
            case YUTA     -> "AUTHENTIC MUTUAL LOVE";
            default       -> "";
        };
    }

    private static void domain(ServerPlayer p, ServerLevel level, CursedEnergy.Data d) {
        Technique t = d.technique;
        if (!hasDomain(t)) {
            msg(p, "\u00a78" + t.display + " has no Domain Expansion.");
            castPaid = false;
            return;
        }
        if (!pay(p, d, DOMAIN_COST)) return;

        String name = domainName(t);
        p.displayClientMessage(Component.literal("\u00a75\u00a7lDOMAIN EXPANSION"), true);
        p.getServer().getPlayerList().broadcastSystemMessage(
                Component.literal("\u00a75\u00a7l\u2726 " + p.getName().getString()
                        + " \u2014 " + name + " \u2726"), false);

        Vec3 c = p.position();
        // barrier goes up
        CombatUtil.sound(level, c, S_ROAR, 1.2f, 0.5f);
        CombatUtil.sound(level, c, S_BOOM, 1.0f, 0.6f);
        for (double r = 2; r <= 10; r += 2) {
            CombatUtil.ring(level, ParticleTypes.SOUL_FIRE_FLAME, c, r, (int) (r * 8));
        }
        CombatUtil.spiral(level, ParticleTypes.END_ROD, c, 2.5, 6.0, 90);

        CombatUtil.effect(p, RESISTANCE, DOMAIN_TICKS, 2);   // you own the space

        // sure-hit pulses
        for (int i = 0; i < 10; i++) {
            final int w = i;
            JJKMod.schedule(w * 20, () -> {
                Vec3 at = p.position();
                domainPulse(p, level, t, at);
            });
        }
    }

    /** One sure-hit pulse, flavoured per domain. */
    private static void domainPulse(ServerPlayer p, ServerLevel level, Technique t, Vec3 at) {
        CombatUtil.ring(level, ParticleTypes.SOUL, at, 9, 56);
        for (LivingEntity e : CombatUtil.around(level, p, at, 10)) {
            switch (t) {
                case GOJO -> {   // infinite information -> total lockdown
                    CombatUtil.magicDamage(level, p, e, 9f);
                    CombatUtil.effect(e, SLOWNESS, 60, 4);
                    CombatUtil.effect(e, BLINDNESS, 60, 0);
                    CombatUtil.effect(e, NAUSEA, 60, 0);
                }
                case SUKUNA -> { // endless dismantle & cleave
                    CombatUtil.magicDamage(level, p, e, 14f);
                    CombatUtil.effect(e, WEAKNESS, 60, 1);
                }
                case MEGUMI -> {
                    CombatUtil.magicDamage(level, p, e, 8f);
                    CombatUtil.effect(e, DARKNESS, 80, 0);
                    CombatUtil.effect(e, SLOWNESS, 60, 2);
                }
                case MAHITO -> {
                    CombatUtil.magicDamage(level, p, e, 11f);
                    CombatUtil.effect(e, WITHER, 60, 1);
                    CombatUtil.effect(e, NAUSEA, 60, 0);
                }
                case JOGO -> {
                    CombatUtil.magicDamage(level, p, e, 12f);
                    e.setRemainingFireTicks(120);
                }
                case DAGON -> {
                    CombatUtil.magicDamage(level, p, e, 10f);
                    CombatUtil.pullToward(e, at, 0.8);
                    CombatUtil.effect(e, SLOWNESS, 60, 2);
                }
                case KENJAKU -> {
                    CombatUtil.magicDamage(level, p, e, 10f);
                    CombatUtil.effect(e, POISON, 80, 1);
                    CombatUtil.effect(e, WEAKNESS, 80, 1);
                }
                case HAKARI -> {
                    CombatUtil.magicDamage(level, p, e, 9f);
                    CombatUtil.effect(p, REGEN, 60, 2);      // the jackpot pays HIM
                    CombatUtil.effect(p, ABSORPTION, 100, 2);
                }
                case HIGURUMA -> {                            // executes the wounded
                    float missing = e.getMaxHealth() - e.getHealth();
                    CombatUtil.magicDamage(level, p, e, 9f + Math.min(14f, missing * 0.35f));
                    CombatUtil.effect(e, WEAKNESS, 80, 2);
                }
                case YUTA -> {
                    CombatUtil.magicDamage(level, p, e, 12f);
                    CombatUtil.effect(p, REGEN, 60, 1);
                }
                default -> CombatUtil.magicDamage(level, p, e, 8f);
            }
        }
    }

    /* ============================ helpers ============================ */
    private static boolean pay(ServerPlayer p, CursedEnergy.Data d, float cost) {
        if (d.awakened > 0) return true;       // free casts while awakened
        if (CursedEnergy.spend(d, cost)) return true;
        msg(p, "§9Not enough cursed energy.");
        castPaid = false;                      // earns no awakening
        return false;
    }

    private static void slashParticles(ServerLevel level, ServerPlayer p, int count) {
        Vec3 front = p.getEyePosition().add(p.getViewVector(1f).scale(2));
        CombatUtil.particles(level, ParticleTypes.SWEEP_ATTACK, front, count, 0.6, 0.02);
        CombatUtil.particles(level, ParticleTypes.CRIT, front, count * 2, 0.6, 0.05);
    }

    private static void domainText(ServerPlayer p, String text) {
        p.displayClientMessage(Component.literal(text), true);
    }

    private static void msg(ServerPlayer p, String text) {
        p.displayClientMessage(Component.literal(text), true);
    }
}
