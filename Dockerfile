# syntax=docker/dockerfile:1.7
# Java 22+ is required: run.sh uses multi-file source launch (JEP 458) for profiles/*.java
FROM amazoncorretto:25-al2023

ARG TARGETARCH
ARG DUCKDB_VERSION=1.5.1
ARG PLANETILER_VERSION=0.10.2
ARG S5CMD_VERSION=2.3.0
ARG TILES_VERSION=dev

LABEL org.opencontainers.image.title="overture-tiles"

ENV TILES_VERSION=${TILES_VERSION}

# Explicitly distrusted roots; update-ca-trust propagates them to the system bundle and Java cacerts
COPY certs/blocklist/ /etc/pki/ca-trust/source/blocklist/

RUN dnf upgrade -y ca-certificates && update-ca-trust extract && \
    dnf install -y gzip tar unzip && dnf clean all && \
    \
    # Install s5cmd for S3 operations (parallel transfers; prefer over AWS CLI for data movement)
    S5CMD_ARCH=$([ "${TARGETARCH}" = "arm64" ] && echo "Linux-arm64" || echo "Linux-64bit") && \
    curl -fsSL "https://github.com/peak/s5cmd/releases/download/v${S5CMD_VERSION}/s5cmd_${S5CMD_VERSION}_${S5CMD_ARCH}.tar.gz" \
        -o s5cmd.tar.gz && \
    tar -xzf s5cmd.tar.gz s5cmd && \
    mv s5cmd /usr/local/bin/s5cmd && \
    rm s5cmd.tar.gz && \
    \
    # Install DuckDB CLI (bbox filtering of GeoParquet)
    curl -fsSL "https://github.com/duckdb/duckdb/releases/download/v${DUCKDB_VERSION}/duckdb_cli-linux-${TARGETARCH}.zip" \
        -o duckdb_cli.zip && \
    unzip duckdb_cli.zip -d /usr/local/bin/ && \
    rm duckdb_cli.zip && \
    \
    # Download Planetiler JAR (tile generation)
    curl -fsSL "https://github.com/onthegomap/planetiler/releases/download/v${PLANETILER_VERSION}/planetiler.jar" \
        -o /planetiler.jar

# Copy profiles
COPY profiles /profiles

# Copy scripts
COPY run.sh /run.sh
COPY bbox.sh /bbox.sh

ENTRYPOINT ["bash", "/run.sh"]
