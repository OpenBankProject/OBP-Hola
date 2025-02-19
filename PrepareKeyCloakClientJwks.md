This is for the local development environment. For production, modifications are needed.
Note: for Keycloak 26.1.2, I can not import public key using UI page and Rest Endpoint, there is a bug for KeyCloak:https://github.com/keycloak/keycloak/issues/37066



# Generate a private key
openssl ecparam -name prime256v1 -genkey -noout -out private_key.pem

# Extract the public key
openssl ec -in private_key.pem -pubout -out public_key.pem

# convert pem to jwk
## install jwcrypto (if not installed)
pip install jwcrypto

## Convert the Public Key to JWK python code:
``` 
from jwcrypto import jwk
import json

# Load private key
with open("private_key.pem", "rb") as f:
    pem_data = f.read()

key = jwk.JWK.from_pem(pem_data)

# Convert to JWK format
private_jwk = key.export_private()

# Print JWK keys
print("Private JWK:", json.dumps(json.loads(private_jwk), indent=4))

# Save JWK keys
with open("private_key.jwk", "w") as f:
    f.write(private_jwk)



# Load private key
with open("public_key.pem", "rb") as f:
    pem_data = f.read()

key = jwk.JWK.from_pem(pem_data)

# Convert to JWK format
public_jwk = key.export_public()

# Print JWK keys
print("public JWK:", json.dumps(json.loads(public_jwk), indent=4))

# Save JWK keys
with open("public_key.jwk", "w") as f:
    f.write(public_jwk)
```
## This will output a JWK-formatted public and private keys:
```
Private JWK: {
"crv": "P-256",
"d": "-ZXB3lAwc_xEVkc8Sj3GiZD3MjR7NkVA3MPzyx-ssNM",
"kid": "ZpYOcPjauwcxo_4jm1QR-p0vAdxnEEB4HNecB3J2Zk8",
"kty": "EC",
"x": "tODNrchqmkr0GPZEuT1B39EUwCBeuJHWiphMIMLjLi4",
"y": "UfU-riAawiOxgoJqYhsvhD53Tj9fQu6UJiRIxed16wQ"
}
public JWK: {
"crv": "P-256",
"kid": "ZpYOcPjauwcxo_4jm1QR-p0vAdxnEEB4HNecB3J2Zk8",
"kty": "EC",
"x": "tODNrchqmkr0GPZEuT1B39EUwCBeuJHWiphMIMLjLi4",
"y": "UfU-riAawiOxgoJqYhsvhD53Tj9fQu6UJiRIxed16wQ"
}

```
# then copy Private JWK to application.
oauth2.jws_alg=ES256
oauth2.jwk_private_key={"crv":"P-256","d":"-ZXB3lAwc_xEVkc8Sj3GiZD3MjR7NkVA3MPzyx-ssNM","kid":"ZpYOcPjauwcxo_4jm1QR-p0vAdxnEEB4HNecB3J2Zk8","kty":"EC","x":"tODNrchqmkr0GPZEuT1B39EUwCBeuJHWiphMIMLjLi4","y":"UfU-riAawiOxgoJqYhsvhD53Tj9fQu6UJiRIxed16wQ"}

and also set the public key to OBP-Hola/src/main/resources/static/.well-known/jwks.json
and set it to KeyCloak -> Client ->Keys -> set `Use JWKS URL` on, and put `http://host.docker.internal:8081/.well-known/jwks.json` to JWKS URL   