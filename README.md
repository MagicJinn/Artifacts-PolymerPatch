# Artifacts Polymer Patch

![icon.png](https://raw.githubusercontent.com/MagicJinn/Artifacts-PolymerPatch/refs/heads/main/icon.png)

## Play Artifacts with vanilla clients

![Modrinth: Artifacts Polymer Patch](https://img.shields.io/badge/Modrinth-Artifacts_Polymer_Patch-00ae5d?logo=modrinth) ![CurseForge: Artifacts Polymer Patch](https://img.shields.io/badge/CurseForge-Artifacts_Polymer_Patch-f16437?logo=curseforge)

A companion patch for [Artifacts](https://modrinth.com/mod/artifacts) that adds the [Polymer](https://modrinth.com/mod/polymer) glue so **vanilla clients** can join a server and use most of the mod. Artifacts still does the real work. This only fills in the places where it normally leans on its own client mod.

Inspired by the [PolymerPorts](https://github.com/PolymerPorts) patches.

**Minecraft 26.1.2** · **Artifacts 15.1.3** · **Polymer 0.16.5** · **Java 25**

## Server setup

1. Install **Artifacts**, **Polymer**, **Fabric API**, **Trinkets Updated**, **Trinkets Polymer**, and this patch on the server.
2. Enable Auto-Host in Polymer settings (`config/polymer/auto-host.json`).
3. Have clients accept the Polymer resource pack.

Clients install **nothing**: no Fabric API, no Artifacts, no Trinkets.

## Vanilla clients only

This patch is **server-side / Polymer only** for now. Players with the Artifacts client mod are **kicked on join**. Install Artifacts on the server only; clients use the Polymer resource pack.

Mixing Artifacts client + Polymer stand-ins does not work reliably, so dual-path support was dropped in favor of a hard vanilla-client requirement.

## Workarounds

Artifacts expects its client mod for a handful of things. This patch fakes them server-side for vanilla clients, and patched items explain themselves in their tooltip:

- **Aqua Dashers**: Shows waterlogged barriers while sprinting to avoid desync.
- **Strider Shoes**: Shows magma underfoot on lava so walking stays in sync.
- **Snowshoes**: Shows snow blocks in powder snow so walking stays in sync.
- **Steadfast Spikes**: Ice blocks are replaced with non-slippery ice so you don't slip out of sync.
- **Helium Flamingo**: Best-effort at simulating swimming physics in the air. Shrinks player hitbox while flying, and shows a charge bar while using or charging.
- **Flippers**: Spoofs swimming speed on the client and server.
- **Charm of Sinking**: Increases sinking, walking speed and step height in water to compensate. Always on (no toggle).
- **Digging Claws**: Triggers block breaking early on the server.
- **Umbrella**: Held upright like a spear so the canopy looks right.
- **Warp Drive**: your inventory resyncs after throwing a pearl, so it doesn't look used up.
- **Everlasting Beef / Eternal Steak**: comes back after every bite, with no slot flicker.
- **Mimic**: a proper chest that opens its lid when it hops.
- **Equip sounds**: everyone hears them, including the wearer.

## Good to know

- **Worn artifacts are invisible** on the player model; the item in your hand and inventory is fine.
- **The Artifacts config screen and toggle key are client-only**, so vanilla players can't use them.
- Abilities run server-side, so some client-side feedback may be missing or look a little different.
