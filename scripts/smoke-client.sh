#!/usr/bin/env bash
# Launch an actual development client under Xvfb and wait for its sound engine,
# which starts after the chat component and mixins have been initialized.
set -euo pipefail
module=${1:?target module required}
log=client-smoke.log
export ALSOFT_DRIVERS=null LIBGL_ALWAYS_SOFTWARE=1
(timeout -k 10s 240s xvfb-run -a ./gradlew -Ptarget="$module" ":$module:runClient" --no-daemon --console=plain >"$log" 2>&1) &
pid=$!
ok=false
for ((i=0; i<240; i++)); do
    if grep -q 'Sound engine started' "$log"; then
        # Give deferred startup errors a chance to appear.
        sleep 10
        if kill -0 "$pid" 2>/dev/null && ! grep -Eq 'Mixin apply for mod homegui failed|MixinApplyError|InvalidInjectionException|---- Minecraft Crash Report ----' "$log"; then
            ok=true
        fi
        break
    fi
    kill -0 "$pid" 2>/dev/null || break
    sleep 1
done
kill "$pid" 2>/dev/null || true
wait "$pid" 2>/dev/null || true
if [[ "$ok" != true ]]; then
    echo 'Minecraft startup smoke test failed or did not reach sound engine startup.'
    tail -n 100 "$log"
    # The Actions API exposes annotations even where raw job logs are unavailable.
    detail=$(tail -n 80 "$log" | tail -c 24000)
    detail=${detail//'%'/'%25'}
    detail=${detail//$'\r'/'%0D'}
    detail=${detail//$'\n'/'%0A'}
    echo "::error title=Minecraft startup::${detail}"
    exit 1
fi
echo 'Minecraft reached sound engine startup with HomeGui and no detected mixin failure.'
