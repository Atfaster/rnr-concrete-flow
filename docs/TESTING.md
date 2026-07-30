# Release testing

Run both supported dependency boundaries before every release:

```powershell
.\gradlew.bat clean build --no-daemon `
  -Ptfc_curse_file=7442504 `
  -Prnr_curse_file=7339318

.\gradlew.bat clean build --no-daemon `
  -Ptfc_curse_file=8242625 `
  -Prnr_curse_file=8099778
```

These combinations currently represent RNR 0.2.2/TFC 3.2.20 and
RNR 0.2.4/TFC 3.2.23 respectively.

## Automated

- Unit tests pass.
- The release JAR contains `META-INF/mods.toml`, the mixin config, and refmap.
- No RNR, TFC, Minecraft, or Forge classes are bundled into the release JAR.
- `verifyReleaseJar` passes for both dependency combinations.

## Dedicated server

Test with the minimum and newest supported RNR/TFC versions.

1. Start a dedicated Forge server and confirm there are no mixin errors.
2. Connect with the addon installed on both sides.
3. Connect with the addon absent on the client.
4. Pour onto straight foundations one, two, and three blocks wide.
5. Verify exactly 25 blocks are filled when enough foundations are reachable.
6. Test turns, branches, obstacles, gaps, chunk borders, and stepped descents.
7. Verify concrete never climbs upward.
8. Break a newly filled frontier block before the next wave and verify it is
   not restored and does not continue spreading.
9. Unload a dimension during a long configured delay and verify no task leaks.
10. Toggle every config option and restart.

## Landslides

- No horizontal supports: the RNR road block may slide.
- One horizontal support plus support below: it stays.
- Two horizontal supports: it stays.
- Confirm ordinary TFC landslide blocks retain their original behavior.

## Barrel

- 10,000 mB water plus 64 powder produces 6,400 mB concrete.
- Exact ratios still work.
- Other instant barrel recipes retain their original matching behavior.
