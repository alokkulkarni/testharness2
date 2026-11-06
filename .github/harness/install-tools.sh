#!/usr/bin/env bash
# Installs the runner toolchain at the versions pinned in docker/versions.env (SDD 08 §1, ADR-0010, NFR-011).
# The same script builds the runner images (docker/runner/Dockerfile) and runs in copilot-setup-steps.yml, where no
# container is available (C-07). Ubuntu 24.04, amd64 or arm64; run as root, or as a user with sudo. Bash 3.2 or later.
#
# Usage: install-tools.sh [--host] [--dry-run] <layer|tool>...  layers: core, codeql, mobile (docker/tools.json names
#        them; mobile is amd64 only)
#   --host      on a host that is not ours (GitHub's runner for copilot-setup-steps.yml): leave its apt alone, so apt
#               tools are skipped (its own are used) and its sources are never rewritten
#   --dry-run   print each tool and its downloads (and apt, npm, pip packages) without installing anything
# Env: HARNESS_ARCH (amd64|arm64, default from uname -m), HARNESS_VERSIONS_ENV, HARNESS_TOOLS_JSON,
#      HARNESS_APT_SOURCES (default /etc/apt/sources.list.d/ubuntu.sources).
#
# Integrity: every download is checked against the publisher's checksum file (which catches corruption, not a
# compromised publisher, since it comes from the same place) or, where the publisher has none, a sha256 pinned in
# versions.env. apt packages come from the Ubuntu snapshot pinned in versions.env, with the archive's signatures.
# npm, pip and gem install pinned top-level versions; their dependencies and Playwright's browsers are not hash-pinned
# (residual, docs/spikes/M07.md). Any failing step fails the script.
set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# Beside the script in an image (/usr/share/sdlc-harness), one level up in the repository (docker/).
DIR="$HERE"
[ -f "$DIR/versions.env" ] || DIR="$HERE/.."
VERSIONS="${HARNESS_VERSIONS_ENV:-$DIR/versions.env}"
TOOLS_JSON="${HARNESS_TOOLS_JSON:-$DIR/tools.json}"
SOURCES="${HARNESS_APT_SOURCES:-/etc/apt/sources.list.d/ubuntu.sources}"
PREFIX=/opt/sdlc-harness
DRY=0
HOST=0

die() { printf 'install-tools: %s\n' "$*" >&2; exit 1; }
dry() { [ "$DRY" = 1 ]; }

set -a
# shellcheck source=/dev/null
. "$VERSIONS"
set +a

case "${HARNESS_ARCH:-$(uname -m)}" in
  x86_64 | amd64) ARCH=amd64 ;;
  aarch64 | arm64) ARCH=arm64 ;;
  *) die "unsupported architecture: ${HARNESS_ARCH:-$(uname -m)} (amd64 and arm64 only)" ;;
esac
# The names each publisher gives the two architectures.
pick() { if [ "$ARCH" = amd64 ]; then echo "$1"; else echo "$2"; fi; }

SUDO=""
[ "$(id -u)" = 0 ] || SUDO="sudo"
TMP=""
DL=""
trap '[ -z "$TMP" ] || rm -rf "$TMP"' EXIT
CURL=(curl -fsSL --retry 3 --proto '=https' --proto-redir '=https' --tlsv1.2)

