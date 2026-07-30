# RNR Concrete Flow

RNR Concrete Flow is a server-side addon for Roads and Roofs TFC that improves
large concrete-road construction without bypassing its material cost.

## What it changes

- One concrete bucket fills up to 25 nearest connected road foundations.
- Concrete follows narrow roads, turns, branches, and stepped downhill slopes.
- Blocked directions no longer waste part of the bucket.
- Spread speed is configurable and defaults to RNR's original 10-tick delay.
- RNR road blocks with support below need only one horizontal neighbor to resist
  landslides.
- Concrete powder can process in a TFC barrel with excess water. The amount of
  concrete produced remains proportional to the powder used.

Every feature can be disabled independently in the server config.

## Requirements

- Minecraft 1.20.1
- Forge 47.4.18+
- TerraFirmaCraft 3.2.20 through 3.2.x (tested through 3.2.23)
- Roads and Roofs TFC 0.2.2 through 0.2.x (tested through 0.2.4)

The mod is required on the server and optional on connecting clients. Install it
locally for singleplayer.

RNR Concrete Flow is an independent addon and is not affiliated with or
endorsed by the Roads and Roofs TFC team.
