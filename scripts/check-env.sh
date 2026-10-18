#!/usr/bin/env bash
# Keeps the env/*.example templates in step with the ${VAR} placeholders the services read.
set -euo pipefail
cd "$(dirname "$0")/.."

services=(auth gateway user)
python_configs=()
failures=0

fail() { echo "FAIL: $*"; failures=$((failures + 1)); }

# Variable names defined in a template, ignoring comments.
template_keys() { grep -v '^[[:space:]]*#' "$1" | sed -n 's/^\([A-Za-z_][A-Za-z0-9_]*\)=.*/\1/p'; }
# ${NAME}: no default, the service refuses to start without it.
no_default_vars() { grep -ohE '\$\{[A-Za-z_][A-Za-z0-9_]*\}' "$@" | tr -d '${}' | sort -u || true; }
# ${NAME:}: an empty default, which silently disables a feature when the value is forgotten.
empty_default_vars() { grep -ohE '\$\{[A-Za-z_][A-Za-z0-9_]*:\}' "$@" | tr -d '${}:' | sort -u || true; }
# True when ${NAME} or ${NAME:default} appears in any of the files.
referenced() { grep -qsE "\\\$\{$1[:}]" "${@:2}"; }

for svc in "${services[@]}"; do
  main="$svc/src/main/resources/application.yaml"
  template="env/dev/$svc.env.example"
  known="$(template_keys env/dev/common.env.example; template_keys "$template")"

  for var in $(no_default_vars "$main"; empty_default_vars "$main"); do
    echo "$known" | grep -qx "$var" || fail "$main reads $var, missing from env/dev/common.env.example and $template"
  done
  for var in $(template_keys "$template"); do
    referenced "$var" "$svc"/src/main/resources/application*.yaml || fail "$template defines $var, which $svc never reads"
  done
done

for var in $(template_keys env/dev/common.env.example); do
  referenced "$var" */src/main/resources/application*.yaml || fail "env/dev/common.env.example defines $var, which no service reads"
done

local_known="$(template_keys env/local.env.example)"
for var in $(no_default_vars */src/main/resources/application-local.yaml; \
             empty_default_vars */src/main/resources/application-local.yaml */src/main/resources/application.yaml); do
  echo "$local_known" | grep -qx "$var" || fail "a service reads $var on the host, missing from env/local.env.example"
done
for var in $local_known; do
  referenced "$var" */src/main/resources/application*.yaml || grep -qsw -e "$var" "${python_configs[@]}" \
    || fail "env/local.env.example defines $var, which nothing reads"
done

if [ "$failures" -gt 0 ]; then
  echo "$failures env mismatch(es)"
  exit 1
fi
echo "env templates match the service configs"