# get <url> <sha256|sha512> <hash | @checksums-url>: download to $DL and verify it. A dry run prints the URLs only;
# each installer then stops with `dry && return 0`.
get() {
  local url="$1" algo="$2" want="$3" name sums
  name="$(basename "${url%%\?*}")"
  if dry; then
    echo "$url"
    if [ "${want:0:1}" = @ ]; then echo "${want:1}"; fi
    return 0
  fi
  command -v curl >/dev/null || apt_ready # a bare image: curl, unzip and xz-utils come with apt (not with --host)
  if [ -z "$TMP" ]; then
    TMP="$(mktemp -d)"
    chmod a+rx "$TMP" # apt's sandbox user reads a local .deb from here
  fi
  DL="$TMP/$name"
  "${CURL[@]}" -o "$DL" "$url" || die "download failed: $url"
  if [ "${want:0:1}" = @ ]; then
    sums="$("${CURL[@]}" "${want:1}")" || die "download failed: ${want:1}"
    # A checksums file lists "<hash>  <name>"; a single-file one may hold only the hash.
    want="$(awk -v n="$name" '$2 == n || $2 == "*" n { print $1; exit }' <<<"$sums")"
    if [ -z "$want" ] && [ "$(wc -w <<<"$sums")" -eq 1 ]; then want="$(tr -d '[:space:]' <<<"$sums")"; fi
    [ -n "$want" ] || die "no checksum for $name in ${3:1}"
  fi
  echo "$want  $DL" | "${algo}sum" -c --quiet - >/dev/null 2>&1 || die "checksum mismatch: $url"
}

# Link a binary onto the PATH; record a variable for later Actions steps when this runs in copilot-setup-steps.
link() { $SUDO ln -sf "$1" "/usr/local/bin/${2:-$(basename "$1")}"; }
github_env() { if [ -n "${GITHUB_ENV:-}" ]; then echo "$1=$2" >>"$GITHUB_ENV"; fi; }

APT_READY=0
# apt from the pinned snapshot: CA certificates first (the snapshot is HTTPS), then every source in $SOURCES points
# at it, and apt is not used unless every one does. Other source files (a GitHub runner's) are M07c's.
apt_ready() {
  [ "$APT_READY" = 1 ] && return 0
  [ "$HOST" = 1 ] && die "${CURRENT:-a tool} needs apt, which --host leaves alone: install it with the host's own tooling"
  [ -f "$SOURCES" ] || die "no $SOURCES: cannot point apt at the snapshot"
  export DEBIAN_FRONTEND=noninteractive
  $SUDO apt-get update -qq
  $SUDO apt-get install -y -qq --no-install-recommends ca-certificates >/dev/null
  $SUDO sed -i.harness-bak -E \
    "s#^URIs: https?://[^ ]*(archive|security|ports)\.ubuntu\.com/ubuntu(-ports)?/?#URIs: https://snapshot.ubuntu.com/ubuntu/${UBUNTU_SNAPSHOT}/#" \
    "$SOURCES"
  $SUDO rm -f "$SOURCES.harness-bak"
  grep -q '^URIs:' "$SOURCES" || die "$SOURCES names no URIs: cannot point apt at the snapshot"
  if grep '^URIs:' "$SOURCES" | grep -qv "https://snapshot.ubuntu.com/ubuntu/${UBUNTU_SNAPSHOT}/"; then
    die "an apt source in $SOURCES does not point at the snapshot ${UBUNTU_SNAPSHOT}: $(grep '^URIs:' "$SOURCES" | grep -v snapshot | head -1)"
  fi
  $SUDO apt-get update -qq
  $SUDO apt-get install -y -qq --no-install-recommends --allow-downgrades ca-certificates curl unzip xz-utils >/dev/null
  APT_READY=1
}
apt_install() {
  if [ "$HOST" = 1 ]; then
    echo "skip ${CURRENT}: an apt package; the host's own is used (--host)"
    return 0
  fi
  if dry; then echo "apt: $*"; return 0; fi
  apt_ready
  $SUDO apt-get install -y -qq --no-install-recommends --allow-downgrades "$@" >/dev/null
}
npm_install() {
  if dry; then echo "npm: $*"; return 0; fi
  $SUDO npm install -g --no-fund --no-audit "$@" >/dev/null
}
pip_install() {
  if dry; then echo "pip: $*"; return 0; fi
  [ -x "$PREFIX/venv/bin/pip" ] || $SUDO python3 -m venv "$PREFIX/venv"
  $SUDO "$PREFIX/venv/bin/pip" install -q --no-cache-dir --disable-pip-version-check "$@"
}

