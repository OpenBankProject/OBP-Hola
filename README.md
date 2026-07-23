# OBP-Hola

The Open Bank Project *Hola App* is a reference implementation of the OAuth2 authentication and consent flow. It demonstrates and tests OBP authentication, consent creation and data access via OBP API. It supports UK, Berlin Group, and OBP styles. Hola is written in Java / Spring Boot.

Hola App supports multiple OIDC providers:
- **OBP-OIDC** (default) — the Open Bank Project's built-in OIDC provider
- **Ory Hydra** — via [Ory Hydra](https://www.ory.sh/hydra) and [OBP Hydra Identity Provider](https://github.com/OpenBankProject/OBP-Hydra-Identity-Provider)

A working Hola App setup can be used to drive automatic tests using [OBP Selenium](https://github.com/OpenBankProject/OBP-Selenium).

## Quick start (local development)

Configuration is composed from small, single-purpose Spring profiles. Each one is a delta
on `application.properties`, and a later profile overrides an earlier one:

| Profile | Tracked? | What it changes |
|---|---|---|
| *(none)* | yes | `application.properties` — the base: OBP-OIDC on 9000, OBP-API over plain HTTP, port 48123 |
| `keycloak` | yes | use Keycloak as the OIDC provider instead of OBP-OIDC |
| `mtls` | yes | talk to OBP-API over mutual TLS (https + client certificate) |
| `local` | **no — gitignored** | your ports, URLs and secrets |

```sh
./build_and_run.sh --spring.profiles.active=keycloak,mtls,local
```

Mix them to match your setup: drop `mtls` for a plain-HTTP OBP-API, drop `keycloak` to stay
on OBP-OIDC. `local` is optional — Spring skips a profile whose file is absent, so the
command above is always safe.

The tracked profiles contain no secrets and nothing machine-specific, so **you should never
need to edit them** — put your own values in `local` (step 2).

| Setting | Default with `keycloak,mtls` | Change it via |
|---|---|---|
| Hola App port | `48123` (from the base) | `server.port` in `local` |
| OBP-API | `https://localhost:8080` | `obp.base_url` |
| OIDC provider | `http://localhost:7070/realms/master` | `oauth2.public_url` |

**These ports are a reference, not a project convention** — expect to override at least the
OIDC one. The single hard requirement is that `oauth2.public_url` matches OBP-API's own
Keycloak configuration (`oauth2.keycloak.host`, `oauth2.keycloak.well_known`, and the entry
in `oauth2.jwk_set.url`); otherwise OBP-API rejects the tokens this app obtains.

Any JDK from 11 upwards works.

### 1. Start the dependencies

```sh
# OIDC provider, e.g. Keycloak
docker start <your-keycloak-container>

# OBP-API, from the OBP-API repo. --mtls serves HTTPS with mutual TLS on 8080.
./flushall_fast_build_and_run.sh --mtls
```

Running OBP-API without mTLS is fine too — just leave the `mtls` profile out.

### 2. Create your local profile

```sh
cd src/main/resources
cp application-local.properties.example application-local.properties
```

Edit the copy — it is gitignored, so your ports and secrets can never be committed. A
typical one is only a few lines:

```properties
oauth2.public_url=http://localhost:7787/realms/master
oauth2.client_secret=<Keycloak: Clients > open-bank-project > Credentials>
server.port=8081
```

`oauth2.redirect_uri` follows `server.port` automatically, but whatever it resolves to must
be registered as a valid redirect URI on the Keycloak client.

Environment variables work too — `OBP_BASE_URL`, `MTLS_KEYSTORE_PATH`, `KEYCLOAK_PUBLIC_URL`,
`KEYCLOAK_CLIENT_SECRET`, and so on. **They outrank `application-local.properties`**: the
precedence is command line › environment › property files, so "local wins" is only true
among files. A stale `export` in your shell will silently override your local profile.

### 3. Build and run

```sh
./build_and_run.sh --spring.profiles.active=keycloak,mtls,local
```

`build_and_run.sh` waits for OBP-API and the OIDC provider first, and tells you what to start
if either is missing. `SKIP_HEALTH_CHECKS=true` bypasses the wait.

Then open the app on whichever port you configured (`http://localhost:8081` above).

### mTLS between Hola and OBP-API

With the `mtls` profile active, Hola presents `src/main/resources/cert/hola-client.p12`
(`CN=obp-hola, OU=TPP, O=Hola Ltd`) and trusts OBP-API's server certificate through
`hola-truststore.p12`. Both are referenced as `classpath:` so they work on any machine, and both
are committed for local development.

Both come from OBP-API's development CA, which is the root OBP-API's own truststore trusts:

```sh
./scripts/generate_dev_certs.sh                       # finds a sibling OBP-API checkout
OBP_API_HOME=~/src/OBP-API ./scripts/generate_dev_certs.sh
```

The script generates Hola's private key here and has that CA sign it — the arrangement being
modelled, where the TPP holds its own key and the bank's CA issues the certificate. Hola is never
handed a key it did not generate. Re-run it after OBP-API regenerates its set
(`./scripts/generate_dev_certs.sh` there), since a new CA invalidates the old signature.

Clearing `mtls.keyStore.path` / `mtls.trustStore.path`, or simply not activating the `mtls`
profile, falls back to a plain HTTP client.

### Troubleshooting

| Symptom | Cause |
|---|---|
| `Cannot reach the OIDC provider at ...` at startup | The provider is not running, or `KEYCLOAK_PUBLIC_URL` has the wrong port/realm. The discovery document is fetched eagerly, so the app cannot start without it. |
| `OBP-API is not reachable at localhost:8080` | OBP-API is not started. The check is a TCP probe on purpose: an HTTP probe cannot succeed against mTLS, which rejects the handshake without a client certificate. |
| Token exchange fails with `invalid_client` | `KEYCLOAK_CLIENT_SECRET` is unset or stale. |
| OBP-API returns 401 for a token Keycloak issued happily | `KEYCLOAK_PUBLIC_URL` and OBP-API's Keycloak config disagree, so OBP-API cannot validate the signature. |

## Build with maven

Check out the code from this repository and build it by running `mvn clean package` inside the main folder.

The resulting JAR file of Hola App will be in the `target` folder.

## Prepare truststore

> For local development against a local OBP-API, use `./scripts/generate_dev_certs.sh` instead —
> see "mTLS between Hola and OBP-API" above. The manual steps below are for pointing Hola at a
> remote deployment, where the certificates are not yours to generate.

Assuming OBP-API server URL is: `apisandbox.openbankproject.com`

Assuming Hydra server URL is: `oauth2.openbankproject.com`

- retrieve OBP API and Hydra server certificates:

    `openssl s_client -servername apisandbox.openbankproject.com -connect apisandbox.openbankproject.com:443 </dev/null 2>/dev/null | openssl x509 -inform PEM -outform DER -out obp-api.cer`

    `openssl s_client -servername oauth2.openbankproject.com -connect oauth2.openbankproject.com:443 </dev/null 2>/dev/null | openssl x509 -inform PEM -outform DER -out hydra.cer`

- import both certificates to truststore.jks:

    `keytool -import -alias api -keystore truststore.jks -file obp-api.cer`
    
    `keytool -import -alias hydra -keystore truststore.jks -file hydra.cer`
    
## Prepare keystore

If mTLS is enabled on the OBP API instance, the client key needs to be signed by OBP API client CA. Else, any self-signed certificate will do. Continuing assuming you have `client.key` and `client.crt`:

- convert client key and cert to client-cert.p12:
  
    `openssl pkcs12 -export -in client.crt -inkey client.key -certfile user.crt -out client-cert.p12`

- import client-cert.p12 to keystore.jks:

    `keytool -importkeystore -srckeystore client-cert.p12 -srcstoretype pkcs12 -destkeystore keystore.jks`

## Adjust application.properties file

Create `application.properties` according to `application.properties.example`:

* `oauth2.provider` selects the OIDC provider. Set to `obp-oidc` (default) or `hydra`.
* `oauth2.public_url` is the URL of the OAuth2 server. For OBP-OIDC this defaults to `http://localhost:9000/obp-oidc`.
* `obp.base_url` is the main URL of the OBP instance.
* Fill in the locations and passphrases of the previously created keystore and truststore into `mtls.keyStore` and `mtls.trustStore` props
* Register a new API key on the OBP instance, e.g. https://apisandbox.openbankproject.com/consumer-registration and copy and paste all props below "OAuth2:" into  `application.properties`:
  * `oauth2.client_id`
  * `oauth2.redirect_uri`
  * `oauth2.client_scope`
  * `oauth2.client_secret` (for OBP-OIDC or Hydra with client_secret)
  * `oauth2.jws_alg` (Hydra only, for private_key_jwt)
  * `oauth2.jwk_private_key` (Hydra only, for private_key_jwt)
* All other props can be left at default values.

### OBP-OIDC (default)

OBP-OIDC uses `client_secret` authentication and supports the `code` response type. No JWK private key configuration is needed:

```properties
oauth2.provider=obp-oidc
oauth2.public_url=http://localhost:9000/obp-oidc
oauth2.client_id=your_client_id
oauth2.client_secret=your_client_secret
```

### Hydra

To use Hydra, set the provider and configure either `client_secret` or `jwk_private_key` (not both):

```properties
oauth2.provider=hydra
oauth2.public_url=https://oauth2.openbankproject.com/hydra-public
oauth2.client_id=your_client_id
oauth2.client_secret=your_client_secret
```

## Build and Run

The quickest way to build and run:

```bash
./build_and_run.sh
```

This will build with Maven and start the app. You can pass Spring Boot arguments, e.g.:

```bash
./build_and_run.sh --oauth2.provider=hydra --oauth2.public_url=https://oauth2.example.com
```

Or manually:

```bash
mvn clean package -DskipTests
java -jar target/obp-hola-app-0.0.29-SNAPSHOT.jar
```

While the app is running, point your browser to the configured port on localhost (e.g. `http://localhost:8087`) to start the consent flow.

## Build and Run in Docker/Kubernetes

The included Dockerfile will build Hola using Maven and create an image.

Please see `application.properties.docker` for all vars to pass to the container for configuration. Key environment variables include:

* `OAUTH2_PROVIDER` — `obp-oidc` (default) or `hydra`
* `OAUTH_2_PUBLIC_URL` — URL of the OIDC provider
* `OAUTH2_CLIENT_ID`, `OAUTH2_CLIENT_SECRET` — client credentials

## Screenshots of the app

### Landing page
![alt text](https://github.com/OpenBankProject/OBP-Hola/blob/a124b6ace05e35e763e292144c507f2caa675159/src/main/resources/static.screenshots/index.png?raw=true)
### Berlin Group Flow
![alt text](https://github.com/OpenBankProject/OBP-Hola/blob/a124b6ace05e35e763e292144c507f2caa675159/src/main/resources/static.screenshots/index_bg.png?raw=true)
### Consents
![alt text](https://github.com/OpenBankProject/OBP-Hola/blob/a124b6ace05e35e763e292144c507f2caa675159/src/main/resources/static.screenshots/consent.png?raw=true)
### Get Accounts, Balances and Transactions
![alt text](https://github.com/OpenBankProject/OBP-Hola/blob/a124b6ace05e35e763e292144c507f2caa675159/src/main/resources/static.screenshots/accounts.png?raw=true)

Copyright TESOBE GmbH 2020
