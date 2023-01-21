# Fafnir-SSO
Fafnir-SSO is an SSO provider, which provides a Single Sign On functionality based on industry standards and best
practices, using 3rd party providers Fafnir generates JWT's which can be used uniformly by web applications in a
distributed cloud based setup.

Fafnir-IAM is the IAM module for the Hazelcast provider. It gives the ability to create individual user/pass users,
and configure claims for each of those users.

## Authentication Providers
Fafnir-SSO supports the following Authentication providers.
* Apple
* Economic customers
* Facebook
* Google
* Hazelcast (Username/Password)
* LinkedIn
* Microsoft Identity
* MitID
* SAML 2.0
* Unilogin
* Testing

## Authentication Tokens
Fafnir-SSO issues JWT RSA-512 tokens, which can be validated using the exposed public key (available on the 
`/public-key` route). The fields populated are:
* sub: The subjects name, as provided by the Authentication provider.
* iss: The issuer, which will be `fafnir-<provider>`, where provider is the name of the Authentication provider. (E.g. 
`fafnir-google` for the Google provider).
* iat: The time the JWT was issued at.
* name: The full name, as provided by the Authentication provider.

### Special Fields
There are however some special fields for some of the Authentication providers:

#### Hazelcast
The Hazelcast provider will also populate the `role` field on the JWT with the claims from IAM. This field is an array
of strings.

#### SAML
The SAML provider will populate the `role` field which each attribute from the SAML IdP. The `role` field is an array of
strings. Each attribute will be an element in the role field, where the attribute is a string of `key=value`.

This could look like this:
```JSON
{
  "role": [
    "firstName=Acto",
    "company=Acto ApS"
  ]
}
```