# ---- apt packages ----
install_git() { apt_install "git=$GIT_VERSION" openssh-client; }
install_python() { apt_install "python3.12=$PYTHON_VERSION" "python3.12-venv=$PYTHON_VERSION" python3-venv; }
install_xvfb() { apt_install "xvfb=$XVFB_VERSION" xauth; }
install_universal_ctags() { apt_install "universal-ctags=$UNIVERSAL_CTAGS_VERSION"; }
# gnucobol3 pulls libcob4 and the C toolchain cobc compiles through (gcc), at the snapshot's versions.
install_gnucobol() { apt_install "gnucobol3=$GNUCOBOL_VERSION"; }

# ---- languages and build tools ----
install_node() {
  local f base="https://nodejs.org/dist/v${NODE_VERSION}"
  f="node-v${NODE_VERSION}-linux-$(pick x64 arm64).tar.xz"
  get "$base/$f" sha256 "@$base/SHASUMS256.txt"
  dry && return 0
  $SUDO tar -xJf "$DL" -C /usr/local --strip-components=1 --exclude CHANGELOG.md --exclude LICENSE --exclude README.md
}
install_pnpm() { npm_install "pnpm@$PNPM_VERSION"; }
install_temurin() {
  local v="${TEMURIN_VERSION}" f base
  f="OpenJDK21U-jdk_$(pick x64 aarch64)_linux_hotspot_${v/+/_}.tar.gz"
  base="https://github.com/adoptium/temurin21-binaries/releases/download/jdk-${v/+/%2B}"
  get "$base/$f" sha256 "@$base/$f.sha256.txt"
  dry && return 0
  $SUDO mkdir -p /opt/java/temurin-21
  $SUDO tar -xzf "$DL" -C /opt/java/temurin-21 --strip-components=1
  for b in java javac jar jshell keytool javap jdeps jlink; do link /opt/java/temurin-21/bin/$b; done
  github_env JAVA_HOME /opt/java/temurin-21
}
install_maven() {
  local f="apache-maven-${MAVEN_VERSION}-bin.tar.gz"
  local base="https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/${MAVEN_VERSION}"
  get "$base/$f" sha512 "@$base/$f.sha512"
  dry && return 0
  $SUDO mkdir -p /opt/maven
  $SUDO tar -xzf "$DL" -C /opt/maven --strip-components=1
  link /opt/maven/bin/mvn
}
install_gradle() {
  local f="gradle-${GRADLE_VERSION}-bin.zip" base="https://services.gradle.org/distributions"
  get "$base/$f" sha256 "@$base/$f.sha256"
  dry && return 0
  $SUDO rm -rf "/opt/gradle-${GRADLE_VERSION}"
  $SUDO unzip -q "$DL" -d /opt
  link "/opt/gradle-${GRADLE_VERSION}/bin/gradle"
}
dotnet_sdk() {
  local v="$1" f base
  f="dotnet-sdk-${v}-linux-$(pick x64 arm64).tar.gz" base="https://builds.dotnet.microsoft.com/dotnet/Sdk/${v}"
  get "$base/$f" sha512 "@$base/$f.sha512"
  dry && return 0
  apt_install libicu74 libssl3t64 libgssapi-krb5-2 zlib1g
  $SUDO mkdir -p /usr/share/dotnet
  $SUDO tar -xzf "$DL" -C /usr/share/dotnet
  link /usr/share/dotnet/dotnet
  github_env DOTNET_ROOT /usr/share/dotnet
}
install_dotnet_sdk_8() { dotnet_sdk "$DOTNET_SDK_8_VERSION"; }
install_dotnet_sdk_10() { dotnet_sdk "$DOTNET_SDK_10_VERSION"; }

