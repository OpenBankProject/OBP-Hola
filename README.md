# OBP-Hola

The Open Bank Project *Hola App* is a reference implementation of the OAuth2 authentication and consent flow. It demonstrates and tests OBP authentication, consent creation and data access via OBP API. It supports UK, Berlin Group, and OBP styles. Hola is written in Java / Spring Boot.

Besides OBP API, Hola App depends on [Ory Hydra](https://www.ory.sh/hydra) and [OBP Hydra Identity Provider](https://github.com/OpenBankProject/OBP-Hydra-Identity-Provider).

A working Hola App setup can be used to drive automatic tests using [OBP Selenium](https://github.com/OpenBankProject/OBP-Selenium).

## Build with maven

Build with `mvn clean package`

Hola JAR file will be in `target` folder

## prepare truststore
Assuming OBP-API server URL is: `apisandbox.openbankproject.com`
Assuming Hydra server URL is: `oauth2.openbankproject.com`

- retrieve OBP API and Hydra server certificates:

    `openssl s_client -servername apisandbox.openbankproject.com -connect apisandbox.openbankproject.com:443 </dev/null 2>/dev/null | openssl x509 -inform PEM -outform DER -out obp-api.cer`

    `openssl s_client -servername oauth2.openbankproject.com -connect oauth2.openbankproject.com:443 </dev/null 2>/dev/null | openssl x509 -inform PEM -outform DER -out hydra.cer`

- import both certificates to the truststore file truststore.jks, and put it in resources/cert folder

    `keytool -import -alias api -keystore truststore.jks -file obp-api.cer`
    
    `keytool -import -alias hydra -keystore truststore.jks -file hydra.cer`
    
## prepare keystore

If mTLS is enabled on the OBP API instance, the client key needs to be signed by OBP API client CA. Else, any self-signed certificate will do. Continuing assuming you have `client.key` and `client.crt`:

- Convert client key and cert to client-cert.p12
  
    `openssl pkcs12 -export -in client.crt -inkey client.key -certfile user.crt -out client-cert.p12`

- Convert client-cert.p12 to file keystore.jks

    `keytool -importkeystore -srckeystore client-cert.p12 -srcstoretype pkcs12 -destkeystore keystore.jks`

## Adjust application.properties file

Create `application.properties` according to `application.properties.example`.

## Run

`java -jar obp-hola-app-0.0.29-SNAPSHOT.jar`

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
