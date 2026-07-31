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

## Extending existing wet concrete

Use both the RNR metal bucket and TFC fluid containers.

- 39 mB: no block is reserved and no fluid is consumed.
- 40 mB: one block is filled.
- 100 mB: two blocks are filled and 20 mB remains.
- 999 mB: 24 blocks are filled and 39 mB remains.
- 1,000 mB: 25 blocks are filled.
- Normal-click a 10,000 mB TFC barrel: at most 25 blocks are filled and
  9,000 mB remains when enough foundations are reachable.
- Repeat normal clicks with the same 10,000 mB barrel: each click fills at most
  25 blocks and ten successful full clicks consume the barrel.
- Sneak-click concrete, water, and another non-concrete fluid container and
  verify the addon's handler does not intercept their original behavior.
- If only 17 foundations are reachable, consume only 680 mB.
- With no reachable foundation, consume nothing and do not place a concrete
  fluid block above the road.
- Refill a wet area whose original spread is still active.
- Have two players refill the same active area in quick succession.
- Join two independently active wet areas and verify no foundation is filled or
  charged twice.
- Break a reserved frontier parent and verify neither it nor dependent blocks
  are restored.
- Repeat the metal-bucket cases from a client without this addon. A temporary
  client prediction is acceptable only if the server immediately corrects it;
  the final world state and fluid amount must be authoritative.

## Landslides

- No horizontal supports: the RNR road block may slide.
- One horizontal support plus support below: it stays.
- Two horizontal supports: it stays.
- Confirm ordinary TFC landslide blocks retain their original behavior.

## Barrel

- 10,000 mB water plus 64 powder produces 6,400 mB concrete.
- Exact ratios still work.
- Other instant barrel recipes retain their original matching behavior.
