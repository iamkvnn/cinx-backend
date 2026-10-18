#!/usr/bin/env bash
set -Eeuo pipefail

usage() {
  printf 'Usage: %s [--integration] <module>[,<module>...] [...]\n' "$0"
}

integration=false
modules=()

while (($# > 0)); do
  case "$1" in
    --integration|-i)
      integration=true
      shift
      ;;
    --modules|-m)
      if (($# < 2)); then
        printf 'Missing value for %s.\n' "$1" >&2
        usage >&2
        exit 2
      fi
      IFS=',' read -r -a requested_modules <<< "$2"
      modules+=("${requested_modules[@]}")
      shift 2
      ;;
    --help|-h)
      usage
      exit 0
      ;;
    --*)
      printf 'Unknown option: %s\n' "$1" >&2
      usage >&2
      exit 2
      ;;
    *)
      IFS=',' read -r -a requested_modules <<< "$1"
      modules+=("${requested_modules[@]}")
      shift
      ;;
  esac
done

if ((${#modules[@]} == 0)); then
  printf 'Provide at least one Maven module.\n' >&2
  usage >&2
  exit 2
fi

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd -- "$script_dir/.." && pwd)"

validated_modules=()
for module in "${modules[@]}"; do
  module="${module//[[:space:]]/}"
  if [[ -z "$module" || ! "$module" =~ ^[A-Za-z0-9_.-]+$ || ! -f "$repo_root/$module/pom.xml" ]]; then
    printf 'Unknown Maven module: %s\n' "$module" >&2
    exit 2
  fi
  validated_modules+=("$module")
done

module_csv="$(IFS=,; printf '%s' "${validated_modules[*]}")"
maven_arguments=(-f "$repo_root/pom.xml" -pl "$module_csv" -am)
mode="unit"

if [[ "$integration" == true ]]; then
  maven_arguments+=(-Pintegration verify)
  mode="unit and integration"
else
  maven_arguments+=(test)
fi

printf 'Verifying Maven module(s) [%s] in %s mode.\n' "$module_csv" "$mode"
(
  cd -- "$repo_root"
  mvn "${maven_arguments[@]}"
)
