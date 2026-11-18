#!/usr/bin/env bash
set -u

for attempt in 1 2; do
  if "$HOME/.maestro/bin/maestro" test .maestro; then
    exit 0
  fi
  echo "Maestro attempt $attempt failed; retrying after emulator settle"
  adb shell am force-stop org.dpdns.bnai.anime >/dev/null 2>&1 || true
  sleep 10
done

exit 1
