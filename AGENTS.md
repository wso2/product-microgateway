# AGENTS.md

## What this repo is

**Choreo Connect** (a.k.a. product-microgateway) — a cloud-native, Envoy-powered API gateway proxy. In the Choreo platform it runs as the `choreo-connect` K8s service (area `02-apim`). **Only the `choreo` branch is maintained here** (the workspace tracks this single branch, not `main`/`dev`/`stage`/`prod` worktrees).

It exposes microservices as managed APIs (from an OpenAPI definition, or fronting WSO2 API Manager) and applies QoS: security, rate limiting, analytics, and mediation.

## Architecture — the four runtime components

A request flows: **client → Router → Enforcer → backend**, with the **Adapter** configuring Router and Enforcer at runtime over xDS/gRPC.

| Component | Module | Language | Role |
|-----------|--------|----------|------|
| **Router** | `router/` | Envoy + Lua/WASM filters | Client-facing data plane. Routes requests; calls Enforcer via ext-authz for each request. Built only in the `Release` Maven profile (rarely changes). |
| **Enforcer** | `enforcer-parent/` (`enforcer` + `commons`) | Java 11 | Intercepts requests from the Router (gRPC ext-authz). Applies auth (JWT/OAuth/API key), subscription validation, rate-limit checks, CORS, and publishes analytics. |
| **Adapter** | `adapter/` | Go 1.24 | The control component. Reads API configs + events (from APIM eventhub / control plane), converts OpenAPI → Envoy config, and pushes to Router and Enforcer as an **xDS management server (gRPC, go-control-plane)**. Entry point: `adapter/cmd/adapter/main.go` → `internal/adapter/adapter.go` `Run()`. |
| **Rate Limiter** | `rate-limiter/` | Go (forked `envoyproxy/ratelimit`) | Global rate-limiting service that the Router/Enforcer consult. |

Supporting modules:
- `mcp/` — `transform-mcp` Go service (MCP / Model Context Protocol transform server, gin-based).
- `api/proto/` — Protobuf definitions (`wso2.discovery.*`) shared as the xDS contract between Adapter (Go) and Enforcer (Java). Regenerate Go stubs with `api/protogen.sh`.
- `envoy-filters/` — custom Envoy filters.
- `distribution/` — assembles the runnable `choreo-connect-<version>.zip` (docker-compose + configs).
- `integration/` — TestNG integration tests (`test-integration`) and `mock-backend-server`.

The Adapter is the brain: it owns the xDS snapshot caches (`adapter/internal/discovery/xds/`) and registers many discovery services in `runManagementServer` (ADS for Router config; API/Config/Subscription/KeyManager/Throttle discovery for the Enforcer).

## Build & run

Multi-module Maven build orchestrates Go and Java together (Go modules are built via `exec-maven-plugin` / `adapter/scripts/build.sh`, and docker images via the docker-maven-plugin).

**Prerequisites:** docker & docker-compose, JDK 11 (required — enforcer pom targets Java 11), Maven 3.6, Go 1.24.

```bash
# First time only after a previous build: remove stale containers so docker-maven-plugin can recreate images
sh remove-containers.sh

# Full build (default profile builds all modules EXCEPT router)
mvn clean install            # MUST run with Java 11

# Release build (also builds the router module)
mvn clean install -P Release

# Run the assembled distribution
cd distribution/target && unzip choreo-connect-<version>.zip
cd choreo-connect-<version>/docker-compose/choreo-connect && docker-compose up
```

Rebuilding a single component: `sh remove-containers.sh` (edit it if you want to keep other containers up), then `mvn clean install` inside that module's directory, then `docker-compose up` from the distribution again.

Running the Adapter as a standalone Go binary requires `export MGW_HOME=<dir containing configs>` (defaults to `adapter/../resources` in build.sh).

Multi-arch images: `./build-ubuntu-multi-platform-images.sh` (uses docker buildx, `linux/amd64,linux/arm64`).

## Tests

**Adapter (Go) unit tests** — `adapter/scripts/build.sh` is the canonical runner; it exports many `cc_*` / `CC_*` env vars the tests depend on, then runs:
```bash
cd adapter
go test -race -coverprofile=./target/coverage.txt -covermode=atomic ./...   # all
go test -race ./internal/oasparser/...                                       # single package
go test -race -run TestName ./internal/oasparser/...                         # single test
```
The same script also runs `golint -set_exit_status ./...` and `go vet ./...` — match those before pushing.

**MCP (Go):** `cd mcp && make test` (runs `go test -v -race`), plus `make lint` / `make vet` / `make fmt`.

**Integration tests (Java, TestNG)** — `cd integration/test-integration && mvn clean install`. Two modes, each driven by a `testng-*.xml` suite:
- **Standalone** (`testng-cc-standalone.xml`): CC fronts OpenAPI definitions directly. To add a case: drop the OpenAPI into `src/test/resources/openAPIs`, deploy it in a `setup/standalone/Cc*.java` class, write the test under `testcases/standalone`, and register it in the testng XML.
- **WithAPIM** (`testng-cc-with-apim.xml`): CC fronts WSO2 API Manager. Test classes extend `ApimBaseTest`; APIs/Apps/Subs are seeded from `src/test/resources/apimApisAppsSubs`. Two groups: `apis-apps-subs-pulled-at-startup` (CC pulls config at boot) and `all-events-received-via-eventhub` (config delivered via eventhub events).
- See `integration/test-integration/readme.md` for the full recipe, including how to avoid restarting API Manager on every run.

## Key conventions

- **xDS contract changes** touch two languages: edit the `.proto` in `api/proto/wso2/discovery/...`, regenerate Go stubs (`api/protogen.sh`), and keep the Java enforcer's generated discovery services in sync. The Adapter and Enforcer must agree on the proto.
- **Config is env-var overridable.** Runtime config (`config.toml`) values can be overridden by `cc_<section>_<key>` / `CC_<Section>_<Key>` environment variables — heavily used in tests and docker-compose. `admin:admin` is the default adapter credential.
- **Java targets JDK 11** specifically (not newer) — building with another JDK will fail the enforcer module.
- The **router module is excluded from the default Maven profile** — only build it with `-P Release` when Envoy config/filters actually change.