# ---- browsers and diagrams ----
install_playwright() {
  npm_install "playwright@$PLAYWRIGHT_VERSION"
  dry && return 0
  apt_ready
  # Browsers in a shared path the runner user reads; their system libraries from the snapshot.
  $SUDO env PLAYWRIGHT_BROWSERS_PATH=/opt/ms-playwright playwright install --with-deps >/dev/null
  $SUDO chmod -R a+rX /opt/ms-playwright
  github_env PLAYWRIGHT_BROWSERS_PATH /opt/ms-playwright
}
install_drawio() {
  local f base="https://github.com/jgraph/drawio-desktop/releases/download/v${DRAWIO_VERSION}"
  f="drawio-$(pick amd64 arm64)-${DRAWIO_VERSION}.deb"
  get "$base/$f" sha256 "@$base/Files-SHA256-Hashes.txt"
  dry && return 0
  chmod a+r "$DL"
  apt_install "$DL"
}

# ---- security and analysis ----
release_binary() { # <url> <checksums-url> <binary-in-archive> <name>
  get "$1" sha256 "@$2"
  dry && return 0
  $SUDO tar -xzf "$DL" -C "$TMP" "$3"
  $SUDO install -m 0755 "$TMP/$3" "/usr/local/bin/$4"
}
install_semgrep() {
  pip_install "semgrep==$SEMGREP_VERSION"
  dry && return 0
  link "$PREFIX/venv/bin/semgrep"
}
install_sqlglot() { pip_install "sqlglot==$SQLGLOT_VERSION"; }
install_gitleaks() {
  local base="https://github.com/gitleaks/gitleaks/releases/download/v${GITLEAKS_VERSION}"
  release_binary "$base/gitleaks_${GITLEAKS_VERSION}_linux_$(pick x64 arm64).tar.gz" "$base/gitleaks_${GITLEAKS_VERSION}_checksums.txt" gitleaks gitleaks
}
install_syft() {
  local base="https://github.com/anchore/syft/releases/download/v${SYFT_VERSION}"
  release_binary "$base/syft_${SYFT_VERSION}_linux_$(pick amd64 arm64).tar.gz" "$base/syft_${SYFT_VERSION}_checksums.txt" syft syft
}
install_grype() {
  local base="https://github.com/anchore/grype/releases/download/v${GRYPE_VERSION}"
  release_binary "$base/grype_${GRYPE_VERSION}_linux_$(pick amd64 arm64).tar.gz" "$base/grype_${GRYPE_VERSION}_checksums.txt" grype grype
}
install_scc() {
  local base="https://github.com/boyter/scc/releases/download/v${SCC_VERSION}"
  release_binary "$base/scc_Linux_$(pick x86_64 arm64).tar.gz" "$base/checksums.txt" scc scc
}
install_tree_sitter() {
  local sum
  sum="$(pick "$TREE_SITTER_SHA256_AMD64" "$TREE_SITTER_SHA256_ARM64")"
  get "https://github.com/tree-sitter/tree-sitter/releases/download/v${TREE_SITTER_VERSION}/tree-sitter-linux-$(pick x64 arm64).gz" sha256 "$sum"
  dry && return 0
  gunzip -c "$DL" | $SUDO tee /usr/local/bin/tree-sitter >/dev/null
  $SUDO chmod 0755 /usr/local/bin/tree-sitter
}

# ---- agents and GitHub ----
install_gh() {
  local d base="https://github.com/cli/cli/releases/download/v${GH_VERSION}"
  d="gh_${GH_VERSION}_linux_$(pick amd64 arm64)"
  get "$base/$d.tar.gz" sha256 "@$base/gh_${GH_VERSION}_checksums.txt"
  dry && return 0
  $SUDO tar -xzf "$DL" -C "$TMP"
  $SUDO install -m 0755 "$TMP/$d/bin/gh" /usr/local/bin/gh
}
install_claude_code() { npm_install "@anthropic-ai/claude-code@$CLAUDE_CODE_VERSION"; }
install_copilot_cli() { npm_install "@github/copilot@$COPILOT_CLI_VERSION"; }

