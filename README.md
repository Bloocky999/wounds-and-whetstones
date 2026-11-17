# Hammer & Hearth

A standalone Fabric mod focused on slow, hands-on metalworking and custom forged weapons. Its mod ID is `hammerandhearth`; it does not include Pandemonium's conditions, moodles, masks, camera effects, or commands.

## Compatibility

- Minecraft **26.3**
- Fabric Loader **0.19.0 or later**
- Java **25**
- Fabric API

Install the mod on both the client and the server. The forging screen is a client/server minigame.

## Forging a weapon

1. Roast raw copper in a crucible over a lit campfire, smelt native copper into a bloom, and consolidate the bloom on an anvil with a forge hammer. Iron uses the corresponding vanilla ore and bloomery route.
2. Craft a blade billet from two ingots. Carry the billet, a forge hammer, and one handle material: a stick, bone, leather, or iron nugget.
3. Use the hammer on an anvil. Click individual cells on the 16×16 billet to strike them away. Right-click restores the last removed cell. Every strike wears the hammer.
4. Shape a connected blade, keep enough metal in the tang, and refine the cutting edge and point. The profile determines damage, durability, and enchantability.
5. Select a handle and finish the sword. Handle material changes attack speed and also influences durability or enchantability.

## Install the built jar

Download `hammer-and-hearth-0.2.0.jar` from the GitHub Actions artifact or the Releases page and place it in the `mods` folder for both Minecraft client and server.

## Build

Requires Java 25 and Gradle. Run `gradle build --no-daemon`; the mod jar is written to `build/libs/`.

## License

All Rights Reserved. Code and original mod content by Bloocky999. Fabric Loader and Fabric API are separate dependencies.
