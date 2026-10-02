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

Optional: [Accessories](https://modrinth.com/mod/accessories), [Polymer AutoHost](https://modrinth.com/mod/polymer) for automatic resource pack hosting.

## Install

1. Install Artifacts, Polymer, Fabric API, and Trinkets Updated on the **server**.
2. Add **artifacts-polymer-patch** to the server `mods` folder (build with `./gradlew build`, jar under `build/libs/`).
3. Ensure clients accept the Polymer resource pack (AutoHost, or `/polymer generate-packs` and host the pack yourself).
4. Clients **do not** need Artifacts installed.

## What this patch does

- **Mixin** on Artifacts’ `FabricRegister` to register Polymer overlays when Artifacts registers content (items, entities, sounds, data components, attributes, etc.).
- **Items**: `VanillaModeledPolymerItem` overlays with Artifacts assets bridged into the Polymer pack (`PolymerResourcePackUtils.addModAssets("artifacts")`).
- **Mimic entity**: custom `PolymerEntity` overlay (visual placeholder; see limitations).
- **Networking**: skips Artifacts **clientbound** custom payloads when the client cannot receive them (avoids disconnects on vanilla clients).
- **CCA**: limits `SwimDataComponent` sync to clients that can receive Artifacts swim packets.

## What vanilla clients see (honest status)

| Area | Status |
|------|--------|
| Joining the server | **Works** with Polymer pack when networking/registries are patched |
| Artifact items in inventory / trinkets | **Mostly works**; models/textures from Artifacts pack, vanilla item type on wire (`trial_key` base) |
| Artifact abilities / combat | **Server-side**; should function; client-only feedback may be missing |
| Mimic mob | **Partial**; static **chest** block display on a hidden armor stand (no open/attack animation, no Artifacts mimic model) |
| Equipped trinket rendering on player | **Limited** without Artifacts client or extra Polymer virtual-entity work |
| Artifact toggle key / config UI | **Not available** on vanilla clients |
| Custom sounds at player | Server plays sounds; duplicate client packet is skipped for vanilla |
| Campsite structures / worldgen | **Server-only** registries; clients do not need Artifacts blocks |

This is an incremental patch: it compiles and covers the main registry and item/entity polymerization path. Further work (mimic animations/facing, trinket body rendering, Accessories UI) is expected.

## Building

```bash
./gradlew build
```

The build downloads `artifacts-fabric-15.1.3.jar` into `libs/` on first compile (Modrinth Maven’s `15.1.3` coordinate is not the Fabric jar).

## License

MIT (same as this repository). Artifacts and Polymer are separate projects with their own licenses.
