Fafnir-SSO
===
Fafnir-SSO is an SSO provider, which provides a Single Sign On functionality based on industry standards and best
practices, using 3rd party providers Fafnir generates JWT's which can be used uniformly by web applications in a
distributed cloud based setup.

Authentication Providers
===
Fafnir-SSO supports the following Authentication providers:

* Facebook
* Google
* Unilogin
* Economic customers
* LinkedIn
* Hazelcast (Username/Password)
* MitID

Authentication Tokens
===
Fafnir-SSO issues JWT RSA-512 tokens, which can be validated using the exposed public key. The fields populated are:

* sub: The subjects name, as provided by the Authentication provider.
* iss: The issuer, which will be fafnir-<providername>, where <providername> will be the name of the provider used.
* iat: The time the JWT was issued at.
* name: The full name, as provided by the authentication provider.

Usage
===

Version 2.0
---
In version 2 onward, configuration happens through individual environment variables.
These are (Environment variables marked with :heavy_check_mark: are **required** if you want a specific login provider to be available):
* E-conomic
    * ECONOMIC_AST - The E-conomic Application Secret Token :heavy_check_mark:
    * ECONOMIC_AGT - The Economic Application Grant Token :heavy_check_mark:
* Facebook
    * FACEBOOK_AID - The Facebook Application Id :heavy_check_mark:
    * FACEBOOK_SECRET - The Facebook Secret :heavy_check_mark:
* Google
    * GOOGLE_AID - The Google Application Id :heavy_check_mark:
    * GOOGLE_SECRET - The Google Secret :heavy_check_mark:
* LinkedIn
    * LINKED_IN_AID - The LinkedIn Application Id :heavy_check_mark:
    * LINKED_IN_SECRET - The LinkedIn Secret :heavy_check_mark:
* UniLogin
    * UL_AID - The UniLogin Application Id
    * UL_SECRET - The UniLogin Secret :heavy_check_mark:
    * UL_WS_USER - Your UniLogin WebService username :heavy_check_mark:
    * UL_WS_PASS - Your UniLogin WebService password :heavy_check_mark:
    * UL_SSO - Whether to use UniLogin in Single Sign On mode, default is false.
* Hazelcast
    * HAZELCAST_USERNAME_IS_EMAIL - Determines if usernames are stored in lowercase only, so that look ups can be performed case-insensitively, default is false.
    * HAZELCAST_PASSWORD_IS_ENCRYPTED - Determines if passwords are encrypted using RSA encryption, or hashed with bcrypt, default is false.
    * HAZELCAST_MAP_NAME - The name of the Hazelcast Map to use for storing user data. Default is `fafnir-users`
    * HAZELCAST_TCP_IP_ADDRESS - Makes Fafnir connect to hazelcast using TCP/IP instead of Multicast, to the specified address.
* MitID
    * MITID_AID - The MitID ClientID :heavy_check_mark:
    * MITID_SECRET - The MitID Client-Secret :heavy_check_mark:
    * MITID_AUTHORITY_URL - The URL to the MitID broker authority (for example `https://brokertest.signaturgruppen.dk/op`) :heavy_check_mark:  
  
  If Fafnirs test mode is enabled, the MitID provider will use the `mitid_demo` scope instead of `mitid`.  
  Fafnir uses the `ssn` scope in order to add the users name to the resulting JWT.
* Fafnir
    * FAFNIR_URL - The url used to access this instance of fafnir, default is  http://localhost:8080
    * FAFNIR_SUCCESS - The url to redirect to after successful authentication, default is http://localhost:8080/success
    * FAFNIR_FAILURE - The url to redirect to after authentication failure, default is http://localhost:8080/fail
* Testing
    * TEST_ENABLED - enables the `/test` endpoint which will always return a valid jwt for a test user.

Version 1.x (Deprecated)
---
You must provide a configuration as an ACTO_CONF Environment variable, the JSON should look like this:

    {
        "facebookAppId": "0",
        "facebookSecret": "secret",
        "googleAppId": "0",
        "googleSecret": "secret",
        "uniLoginAppId": "0",
        "uniLoginSecret": "secret",
        "uniLoginWSUsername": "username",
        "uniLoginWSPassword": "password",
        "successUrl": "http://localhost:8080/success",
        "failureUrl": "http://localhost:8080/fail",
        "myUrl": "http://localhost:8080",
        "enableParameter": false,
        "testMode": false,
        "hazelcastUsernameIsEmail": false
    }

The different fields mean:  

* facebookAppId: Your facebook appid, you can find this in the facebook developer console.  
* facebookSecret: Your facebook secret, you can find this in the facebook developer console.  
* googleAppId: Your google appid, you can find this in the google developer console.  
* googleSecret: Your google secret, you can find this in the google developer console.  
* uniLoginAppId: Your unilogin appid.  
* uniLoginSecret: Your unilogin secret.  
* uniLoginWSUsername: Your unilogin webservice username.  
* uniLoginWSPassword: Your unilogin webservice password.  
* uniLoginSingleSignOn: (Default: false) Choose if unilogin should be SingleLogin (false) or SingleSignOn (true)   
* successUrl: The URL to redirect to when successful - the JWT will be appended to this URL.  
* failureUrl: The URL to redirect to when unsuccessful.  
* myUrl: The URL for the whole app.
* enableParameter: (Default: false) How the JWT token will be appended to URL, using URL?jwtToken=JWT for true or URL#JWT for false  
* testMode: If set to true, on startup the service/docker image will write a test token to the log. It also enables the /test endpoint, from which you can retrieve a test token programmatically. 
* hazelcastUsernameIsEmail: If set to true, the username will be treated as case insensitive when logging in. It assumes that all usernames (emails) are stored as lowercase. 

How It Works
---
On startup the server will generate a new secure RSA private key. This private key is kept in memory, so will be
destroyed when the service shuts down, invalidating all existing JWT's. It will also expose the public key on the
`/public-key` endpoint. This key is in X509 certificate format (aka. Base64 encoded raw data). You can use this to
validate your JWT.

Your JWT is returned to the success url as a fragment, as browsers do not ordinarily send this part to the server,
so the JWT will not bleed through to server access logs. This means that the browser is responsible for storing the JWT
securely until it is needed for API requests. 