# ---- optional: CodeQL (reverseEngineering.codeql, Q-05) ----
install_codeql() {
  local f base="https://github.com/github/codeql-action/releases/download/codeql-bundle-v${CODEQL_VERSION}"
  f="codeql-bundle-$(pick linux64 linux-arm64).tar.gz"
  get "$base/$f" sha256 "@$base/$f.checksum.txt"
  dry && return 0
  $SUDO mkdir -p /opt/codeql
  $SUDO tar -xzf "$DL" -C /opt/codeql --strip-components=1
  link /opt/codeql/codeql
}

# ---- optional: runner-mobile (SDD 08 §1 and §6), amd64 only ----
# Google publishes the Android platform-tools and emulator, and Flutter its SDK, for Linux x64 only, so the layer is
# refused on arm64 (Docker Desktop on Apple silicon builds it with --platform linux/amd64, under emulation).
ANDROID_SDK=/opt/android-sdk
AREPO=https://dl.google.com/android/repository
amd64_only() {
  [ "$ARCH" = amd64 ] || die "${CURRENT:-a mobile tool} is amd64 only (runner-mobile): the Android platform-tools, emulator and Flutter have no Linux arm64 build"
}
gem_install() {
  if dry; then echo "gem: $*"; return 0; fi
  $SUDO gem install --no-document "$@" >/dev/null
}
# android_package <url> <sha256> <path in the SDK>: Google's zips hold one directory, which becomes that path.
android_package() {
  local top
  get "$1" sha256 "$2"
  dry && return 0
  rm -rf "$TMP/unzip" && mkdir "$TMP/unzip"
  unzip -q "$DL" -d "$TMP/unzip"
  top="$(ls -A "$TMP/unzip")"
  [ "$(wc -l <<<"$top")" -eq 1 ] && [ -d "$TMP/unzip/$top" ] || die "$(basename "$1") does not hold one directory"
  $SUDO mkdir -p "$(dirname "$ANDROID_SDK/$3")"
  $SUDO rm -rf "${ANDROID_SDK:?}/$3"
  $SUDO mv "$TMP/unzip/$top" "$ANDROID_SDK/$3"
  github_env ANDROID_HOME "$ANDROID_SDK"
}
install_ruby() {
  amd64_only
  # fastlane's Ruby (>= 3.2), with the headers and compiler its gems' native extensions build with (purged after
  # fastlane's gem install).
  apt_install "ruby3.2=$RUBY_VERSION" "libruby3.2=$RUBY_VERSION" "ruby3.2-dev=$RUBY_VERSION" ruby build-essential
}
install_android_cmdline_tools() {
  amd64_only
  android_package "$AREPO/commandlinetools-linux-${ANDROID_CMDLINE_TOOLS_BUILD}_latest.zip" \
    "$ANDROID_CMDLINE_TOOLS_SHA256" cmdline-tools/latest
}
install_android_platform_tools() {
  amd64_only
  android_package "$AREPO/platform-tools_r${ANDROID_PLATFORM_TOOLS_VERSION}-linux.zip" \
    "$ANDROID_PLATFORM_TOOLS_SHA256" platform-tools
}
install_android_build_tools() {
  amd64_only
  # Google names 36.1.0 r36.1, 37.0.0 r37 and 35.0.1 r35.0.1.
  local r="${ANDROID_BUILD_TOOLS_VERSION%.0}"
  r="${r%.0}"
  android_package "$AREPO/build-tools_r${r}_linux.zip" "$ANDROID_BUILD_TOOLS_SHA256" \
    "build-tools/$ANDROID_BUILD_TOOLS_VERSION"
}
install_android_platform() {
  amd64_only
  android_package "$AREPO/platform-${ANDROID_PLATFORM_VERSION}_r$(printf %02d "$ANDROID_PLATFORM_REVISION").zip" \
    "$ANDROID_PLATFORM_SHA256" "platforms/android-$ANDROID_PLATFORM_VERSION"
}
install_android_emulator() {
  amd64_only
  android_package "$AREPO/emulator-linux_x64-${ANDROID_EMULATOR_BUILD}.zip" "$ANDROID_EMULATOR_SHA256" emulator
  dry && return 0
  # The libraries the emulator loads; it runs only where /dev/kvm is passed in (not on Docker Desktop for macOS).
  apt_install libx11-6 libpulse0 libnss3 libxcomposite1 libxcursor1 libxdamage1 libxi6 libxtst6 libasound2t64 libgl1 libxkbfile1 libdbus-1-3
}
install_flutter() {
  amd64_only
  get "https://storage.googleapis.com/flutter_infra_release/releases/stable/linux/flutter_linux_${FLUTTER_VERSION}-stable.tar.xz" \
    sha256 "$FLUTTER_SHA256"
  dry && return 0
  $SUDO rm -rf /opt/flutter
  $SUDO tar -xJf "$DL" -C /opt
  # The SDK is a git checkout: git refuses it to a user who does not own it unless it is marked safe.
  $SUDO git config --system --add safe.directory /opt/flutter
  link /opt/flutter/bin/flutter
  link /opt/flutter/bin/dart
}
install_maestro() {
  amd64_only
  local base="https://github.com/mobile-dev-inc/maestro/releases/download/cli-${MAESTRO_VERSION}"
  get "$base/maestro.zip" sha256 "@$base/checksums_sha256.txt"
  dry && return 0
  $SUDO rm -rf /opt/maestro
  $SUDO unzip -q "$DL" -d /opt
  link /opt/maestro/bin/maestro
}
install_fastlane() {
  amd64_only
  gem_install "fastlane:$FASTLANE_VERSION"
  # The compiler and Ruby's headers only built the gems' native extensions (nkf): fastlane runs without them, and
  # autoremove takes the toolchain they pulled in (gcc, make, libc6-dev), which runner-core does not carry either.
  if dry; then echo "apt purge: --auto-remove build-essential ruby3.2-dev"; return 0; fi
  $SUDO apt-get purge -y -qq --auto-remove build-essential ruby3.2-dev >/dev/null
}

