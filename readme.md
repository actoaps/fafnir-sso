Fafnir-SSO
===
Fafnir-SSO is an SSO provider, which provides a Single Sign On functionality based on industry standards and best
practices, using 3rd party providers Fafnir generates JWT's which can be used uniformly by web applications in a
distributed cloud based setup.

Authentication Providers
---
Fafnir-SSO supports the following Authentication providers:

* Facebook
* Google
* Unilogin
* Economic customers
* LinkedIn

Authentication Tokens
---
Fafnir-SSO issues JWT RSA-512 tokens, which can be validated using the exposed public key. The fields populated are:

* sub: The subjects name, as provided by the Authentication provider.
* iss: The issuer, which will be fafnir-<providername>, where <providername> will be the name of the provider used.
* iat: The time the JWT was issued at.
* name: The full name, as provided by the authentication provider.

Usage
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
    }

The different fields mean:  

* facebookAppId: Your facebook appid, you can find this in the facebook developer console.  
* facebookSecret: Your facebook secret, you can find this in the facebook developer console.  
* googleAppId: Your google appid, you can find this in the google developer console.  
* googleSecret: Your google secret, you can find this in the facebook developer console.  
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

How It Works
---
On startup the server will generate a new secure RSA private key. This private key is kept in memory, so will be
destroyed when the service shuts down, invalidating all existing JWT's. It will also expose the public key on the
`/public-key` endpoint. This key is in X509 certificate format (aka. Base64 encoded raw data). You can use this to
validate your JWT.

Your JWT is returned to the success url as a fragment, as browsers do not ordinarily send this part to the server,
so the JWT will not bleed though to server access logs. This means that the browser is responsible for storing the JWT
securely until it is needed for API requests.