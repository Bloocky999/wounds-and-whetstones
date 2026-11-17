# Hammer & Hearth

A standalone Fabric mod focused on slow, hands-on metalworking and custom forged weapons. Its mod ID is `hammerandhearth`; it does not include Pandemonium's conditions, moodles, masks, camera effects, or commands.

## Compatibility

- Minecraft **26.3**
- Fabric Loader **0.19.0 or later**
- Java **25**
- Fabric API

Install the mod on both the client and the server. The forging screen is a client/server minigame.

All mod ingredients appear in the **Hammer & Hearth** creative tab. The crafting and smelting recipes are unlocked in the vanilla recipe book as soon as a world loads.

## Forging a weapon

1. Craft the clay crucible and use it on a lit campfire while carrying raw copper. This gives native copper. Blast it into a copper bloom, then use the forge hammer on an anvil to consolidate it into a copper ingot. Blast raw iron into an iron bloom and consolidate it on the anvil in the same way.
2. Craft a blade billet from two ingots arranged vertically. Carry the billet, the forge hammer, and one handle material: a stick, bone, leather, or iron nugget.
3. Use the hammer on an anvil. The screen explains the three shaping goals. Click cells on the 16×16 billet to strike metal away; right-click restores the last cell and also wears the hammer.
4. Make the top edge into a cutting edge, leave one or two pixels at the point, and keep the tang connected. The live stats show whether the shape can be finished.
5. Select a handle and forge the sword. The blade profile changes damage, durability, and enchantability; the handle changes attack speed and can also influence durability or enchantability.

## Install the built jar

Download `hammer-and-hearth-0.2.1.jar` from the GitHub Actions artifact or the Releases page and place it in the `mods` folder for both Minecraft client and server.

## Build

Requires Java 25 and Gradle. Run `gradle build --no-daemon`; the mod jar is written to `build/libs/`.

## License

All Rights Reserved. Code and original mod content by Bloocky999. Fabric Loader and Fabric API are separate dependencies.
