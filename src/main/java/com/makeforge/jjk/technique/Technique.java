package com.makeforge.jjk.technique;

/**
 * Each cursed technique / character. maxCE and regenPerTick tune how the
 * character *feels* to play.
 *
 * v3 roster: the original 6 + Geto, Nanami, Maki, Mai, Yuta, Choso, Mahito.
 *
 * NOTE: NONE must stay first — the client resolves technique by ordinal.
 * Add new entries at the END so existing ordinals never shift.
 */
public enum Technique {
    NONE   ("None",    100f, 0.15f),
    // --- original six ---
    GOJO   ("Gojo",    140f, 0.20f),   // big pool, ults cost a fortune
    SUKUNA ("Sukuna",  120f, 0.30f),   // aggressive, keeps pressing
    YUJI   ("Yuji",     80f, 0.45f),   // physical, cheap, snappy regen
    MEGUMI ("Megumi",  150f, 0.18f),   // sustained summoner, slow regen
    NOBARA ("Nobara",  100f, 0.40f),   // cheap ranged spam
    TOJI   ("Toji",      0f, 0f),      // Heavenly Restriction — zero CE
    // --- v3 additions ---
    GETO   ("Geto",    160f, 0.22f),   // curse manipulation, swarms
    NANAMI ("Nanami",  110f, 0.28f),   // ratio technique, surgical
    MAKI   ("Maki",      0f, 0f),      // Heavenly Restriction — pure weapon
    MAI    ("Mai",      90f, 0.35f),   // construction — conjures shots
    YUTA   ("Yuta",    170f, 0.25f),   // absurd reserves + Rika
    CHOSO  ("Choso",   120f, 0.30f),   // blood manipulation
    MAHITO ("Mahito",  130f, 0.28f),   // idle transfiguration
    // --- v4 additions ---
    TODO     ("Todo",     130f, 0.28f), // boogie woogie — swap places
    HAKARI   ("Hakari",   150f, 0.25f), // jackpot — RNG payoff
    KASHIMO  ("Kashimo",  140f, 0.30f), // mythical beast amber — lightning
    URAUME   ("Uraume",   130f, 0.26f), // ice formation
    HIGURUMA ("Higuruma", 120f, 0.24f), // judgeman — executioner's blade
    YUKI     ("Yuki",     145f, 0.26f), // star rage — adds mass
    KENJAKU  ("Kenjaku",  165f, 0.20f), // a thousand stolen techniques
    INUMAKI  ("Inumaki",  100f, 0.32f), // cursed speech
    // --- the Disaster Curses ---
    JOGO     ("Jogo",     140f, 0.26f), // volcanic fire
    HANAMI   ("Hanami",   150f, 0.22f), // wood — drains and entangles
    DAGON    ("Dagon",    145f, 0.24f), // shikigami tides
    YOROZU   ("Yorozu",   135f, 0.27f); // construction — molten metal

    public final String display;
    public final float maxCE;
    public final float regenPerTick;

    Technique(String display, float maxCE, float regenPerTick) {
        this.display = display;
        this.maxCE = maxCE;
        this.regenPerTick = regenPerTick;
    }

    /** True for the Heavenly Restriction users — no cursed energy at all. */
    public boolean isRestriction() {
        return this == TOJI || this == MAKI;
    }

    public static Technique byName(String s) {
        for (Technique t : values()) {
            if (t.name().equalsIgnoreCase(s) || t.display.equalsIgnoreCase(s)) return t;
        }
        return null;
    }
}
