package com.makeforge.jjk.ce;

import com.makeforge.jjk.technique.Technique;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Session-scoped cursed energy + technique selection.
 *
 * v2 adds the AWAKENING bar: a second resource that fills passively (+5 every
 * 10 ticks -> full in 10s), locks READY at 100, and is spent to enter a
 * character-specific "awakened" state for a short duration.
 *
 * Still NOT persisted to NBT (resets on relog) — keeps the build clean.
 */
public final class CursedEnergy {
    private CursedEnergy() {}

    // --- awakening tuning ---
    public static final float AWAKEN_MAX      = 100f; // secondary bar cap
    // v3 tuning: slow passive trickle, long payoff.
    public static final float AWAKEN_FILL     = 0.55f;// +0.55% every 50 ticks (+10%)
    public static final int   AWAKEN_TICKRATE = 50;   // ticks between trickles
    public static final int   AWAKEN_DURATION = 900;  // awakened state = 45s

    public static final class Data {
        public Technique technique = Technique.NONE;
        public float ce = Technique.NONE.maxCE;
        // used by Yuji's Black Flash timing window (ticks remaining)
        public int comboWindow = 0;

        // --- v2 awakening ---
        public float   awaken      = 0f;    // 0..AWAKEN_MAX (the purple bar)
        public boolean awakenReady = false; // true once full, until spent
        public int     awakened    = 0;     // ticks remaining of active state
    }

    private static final Map<UUID, Data> SERVER = new ConcurrentHashMap<>();
    private static final Map<UUID, Data> CLIENT = new ConcurrentHashMap<>();

    public static Data server(UUID id) { return SERVER.computeIfAbsent(id, k -> new Data()); }
    public static Data client(UUID id) { return CLIENT.computeIfAbsent(id, k -> new Data()); }

    public static void setTechnique(Data d, Technique t) {
        d.technique = t;
        d.ce = t.maxCE;
        d.comboWindow = 0;
        // reset awakening when swapping characters
        d.awaken = 0f;
        d.awakenReady = false;
        d.awakened = 0;
    }

    /** Regen tick, shared by both sides. Mutates in place. */
    public static void tick(Data d) {
        Technique t = d.technique;
        if (d.ce < t.maxCE) {
            d.ce = Math.min(t.maxCE, d.ce + t.regenPerTick);
        }
        if (d.comboWindow > 0) d.comboWindow--;
    }

    /** Try to spend cost. True if paid (or free). */
    public static boolean spend(Data d, float cost) {
        if (cost <= 0f) return true;           // Toji / free abilities
        if (d.ce < cost) return false;
        d.ce -= cost;
        return true;
    }

    public static void clearClient() { CLIENT.clear(); }

    /**
     * Add to the awakening bar (used by combat gain and the passive trickle).
     * No-op while already awakened; locks "ready" on reaching the cap.
     */
    public static void addAwaken(Data d, float amt) {
        if (d.awakened > 0) return;
        if (d.awakenReady) return;
        d.awaken = Math.min(AWAKEN_MAX, d.awaken + amt);
        if (d.awaken >= AWAKEN_MAX) {
            d.awaken = AWAKEN_MAX;
            d.awakenReady = true;
        }
    }

    /**
     * Wipe all server-side state. Critical: these maps are static, so in
     * singleplayer they survive leaving a world and loading another one in the
     * same JVM -- which made your technique follow you into a brand new world.
     * Called on server shutdown and on player disconnect.
     */
    public static void clearServer() { SERVER.clear(); }

    /** Drop one player's state (on disconnect). */
    public static void remove(java.util.UUID id) { SERVER.remove(id); }
}
