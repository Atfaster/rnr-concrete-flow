# Publishing checklist

## GitHub

- Review `git status`, the staged diff, and repository visibility.
- Confirm no secrets, local paths, downloaded dependency JARs, logs, or worlds
  are tracked.
- Run both dependency combinations in `docs/TESTING.md`.
- Inspect the release JAR and sources JAR.
- Commit only after manual approval.
- Create an annotated `v<version>` tag only after manual approval.
- Create a GitHub Release and attach both JARs.

## CurseForge project

- Project name: RNR Concrete Flow
- Game: Minecraft 1.20.1
- Mod loader: Forge
- Release type: Release
- Categories: Addons, Server Utility, Player Transport
- Required dependencies: TerraFirmaCraft, Roads and Roofs TFC
- Upload only the release JAR, not the sources JAR.
- Use `docs/curseforge-description.md` as the project description.
- Use `assets/logo.png` only if it meets CurseForge's current image rules.
- State that the addon is independent and does not redistribute RNR/TFC assets.

Before uploading, complete every scenario in `docs/TESTING.md`. A successful
compile does not replace dedicated-server, client-join, singleplayer, or
in-game behavior testing.
