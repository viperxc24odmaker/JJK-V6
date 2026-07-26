JJK: Cursed Awakening — icon variants
=====================================

The mod currently uses the PURPLE one (Hollow Purple).

To switch: copy the variant you want over
    src/main/resources/assets/jjk/icon.png
and rebuild. Nothing in fabric.mod.json needs changing --
the path stays the same, only the image swaps.

  icon-purple.png    Hollow Purple  (current default)
  icon-sukuna.png    Sukuna crimson
  icon-gojo.png      Gojo blue
  icon-mahoraga.png  Mahoraga bone/gold

All are 128x128 RGBA, drawn at 64x64 and upscaled 2x
nearest-neighbour so the pixels stay crisp.

This folder is NOT shipped inside the built jar --
it only lives in the repo for convenience.
