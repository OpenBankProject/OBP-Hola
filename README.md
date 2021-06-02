# OBP-Hola

The Open Bank Project *Hola Application* can be used to demonstrate and test the Open Banking OAuth2 and consent creation and usage via OBP API and Ory Hydra. It supports both UK and Berlin Group styles. Hola is written in Java / Spring Boot.

## prepare truststore
Suppose the OBP-API server domain is: `api-mtls.ofpilot.com`

Suppose the Hydra server domain is: `oauth2.api-mtls.ofpilot.com`
- check server side certificate 

    `openssl s_client -showcerts -connect api-mtls.ofpilot.com:443`
    
    `openssl s_client -showcerts -connect oauth2.api-mtls.ofpilot.com:443`
- save the OBP-API server certificate to file ofpilot.cer, save Hydra server certificate to file hydra.cer

    `openssl s_client -servername api-mtls.ofpilot.com -connect api-mtls.ofpilot.com:443 </dev/null 2>/dev/null | openssl x509 -inform PEM -outform DER -out ofpilot.cer`    

    `openssl s_client -servername oauth2.api-mtls.ofpilot.com -connect oauth2.api-mtls.ofpilot.com:443 </dev/null 2>/dev/null | openssl x509 -inform PEM -outform DER -out hydra.cer`
- import ofpilot.cer and hydra.cer to truststore file ofpilot.jks, and put it in resources/cert folder

    `keytool -import -alias ofpilot -keystore ofpilot.jks -file ofpilot.cer`
    
    `keytool -import -alias hydra -keystore ofpilot.jks -file hydra.cer`
    
## prepare keystore
- convert user.key and user.pem file to file cert.p12, The user.pem come from [certificates](https://gitlab-external.tesobe.com/tesobe/boards/tech-internal/-/issues/44)
  
    `openssl pkcs12 -export -in user.pem -inkey user.key -certfile user.pem -out user.p12`
- convert cert.p12 to file user.jks

    `keytool -importkeystore -srckeystore user.p12 -srcstoretype pkcs12 -destkeystore user.jks`

## config application.properties file

```
## keystore and truststore files can be local files or web resources, as example:
mtls.keyStore.path=file:///Users/<some path>/cert/user.jks
#mtls.keyStore.path=http://<some domain>/user.jks
mtls.keyStore.password=<keystore password>
mtls.trustStore.path=file:///Users/<some path>/cert/ofpilot.jks
#mtls.trustStore.path=http://<some domain>/ofpilot.jks
mtls.trustStore.password=<truststore password>
```

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
