# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

OITC Access Control System (ACS) Broker — a Java service that listens for events from FEIG RFID readers, validates transponder credentials against LDAP, and publishes access results to MQTT topics for downstream services. Metrics are exposed via a Prometheus HTTP endpoint.

## Toolchain

- Java 25, Maven 3.9.9 (managed via `.tool-versions` / asdf)
- Pre-commit hooks managed via `.pre-commit-config.yaml`

## First-time setup

The FEIG proprietary JARs must be installed into the local Maven repository before any build will succeed:

```bash
bash libs/installToMavenRepo.bash
```

The native shared libraries (`.so` files in `libs/`) also need versioned symlinks created on the build host:

```bash
cd libs && bash createSymlinks.bash
```

## Build and test

All Maven commands run from the `acs-broker/` subdirectory:

```bash
# Compile
cd acs-broker && mvn compile

# Package (produces JAR + copies runtime deps to target/dependency/)
cd acs-broker && mvn package

# Run all tests
cd acs-broker && mvn test

# Run a single test class
cd acs-broker && mvn test -Dtest=TransponderValidatorTest
```

## Running the application

The JVM needs access to the FEIG native libraries at runtime:

```bash
java -Djava.library.path=libs \
     --enable-native-access=ALL-UNNAMED \
     -cp "acs-broker/target/acs-broker-*.jar:acs-broker/target/dependency/*" \
     de.oberdorf_itc.acs.AcsBroker
```

All configuration is supplied via environment variables. The `.envrc` file (loaded automatically by direnv) provides a complete set of values for local development. Secrets are kept in `.secrets/` and referenced via `*_FILE` env var variants (e.g. `MQTT_PASSWORD_FILE`, `LDAP_PASSWORD_FILE`).

## Architecture

```
AcsBroker (main)
├── PropertiesFromEnvironment   – reads env vars + _FILE secret pattern into a typed Map
├── initializePrometheusExporter – JVM + custom counters on PROMETHEUS_LISTENER_ADDR:PORT
├── MqttPublisher               – connects to MQTT broker (TCP or TLS/SSL)
│   ├── publishStatus()         – topic: {PREFIX}/entrypoints/{ip}/status  → StatusEvent JSON
│   └── publishAccess()         – topic: {PREFIX}/entrypoints/{ip}/access  → AccessEvent JSON
├── [TODO] ReaderMessageServer  – FEIG reader notification listener (stub)
├── [TODO] MessageHandler       – handles per-reader events (stub)
├── [TODO] TransponderValidator – LDAP lookup + access decision (stub)
└── [TODO] LdapService / LdapTransponder / LdapUser – LDAP data layer (stubs)
```

**Configuration** (`PropertiesFromEnvironment`): All env vars are declared with name, type (`string`/`int`/`boolean`), and optional default. Variables ending with `_FILE` are automatically resolved to their file contents (used for passwords/certs). The resulting `Map<String, Object>` is passed into every component that needs it.

**JSON serialization** (`JsonUtil`): Manual string building — no external JSON library. Field order is fixed and matches the MQTT message schema.

**Prometheus counters** (all registered in `AcsBroker.initializePrometheusExporter`):
- `mqtt_messages_sent` — total MQTT publishes
- `mqtt_messages_sent_access_granted` / `_denied` — labelled by `entrypoint_ip`
- `acs_access_granted` / `acs_access_denied` — labelled by `entrypoint_ip`
- `reader_notification_events` — labelled by `entrypoint_ip`

## Key environment variables

| Variable | Default | Purpose |
|---|---|---|
| `FEIG_LISTENER_ADDR` / `FEIG_LISTENER_PORT` | `0.0.0.0` / `10005` | Where to accept FEIG reader notifications |
| `MQTT_SERVER` / `MQTT_PORT` | `localhost` / `1883` | MQTT broker |
| `MQTT_TLS` / `MQTT_CACERT_FILE` / `MQTT_TLS_INSECURE` | `false` / system CA / `false` | TLS settings |
| `MQTT_TOPIC_PREFIX` | — | Required; all topics are `{PREFIX}/entrypoints/{ip}/status\|access` |
| `LDAP_SERVER` / `LDAP_PORT` / `LDAP_TLS` | `localhost` / `389` / `false` | LDAP connection |
| `LDAP_BASEDN_*` / `LDAP_FILTER_*` | see code | Search bases and filter templates for entry points, transponders, users, roles, rules |
| `PROMETHEUS_LISTENER_ADDR` / `PROMETHEUS_LISTENER_PORT` | `0.0.0.0` / `8080` | Metrics endpoint |
| `TZ` | `UTC` | Timezone label in `service_info` metric |