## Usage
This documents the v3.x releases, for previous versions see the [v2 README](https://github.com/actoaps/fafnir-sso/blob/master/README-v2.md).

From version 2 onward, configuration has been handled through individual environment variables. Environment variables
marked with :heavy_check_mark: are **required** if you want to enable that Authentication provider.

### Provider Configuration
These are only present on Fafnir-SSO. Fafnir-IAM does not support individual provider configuration. 
* Apple
  * APPLE_AID - The Apple Application ID :heavy_check_mark:
  * APPLE_SECRET - The Apple Secret :heavy_check_mark:
* Economic customers
  * ECONOMIC_AST - The E-conomic Application Secret Token :heavy_check_mark:
  * ECONOMIC_AGT - The E-conomic Application Grant Token :heavy_check_mark:
* Facebook
  * FACEBOOK_AID - The Facebook Application ID :heavy_check_mark:
  * FACEBOOK_SECRET - The Facebook Secret :heavy_check_mark:
* Google
  * GOOGLE_AID - The Google Application ID :heavy_check_mark:
  * GOOGLE_SECRET - The Google Secret :heavy_check_mark:
* Hazelcast (Username/Password)
  * HAZELCAST_USERNAME_IS_EMAIL - Determines if usernames are stored in lowercase only, so that look ups can be performed case-insensitively, default is false.
  * HAZELCAST_PASSWORD_IS_ENCRYPTED - Determines if passwords are encrypted using RSA encryption, or hashed with bcrypt, default is false.
  * HAZELCAST_MAP_NAME - The name of the Hazelcast Map to use for storing user data. Default is fafnir-users
  * HAZELCAST_TCP_IP_ADDRESS - Makes Fafnir connect to hazelcast using TCP/IP instead of Multicast, to the specified address.
* LinkedIn
  * LINKED_IN_AID - The LinkedIn Application ID :heavy_check_mark:
  * LINKED_IN_SECRET - The LinkedIn Secret :heavy_check_mark:
* Microsoft Identity
As Fafnir is using OpenID Connect to authenticate, you need to check the "ID tokens" box under "Implicit grant and 
hybrid flows" in the Authentication menu. If this box is not checked, you will receive an error upon authentication.
  * MSID_AID - The Azure App Application ID :heavy_check_mark:
  * MSID_SECRET - The Azure App Client Secret :heavy_check_mark:
  * MSID_TENANT - THe Azure App's chosen [tenancy](https://learn.microsoft.com/en-us/azure/active-directory/develop/active-directory-v2-protocols#endpoints)
:heavy_check_mark:
* MitID
  * MITID_AID - The MitID ClientID :heavy_check_mark:
  * MITID_SECRET - The MitID Client-Secret :heavy_check_mark:
  * MITID_AUTHORITY_URL - The URL to the MitID broker authority (for example `https://brokertest.signaturgruppen.dk/op`) :heavy_check_mark:
Do note, if Fafnir-SSO has test mode enabled, the MitID provider will use the `mitid_demo` scope instead of `mitid`.
Fafnir uses the `ssn` scope in order to add the users name to the resulting JWT.
* SAML 2.0
SAML can only be configured through FAFNIR IAM.
* Unilogin
  * UL_AID - The Unilogin Application ID
  * UL_SECRET - The Unilogin Secret :heavy_check_mark:
  * UL_WS_USER - Your Unilogin WebService username :heavy_check_mark:
  * UL_WS_PASS - Your Unilogin WebService password :heavy_check_mark:
  * UL_SSO - Whether to use Unilogin in Single Sign On mode, default is false.
* Testing
  * TEST_ENABLED - enables the `/test` endpoint which will always return a valid JWT for a test user.

### Fafnir Configuration
Fafnir-SSO and Fafnir-IAM also have some configuration options:

* Fafnir-SSO
  * FAFNIR_URL - The URL used to access this instance of Fafnir-SSO, default is `http://localhost:8080`.
  * FAFNIR_SUCCESS - The URL used to access this instance of Fafnir-SSO, default is `http://localhost:8080/loginredirect`.
  * FAFNIR_FAILURE - The URL used to access this instance of Fafnir-SSO, default is `http://localhost:8080/loginerror`.
  * HAZELCAST_TCP_IP_ADDRESS - The Hazelcast instance URI. By default it will run an embedded instance. (The embedded
instance will try connect to other instances on the same network).
  * KEYTSTORE_PASS - The [keystore password](#persistent-key-storage).
  * KEY_PASS - The [key password](#persistent-key-storage).
  * PROVIDER_LOWERCASE_SUBJECT - If set to true, it will transform all subjects returned from the providers to lowercase,
default is `false`. Hazelcast is seperate, see Hazelcast specific configuration.

* Fafnir-IAM
  * FAFNIR_URL - The URL used to get the public key from the SSO instance, default is `http://localhost`
  * FAFNIR_PORT - The PORT for the Fafnir-SSO instance, default is `8080`.
  * HAZELCAST_TCP_IP_ADDRESS - The Hazelcast instance URI. By default it will run an embedded instance. (The embedded
    instance will try connect to other instances on the same network).
  * IAM_ADMIN_SUBECT - The subject to generate for the `FAFNIR_ADMIN` user, default is `ADMIN`.
  * IAM_ADMIN_PASSWORD - If this is present, IAM will autogenerate a Hazelcast user with the claim `FAFNIR_ADMIN` which
gives access to the IAM dashboard.


## Persistent Key Storage
In some cases you may want to store the generated keypair (or use one you generated manually). In this case you should
mount a docker volume on `/var/lib/fafnir` and add the `KEYSTORE_PASS` and `KEY_PASS` environment variables.

If one does not already exists, Fafnir-SSO will generate a keystore in the directory, with the name `fafnir.jks`. 
(Meaning it will be located on `/var/lib/fafnir/fafnir.jks`).

The keystore is a standard JKS format, the key alias is "FAFNIR".

## Fafnir Client
We also provide a Java library for interacting with Fafnir-SSO. It is called the 
[fafnir-client](https://mvnrepository.com/artifact/dk.acto/fafnir-client). And can be installed with Maven or Gradle.

## Building from Source
Ensure that you have JDK 17 and Docker installed. Now, to build you need to run:
```Bash
.\gradlew build
```
This will build the whole project. Now to run both IAM & SSO, you can run the Compose project:
```Bash
docker compose up -d --build
```
You should now be able to access the project. The default compose project includes the `IAM_ADMIN_PASSWORD` environment
variable. This will give you access to IAM via the credentials: `ADMIN:pass`. You can now just open the
[Hazelcast provider](http://localhost:8080/hazelcast/login) and login with those credentials. From there you should get
redirected to the IAM dashboard.
