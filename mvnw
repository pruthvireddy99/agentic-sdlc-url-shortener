#!/usr/bin/env sh
set -eu

if command -v mvn >/dev/null 2>&1; then
  exec mvn "$@"
fi

MAVEN_VERSION=3.9.11
MAVEN_HOME="$HOME/.m2/wrapper/dists/apache-maven-${MAVEN_VERSION}"
MAVEN_BIN="$MAVEN_HOME/apache-maven-${MAVEN_VERSION}/bin/mvn"

if [ ! -x "$MAVEN_BIN" ]; then
  TMP_DIR="${MAVEN_HOME}.tmp"
  mkdir -p "$TMP_DIR"
  ARCHIVE="$TMP_DIR/apache-maven.tar.gz"
  URL="https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/${MAVEN_VERSION}/apache-maven-${MAVEN_VERSION}-bin.tar.gz"
  echo "Downloading Maven ${MAVEN_VERSION}..." >&2
  curl -fsSL "$URL" -o "$ARCHIVE"
  tar -xzf "$ARCHIVE" -C "$TMP_DIR"
  rm -f "$ARCHIVE"
  mkdir -p "$MAVEN_HOME"
  mv "$TMP_DIR/apache-maven-${MAVEN_VERSION}" "$MAVEN_HOME/"
  rmdir "$TMP_DIR" 2>/dev/null || true
fi

exec "$MAVEN_BIN" "$@"
