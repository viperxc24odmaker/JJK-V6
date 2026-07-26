# JJK: Cursed Energy (v1) — Fabric 1.21.11

MakeForge / PixelForge.

## Controls
- `/technique <gojo|sukuna|yuji|megumi|nobara|toji>` — pick your technique
- `/ce` — check your cursed energy
- **G** = primary · **H** = secondary · **J** = ultimate / domain
- HUD bar (bottom-left) shows CE + current technique. Toji shows a red
  "Heavenly Restriction" bar (no CE — pure physical).

## Roster
Gojo (Blue / Red / Hollow Purple), Sukuna (Dismantle / Cleave / Malevolent
Shrine), Yuji (Divergent Fist / Black Flash / Manji Kick), Megumi (Divine Dogs /
Nue / Chimera Shadow Garden), Nobara (Hairpin / Resonance / Straw Doll), Toji
(Inverted Spear / Playful Cloud / Heavenly Restriction).

Black Flash tip: land a Divergent Fist first — it opens a short window where
Black Flash hits for double.

## v1 notes
- CE + technique are **session-only** (reset on relog). Persistence is next.
- No custom entity renderers — everything uses vanilla-safe mechanics.

## Build setup (verified against 1.21.11)
- Fabric Loader 0.18.2, Fabric API 0.141.5+1.21.11, Loom 1.16.3, Java 21,
  official Mojang mappings.

## If the first CI build still complains, look here first
Most 1.21.11 API traps were checked against the official migration primer and
fixed (Identifier factory rename, Wolf.setTame arity, no custom RenderType, no
permission-level command checks). Remaining lower-confidence spots:
1. `technique/TechniqueAbilities.java` — the `MobEffects.*` holder names
   (SPEED / STRENGTH / RESISTANCE / HASTE) if any got renamed.
2. `client/JJKClient.java` — `HudRenderCallback` is deprecated in 1.21.11
   (still present; fully removed in 26.1). Compiles now; swap to HudElementRegistry
   when you move past 1.21.11.
