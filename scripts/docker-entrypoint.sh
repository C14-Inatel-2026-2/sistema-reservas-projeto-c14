#!/bin/sh
set -eu

ca_cert="${DB_CA_CERT_PATH:-/etc/secrets/aiven-ca.pem}"
if [ -f "$ca_cert" ]; then
    rm -f /tmp/database-truststore.p12
    keytool -importcert -noprompt -alias database-ca \
        -file "$ca_cert" \
        -keystore /tmp/database-truststore.p12 \
        -storetype PKCS12 -storepass changeit >/dev/null
fi

exec java -jar /app/app.jar "$@"
