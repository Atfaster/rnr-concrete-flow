# Changelog

All notable changes to this project will be documented in this file.

## 1.0.0 - 2026-07-30

- Added connected breadth-first concrete spreading with a 25-block capacity.
- Added proportional refilling of connected wet-concrete areas from fluid
  containers at 40 mB per newly reserved block.
- Added safe proportional refills of up to 1,000 mB per normal click for
  containers, including repeated use of TFC barrels.
- Added atomic reservation and task merging for repeated or concurrent refills.
- Added same-level priority and downhill step support.
- Added configurable RNR-compatible spreading speed.
- Added one-side landslide support for RNR road blocks.
- Added partial concrete mixing with excess barrel water.
- Added server-only network compatibility.
