# RNR Concrete Flow

RNR Concrete Flow is a server-side compatibility addon for
[Roads and Roofs TFC](https://www.curseforge.com/minecraft/mc-mods/roads-and-roofs-tfc)
on Minecraft 1.20.1 Forge.

It keeps the immersive concrete-road workflow while making large road projects
less wasteful and easier to build on slopes.

## Features

- Fills up to 25 nearest connected road foundations from one concrete bucket.
- Lets additional concrete extend an existing connected wet-concrete area.
- Uses 40 mB per newly filled block and preserves sub-40 mB remainders.
- Limits every normal refill click to 1,000 mB. Large containers can be clicked
  repeatedly while retaining their unused concrete.
- Uses breadth-first spreading, so blocked branches do not waste concrete.
- Follows cardinally connected foundations and descends stepped slopes.
- Never climbs upward, crosses gaps, or force-loads chunks.
- Uses RNR's original 10-tick spread interval by default.
- Lets RNR road blocks resist landslides with one horizontal support when they
  also have support below.
- Lets concrete powder process in a TFC barrel containing excess water.

Example: 64 concrete powder in 10,000 mB of water produces 6,400 mB of concrete.
The unused water is discarded because a TFC barrel cannot contain two fluids.

## Requirements

- Minecraft 1.20.1
- Forge 47.4.18 or newer
- TerraFirmaCraft 3.2.20 to 3.2.x (tested through 3.2.23)
- Roads and Roofs TFC 0.2.2 to 0.2.x (tested through 0.2.4)

The addon is required on the logical server. Clients may connect without it.
Install it on the client as well when playing singleplayer.

## Configuration

Forge creates `rnr_concrete_flow-server.toml` in the world's `serverconfig`
directory.

| Option | Default | Description |
| --- | ---: | --- |
| `enableConnectedConcreteFlow` | `true` | Enables connected 25-block concrete spreading. |
| `concreteSpreadIntervalTicks` | `10` | Delay between waves, from 1 to 1200 ticks. |
| `enableSingleSideRnrRoadSupport` | `true` | Allows one horizontal support to stabilize RNR landslide blocks. |
| `enablePartialConcreteMixing` | `true` | Allows concrete mixing with excess barrel water. |

Changing `enableConnectedConcreteFlow` to `false` cancels active custom flow
tasks and restores RNR's original behavior for future scheduled ticks.

## Extending wet concrete

Right-click a connected wet-concrete road block with a fluid container holding
RNR concrete. Existing wet blocks form a free starting area; only newly filled
foundations consume concrete.

- Each new block costs 40 mB.
- A normal right-click uses at most 1,000 mB (25 blocks).
- Sneak-right-click is left entirely to the container's original behavior.
- Any remainder below 40 mB stays in the container.
- Only concrete for foundations reachable and reserved at interaction time is
  drained.

For example, 100 mB fills two blocks and leaves 20 mB. Each normal click with a
10,000 mB TFC barrel can fill at most 25 blocks, so a full barrel takes up to
ten clicks.

## Building

```text
./gradlew build
```

Windows:

```powershell
.\gradlew.bat build
```

Release and sources JARs are written to `build/libs`.

## Compatibility and updates

The mod uses narrowly scoped mixins against RNR and TFC. Each supported RNR
update must be tested before widening the dependency range. See
[`docs/TESTING.md`](docs/TESTING.md).

Security issues should be reported privately as described in
[`SECURITY.md`](SECURITY.md).

An in-progress pour is intentionally not persisted across a server crash. All
already placed wet concrete remains in the world, but unused capacity from that
bucket is lost.

## Credits

- Roads and Roofs TFC by Therighton and contributors.
- TerraFirmaCraft by Dries007 and contributors.

This addon is independent and is not an official Roads and Roofs TFC project.
No RNR or TFC assets are redistributed.

## License

[MIT](LICENSE)
