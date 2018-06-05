#### Supported transports ####

Encryption is supported on following transports:

- `wss`

- `https`

Authentication is supported on following transports:

- `ws`

- `wss`

- `http`

- `https`

_Note : ws/wss transports will send authentication only during handshake, while http/https will send on every request_

#### Communication Security Protocols ####

When not specified, JVM default list of communication security protocols will be used.

It is recommended to use `TLSv1.2`.

For possible values, consult [JCA documentation](https://docs.oracle.com/javase/8/docs/technotes/guides/security/StandardNames.html#SSLContext)

#### Cipher suites ####

When not specified, JVM default list of cipher suites will be used.

For possible values, consult [JCA documentation](https://docs.oracle.com/javase/8/docs/technotes/guides/security/StandardNames.html#ciphersuites)

#### KeyStore types ####

##### JKS #####

This is default KeyStore type. Can be also requested explicitly by setting option `keystore-type` to value `JKS`.

Following options are considered:

 - `truststore-file` path to JKS keystore containing CA trust anchors, eg.`/path/to/cacerts.jks`.

   If not specified, default value of `$JAVA_HOME/lib/security/cacerts` will be used.

   If path is not absolute, it is relative to current working directory.

 - `truststore-password` password used to unlock trust KeyStore.

   If not specified, default value of `changeit` will be used.

 - `keystore-file` path to JSK KeyStore containing client/server private key.

   If not specified, value of `truststore-file` option is used instead.

 - `keystore-password` password used to unlock KeyStore containing client/server private key.

   If not specified, value of `truststore-password` option is used.

 - `certificate-alias` certificate alias which identify server/client within keyStore.

   If not specified, first PrivateKey entry in store is used.


##### PKCS12

This KeyStore is selected when option `keystore-type` is set to value `PKCS12`.

It is expected that single file contains all certificates and keys used to setup SSL/TLS session.

Following options are considered:

 - `keystore-file` **REQUIRED**  path to PKCS12 KeyStore file containing all certificates and private keys.

   If path is not absolute, it is relative to current working directory.

 - `keystore-password` **REQUIRED** password used to unlock this KeyStore.

 - `certificate-alias` certificate alias which identify server/client within keyStore.

   If not specified, first PrivateKey entry in store is used.

#### Miscellaneous options ####

- `cert-validation-policy` - configure how certificates are validated during handshake.

   * `strict` presented certificate must be _valid_. This is default value.

   * `ignore` no verification is done. **Not recommended, only for testing**.

- `client-verify` - verification level for the TLS client authentication

   * `NONE` - Indicates that the SSL engine will not request client authentication. This is default value.

   * `OPTIONAL` - Indicates that the SSL engine will request client authentication, but will not fail of none is provided.

   * `REQUIRE` - Indicates that the SSL engine will **require** client authentication.


#### Examples ####


 - Set credentials for client endpoint

  `ws://127.0.0.1:12000?auth-username=admin`

 _Note no password - it will be queried at endpoint creation time from SecurityService_

 - Set credentials for client endpoint, including password

 `ws://127.0.0.1:12000?auth-username=admin&auth-password=123456`

 - Enable authentication on server endpoint

 `ws://0.0.0.0:12000?auth`

 _Incoming connections/requests will be authenticated against SecurityService_

 - Require particular cipher suites on client endpoint

 `https://127.0.0.1:13000?ciphers=TLS_DHE_DSS_WITH_AES_256_CBC_SHA256,TLS_DHE_DSS_WITH_AES_256_GCM_SHA384`

 _This will enforce use of TLS_DHE_DSS_WITH_AES_256_CBC_SHA256 or TLS_DHE_DSS_WITH_AES_256_GCM_SHA384 cipher suite._

 - Require particular encryption protocol on client endpoint

 `https://127.0.0.1:13000?protocols=TLS1.2,TLS1.1`

 _This will enforce either TLS1.1 or TLS1.2_

 - Enforce server CA chain length to 3 on client endpoint

 `https://127.0.0.1:13000?server-chain-max-length=3`

 - Create SSL-enabled server endpoint

 `wss://0.0.0.0:12000?keystore-type=PKCS12&keystore-file=/tmp/certbundle.pkcs11`

 - Create server endpoint that require TLS-authentication

 `wss://0.0.0.0:12000?keystore-type=PKCS12&keystore-file=/tmp/certbundle.pkcs11&client-verify=REQUIRE`
 