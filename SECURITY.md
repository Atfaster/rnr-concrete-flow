# Security policy

## Supported versions

Security fixes are provided for the latest release of RNR Concrete Flow.

## Reporting a vulnerability

Please do not open a public issue for a suspected vulnerability. Use GitHub's
private vulnerability reporting for this repository instead. Include the mod
version, Forge/TFC/RNR versions, reproduction steps, and any relevant logs.

Do not include access tokens, private server addresses, world data, or other
secrets in a report.

## Runtime scope

RNR Concrete Flow does not intentionally access the filesystem or network
during gameplay, start external processes, or collect telemetry. Gameplay
changes run on the logical server thread.