# ---- main ----
# A layer's tools, in install order, from docker/tools.json (grep and sed: a bare runner has no jq; the unit tests
# check the result against the manifest, so a tool this misses fails them). A tool built from this repository
# ("built": the Dockerfile copies it from its harness-build stage) is never installed here.
layer_tools() {
  tr -d '\n' <"$TOOLS_JSON" | grep -o '{[^{}]*"layer": *"'"$1"'"[^{}]*}' | grep -v '"built":' |
    sed -E 's/.*"name": *"([^"]+)".*/\1/'
}

CURRENT=""
while [ $# -gt 0 ]; do
  case "$1" in
    --host) HOST=1 ;;
    --dry-run) DRY=1 ;;
    *) break ;;
  esac
  shift
done
[ $# -gt 0 ] || die "usage: install-tools.sh [--host] [--dry-run] <layer|tool>..."
todo=()
for arg in "$@"; do
  case "$arg" in
    core | codeql | mobile)
      found=0
      while IFS= read -r name; do
        todo+=("$name")
        found=1
      done < <(layer_tools "$arg")
      [ "$found" = 1 ] || die "layer $arg has no tools in $TOOLS_JSON"
      ;;
    *) todo+=("$arg") ;;
  esac
done
mobile=" $(layer_tools mobile | tr '\n' ' ') "
for t in "${todo[@]}"; do
  declare -F "install_${t//-/_}" >/dev/null || die "unknown tool or layer: $t"
  # A mobile tool stops the run before anything is installed: runner-mobile is amd64 only, and not for --host.
  if [[ "$mobile" == *" $t "* ]]; then
    CURRENT="$t"
    amd64_only
    [ "$HOST" = 0 ] || die "$t is runner-mobile's: --host does not install the mobile layer"
  fi
done
for t in "${todo[@]}"; do
  CURRENT="$t"
  if dry; then echo "tool: $t"; else echo "install-tools: $t"; fi
  "install_${t//-/_}"
done
