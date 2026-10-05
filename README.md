# Artifacts Polymer Patch

Companion **patch** mod for [Artifacts](https://modrinth.com/mod/artifacts) on Fabric. It does **not** replace or reimplement Artifacts; the real Artifacts mod must be installed on the server. This project adds [Polymer](https://modrinth.com/mod/polymer) hooks so **vanilla clients** (without Artifacts) can join and see a reasonable subset of Artifacts content via Polymer’s server-generated resource pack.

Inspired by [PolymerPorts](https://github.com/PolymerPorts) patches such as [cc-tweaked-patch](https://github.com/PolymerPorts/cc-tweaked-patch) and [farmers-delight-patch](https://github.com/PolymerPorts/farmers-delight-patch).

## Minecraft version

- **Minecraft 26.1.2**
- **Artifacts** `15.1.3` (Fabric)
- **Polymer** `0.16.5+26.1.2`
- **Fabric API** `0.145.3+26.1.1`
- **Java 25**

## Required mods (server)

| Mod | Notes |
|-----|--------|
| [Artifacts](https://modrinth.com/mod/artifacts) | Required; gameplay and content come from this mod |
| [Polymer](https://modrinth.com/mod/polymer) (bundled or modules) | Required |
| [Fabric API](https://modrinth.com/mod/fabric-api) | Required |
| [Trinkets Updated](https://modrinth.com/mod/trinkets-updated) | Strongly recommended; Artifacts equips many items as trinkets on Fabric |
| [Trinkets Polymer](https://modrinth.com/mod/trinkets-polymer) | **Required on the server** whenever Trinkets Updated is installed — handles Fabric registry sync and player inventory/menu slot count for vanilla clients (server-only mod; clients install nothing) |

Optional on the **server**: [Accessories](https://modrinth.com/mod/accessories), [Polymer AutoHost](https://modrinth.com/mod/polymer) for automatic resource pack hosting.

**Vanilla clients** need no mods (no Fabric API, no Trinkets). Accept the Polymer resource pack when prompted.

## Install

1. Install Artifacts, Polymer, Fabric API, **Trinkets Updated**, and **[Trinkets Polymer](https://modrinth.com/mod/trinkets-polymer)** on the **server** (Trinkets Polymer is not optional if you run Trinkets Updated with vanilla clients).
2. Add **artifacts-polymer-patch** to the server `mods` folder (build with `./gradlew build`, jar under `build/libs/`).
3. Ensure clients accept the Polymer resource pack (AutoHost, or `/polymer generate-packs` and host the pack yourself).
4. Clients **do not** need Artifacts installed.

## What this patch does

- **Mixin** on Artifacts’ `FabricRegister` to register Polymer overlays when Artifacts registers content (items, entities, sounds, data components, attributes, etc.).
- **Items (held / inventory)**: `PolyArtifactsItem` overlays with Artifacts `assets/artifacts/items` models bridged into the Polymer pack, including server-side `artifacts:needs_repair` item model property. Items with Artifacts cuboid `*_held` models (currently **umbrella**) keep flat GUI sprites and use the existing held JSON in hand via `minecraft:display_context` / `minecraft:using_item` in the generated pack.
- **Networking**: skips Artifacts **clientbound** custom payloads when the client cannot receive them (avoids disconnects on vanilla clients).
- **CCA**: limits `SwimDataComponent` sync to clients that can receive Artifacts swim packets.
- **Digging claws (Polymer clients)**: server destroy-progress scaling so break time matches server `BLOCK_BREAK_SPEED` from Artifacts (vanilla clients still gate mining on local attributes).
- **Resource pack**: rewrites `assets/artifacts/items/*.json` in the generated pack to drop `artifacts:needs_repair` conditions (vanilla clients cannot load that property).
- **Everlasting beef / eternal steak**: polymer wire type is vanilla beef (eat animation); after eat the server shrinks once and refills the slot like a bottle remainder so client prediction matches.
- **Tooltips**: server-safe helium flamingo ability text (no `ModKeyMappings` / client key bindings on dedicated server during Polymer tooltip build).
- **Toggle trinkets (Polymer clients)**: Universal Attractor, night vision goggles, and similar items default to **on** (Artifacts’ toggle key is client-only).
- **Aqua dashers (Polymer clients)**: per-viewer **client-only** waterlogged `barrier` footing via `ClientboundBlockUpdatePacket` while water-sprinting (server uses Artifacts fluid collision; world blocks unchanged).
- **Mimic (Polymer clients)**: interaction entity hitbox plus item display — chest on ground/idle, mimic spawn egg model while airborne.

Workarounds in source are tagged `POLYMER WORKAROUND:` (grep the repo). **Chorus totem / equipable totem** death protection is fully server-side in Artifacts and needs no patch workaround.

## What vanilla clients see (honest status)

| Area | Status |
|------|--------|
| Joining the server | **Works** with Polymer pack when networking/registries are patched |
| Artifact items in inventory / hotbar | **Works** for item-model-based Artifacts (Polymer pack + `trial_key` wire type); **umbrella** uses 3D held models in hand |
| Trinket slot UI (equip screen) | **[Trinkets Polymer](https://modrinth.com/mod/trinkets-polymer)** on the server; vanilla clients install nothing |
| Worn Artifacts on player body | **Not shown** (Artifacts client 3D meshes only; Polymer virtual item displays were removed as unstable) |
| Artifact abilities / combat | **Server-side**; should function; client-only feedback may be missing |
| Aqua dashers water sprint | **Server** fluid collision + **client-predicted** footing (fake waterlogged barriers for that viewer only) |
| Mimic mob | **Interaction** hitbox (mimic size) + item display: **chest** on ground/idle, **mimic spawn egg** while airborne |
| Artifact toggle key / config UI | **Not available** on vanilla clients; toggle trinkets are forced **on** while equipped |
| Custom sounds at player | Server plays sounds; duplicate client packet is skipped for vanilla |
| Campsite structures / worldgen | **Server-only** registries; clients do not need Artifacts blocks |

Further work: stable worn-trinket visuals, Accessories path.

**Without [Trinkets Polymer](https://modrinth.com/mod/trinkets-polymer) on the server** (while Trinkets Updated is present), vanilla clients typically disconnect on join (extra inventory slots / registry sync). Trinkets Polymer is a separate **server** mod; do not install it on vanilla clients.

## Building

```bash
./gradlew build
```

The build downloads into `libs/` on first compile:

- `artifacts-fabric-15.1.3.jar` (Modrinth Maven’s `15.1.3` coordinate is not the Fabric jar)
- `trinkets-polymer-patch-4.0.0-rc.1.0+26.1.jar` ([Trinkets Polymer](https://modrinth.com/mod/trinkets-polymer), not on Maven)

### Debug command (operators)

- `/artifactspatch all` or `/artifacts-polymer all` (permission level **2**, same as vanilla OP commands)
- Places a **double chest** at your feet (or the nearest spot with room) containing **one of every** `artifacts:*` item from the item registry (including the mimic spawn egg).

### Local server (`runServer`)

Gradle pulls **[Trinkets Updated](https://modrinth.com/mod/trinkets-updated)** and **Trinkets Polymer** as runtime dependencies so `./gradlew runServer` matches a recommended production server (Artifacts trinkets + vanilla join).

## License

MIT (same as this repository). Artifacts and Polymer are separate projects with their own licenses.
