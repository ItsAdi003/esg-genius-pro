#!/usr/bin/env bash
# Starts the ESGenius backend for local development (works in Git Bash, macOS and Linux).
#
# Why this exists: the backend only reads real environment variables. The Supabase settings it needs
# live in the frontend's ../.env under VITE_ names, so a plain `mvnw spring-boot:run` starts without
# them and every API call answers 401 "Authentication is not configured".
#
# What it does, in order (a variable that is already set in your shell is never overwritten):
#   1. loads backend/.env if you created one (KEY=VALUE lines; copy backend/.env.example, never commit it)
#   2. fills SUPABASE_URL and SUPABASE_ANON_KEY from ../.env (VITE_SUPABASE_URL, VITE_SUPABASE_ANON_KEY)
#   3. reports which required variables are missing, by name only (no values are printed)
#   4. starts the backend with the Maven wrapper
#
# Usage:  ./run-local.sh            (from backend/, or from anywhere: backend/run-local.sh)
#         ./run-local.sh --check    (only report which variables are set; does not start anything)

set -u
cd "$(dirname "${BASH_SOURCE[0]}")" || exit 1

# strip carriage returns and one pair of surrounding quotes from a value
clean() { printf '%s' "$1" | tr -d '\r' | sed -E 's/^"(.*)"$/\1/; s/^'"'"'(.*)'"'"'$/\1/'; }

# set VAR to VALUE unless VAR already has a value
default_var() {
  local name="$1" value="$2"
  if [ -z "${!name:-}" ] && [ -n "$value" ]; then export "$name=$value"; fi
}

# 1. backend/.env
if [ -f .env ]; then
  while IFS= read -r line || [ -n "$line" ]; do
    line="$(printf '%s' "$line" | tr -d '\r')"
    case "$line" in ''|\#*) continue ;; esac
    key="${line%%=*}"; val="${line#*=}"
    case "$key" in [A-Za-z_]*) default_var "$key" "$(clean "$val")" ;; esac
  done < .env
  echo "loaded backend/.env"
fi

# 2. Supabase settings from the frontend .env
if [ -f ../.env ]; then
  url="$(grep -E '^VITE_SUPABASE_URL=' ../.env | head -1 | cut -d= -f2-)"
  key="$(grep -E '^VITE_SUPABASE_ANON_KEY=' ../.env | head -1 | cut -d= -f2-)"
  default_var SUPABASE_URL "$(clean "$url")"
  default_var SUPABASE_ANON_KEY "$(clean "$key")"
fi

# 3. report what is missing (names only)
for v in SUPABASE_URL SUPABASE_ANON_KEY DB_URL DB_USERNAME DB_PASSWORD GEMINI_API_KEY; do
  if [ -n "${!v:-}" ]; then
    echo "  $v: set"
  else
    echo "  $v: MISSING"
  fi
done
if [ -z "${SUPABASE_URL:-}" ] || [ -z "${SUPABASE_ANON_KEY:-}" ]; then
  echo "ERROR: the Supabase settings are missing, so every API call would return 401. Put VITE_SUPABASE_URL and"
  echo "       VITE_SUPABASE_ANON_KEY in ../.env, or SUPABASE_URL and SUPABASE_ANON_KEY in backend/.env."
  exit 1
fi
if [ -z "${DB_URL:-}" ] || [ -z "${DB_PASSWORD:-}" ]; then
  echo "WARNING: DB_URL or DB_PASSWORD is missing; the backend will try localhost:5432/esgenius and probably fail."
fi
if [ -z "${GEMINI_API_KEY:-}" ]; then echo "NOTE: GEMINI_API_KEY is missing; classification and the assistant will fail (requirements go to human review)."; fi

if [ "${1:-}" = "--check" ]; then exit 0; fi

# 4. start (the Windows wrapper works from Git Bash; the plain script works elsewhere)
case "$(uname -s)" in
  MINGW*|MSYS*|CYGWIN*) exec ./mvnw.cmd spring-boot:run "$@" ;;
  *) exec ./mvnw spring-boot:run "$@" ;;
esac
