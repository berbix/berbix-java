# Berbix Java SDK
This Berbix Java library provides simple interfaces to interact with the Berbix API.

## Installation

Install via Gradle

    implementation 'com.berbix:berbix-java:1.1.0'

Also available on [Maven](https://search.maven.org/artifact/com.berbix/berbix-java)

## Usage

### Constructing a client

    BerbixClient berbixClient = Berbix.create(
        new Berbix.BerbixOptions.Builder()
            .apiSecret("YOUR_API_SECRET_HERE_DO_NOT_PUT_IN_SOURCE_CODE")
            .build());

### Create a transaction

Create Transaction Request

    CreateTransactionRequest request = new CreateTransactionRequest();
    request.customerUid = "YOUR_CUSTOMER_UID_HERE"; // ID for the user in internal database
    request.templateKey = "YOUR_TEMPLATE_KEY_HERE"; // Template key for this transaction

Create Transaction

    CreateTransactionResponse response = berbixClient.createTransaction(request);

Asynchronous Create Transaction

    CompletableFuture<CreateTransactionResponse> responseFuture = berbixClient.createTransactionAsync(request);

### Create a Hosted transaction

Create Hosted Transaction Request

    CreateHostedTransactionRequest request = new CreateHostedTransactionRequest();
    request.customerUid = "YOUR_CUSTOMER_UID_HERE"; // ID for the user in internal database
    request.templateKey = "YOUR_TEMPLATE_KEY_HERE"; // Template key for this transaction

Create Transaction

    CreateHostedTransactionResponse response = berbixClient.createHostedTransaction(request);

Asynchronous Create Transaction

    CompletableFuture<CreateHostedTransactionResponse> responseFuture = berbixClient.createHostedTransactionAsync(request);

### Create tokens from refresh token

    String refreshToken = ""; // fetched from database
    Tokens transactionTokens = Tokens.fromRefresh(refreshtoken);

### Fetch transaction data

    Transaction transaction = client.fetchTransaction(tokens);
    CompletableFuture<Transaction> transaction = client.fetchTransactionAsync(tokens);

## Reference

### `Berbix`

#### Methods

##### `create`

    BerbixClient create(BerbixOptions options)

##### `BerbixOptions`

Supported options:

- `apiSecret` (required) - The API secret that can be found in your Berbix Dashboard.

### `BerbixClient`

#### Methods

##### `CreateTransactionResponse createTransaction(CreateTransactionRequest createTransactionRequest)`

Creates a transaction within Berbix to initialize the client SDK. Typically after creating
a transaction, you will want to store the refresh token in your database associated with the
currently active user session.

CreateTransactionRequest Supported options:

- `email` - Previously verified email address for a user.
- `phone` - Previously verified phone number for a user.
- `customerUid` - An ID or identifier for the user in your system.
- `templateKey` - The template key for this transaction.

Returns CreateTransactionResponse:

- `Tokens` - Tokens for transaction. This is the tokens object for future SDK calls. 

Also supports an async version that returns a CompletableFuture: `createTransactionAsync`

##### `CreateHostedTransactionResponse createHostedTransaction(CreateHostedTransactionRequest createHostedTransactionRequest)`

Creates a hosted transaction within Berbix to initialize the client SDK. This works the same as create_transaction except `hosted_options` is a valid parameter and that it returns an explicit `hosted_url` for hosted transactions.

Supported options:

- `email` - Previously verified email address for a user.
- `phone` - Previously verified phone number for a user.
- `customerUid` - An ID or identifier for the user in your system.
- `templateKey` - The template key for this transaction.
- `hostedOptions` - Optional configuration object for creating hosted transactions. The `hostedOptions` object can optionally include the following fields:
    - `completionUrl` - Email address to which completion alerts will be sent for this transaction.
    - `redirectUrl` - URL to redirect the user to after they complete the transaction. If not specified, the URL specified in the Berbix dashboard will be used instead.

Returns CreateHostedTransactionResponse:

- `Tokens` - Tokens for transaction. This is the tokens object for future SDK calls.
- `hostedUrl` - Represents the hosted transaction URL.

Also supports an async version that returns a CompletableFuture: `createHostedTransactionAsync`

##### `Transaction fetchTransaction(Tokens tokens)`

Fetches all of the information associated with the transaction. If the user has already completed the steps of the transaction, then this will include all of the elements of the transaction payload as described on the [Berbix developer docs](https://developers.berbix.com).

Also supports an async version that returns a CompletableFuture: `fetchTransactionAsync`

##### `Tokens refreshTokens(Tokens tokens)`

This is typically not needed to be called explicitly as it will be called by the higher-level
SDK methods, but can be used to get fresh client or access tokens.

Also supports an async version that returns a CompletableFuture: `refreshTokensAsync`

##### `boolean validate_signature(String secret, String body, String headerring)`

This method validates that the content of the webhook has not been forged. This should be called for every endpoint that is configured to receive a webhook from Berbix.

Parameters:

- `secret` - This is the secret associated with that webhook. NOTE: This is distinct from the API secret and can be found on the webhook configuration page of the dashboard.
- `body` - The full request body from the webhook. This should take the raw request body prior to parsing.
- `header` - The value in the 'X-Berbix-Signature' header.

##### `boolean deleteTransaction(Tokens tokens)`

Permanently deletes all submitted data associated with the transaction corresponding to the tokens provided.
Returns a boolean whether the request was successful.

Also supports an async version that returns a CompletableFuture: `deleteTransactionAsync`

##### `Transaction updateTransaction(Tokens tokens, UpdateTransactionRequest request)`

Changes a transaction's "action", for example upon review in your systems. Returns the updated transaction upon success.

`UpdateTransactionRequest` Parameters:

- `String action` - A string describing the action taken on the transaction. Typically this will either be "accept" or "reject".
- `String note` - A string containing an optional note explaining the action taken.

Also supports an async version that returns a CompletableFuture: `updateTransactionAsync`

##### `boolean overrideTransaction(Tokens tokens, OverrideTransactionRequest request)`

Completes a previously created transaction, and overrides its return payload and flags to match the provided parameters.

`OverrideTransactionRequest` Parameters:

- `ResponsePayload responsePayload` - A string describing the payload type to return when fetching transaction metadata, e.g. "us-dl". See [our testing guide](https://docs.berbix.com/docs/testing) for possible options.
- `List<String> flags` - An optional list of flags to associate with the transaction (independent of the payload's contents), e.g. ["id_under_18", "id_under_21"]. See [our flags documentation](https://docs.berbix.com/docs/id-flags) for a list of flags.
- `Map<String, String> overrideFields` - An optional mapping from a [transaction field](https://docs.berbix.com/reference#gettransactionmetadata) to the desired override value, e.g. `override_fields={"date_of_birth": "2000-12-09"}}`

Also supports an async version that returns a CompletableFuture: `overrideTransactionAsync`

### `Tokens`

#### Properties

##### `String accessToken`

This is the short-lived bearer token that the backend SDK uses to identify requests associated with a given transaction. This is not typically needed when using the higher-level SDK methods.

##### `String clientToken`

This is the short-lived token that the frontend SDK uses to identify requests associated with a given transaction. After transaction creation, this will typically be sent to a frontend SDK.

##### `String refreshToken`

This is the long-lived token that allows you to create new tokens after the short-lived tokens have expired. This is typically stored in the database associated with the given user session.

##### `Long transactionId`

The internal Berbix ID number associated with the transaction.

##### `ZonedDateTime expiresAt`

The time at which the access and client tokens will expire.

### Integration with frameworks

Recommend registering the BerbixClient in a lifecycle manager so it gets closed when the service is terminating.

#### Dropwizard

    BerbixClient client;
    AutoCloseableManager clientManager = new AutoCloseableManager(client);
    environment.lifecycle().manage(clientManager);


## Publishing

To release a new version of the SDK, first bump the version in `berbix-java/build.gradle`.

Publish to maven:
    
    gradle berbix-java:publishBerbixJavaPublicationToMavenCentralRepository

Set credentials in `~/.gradle/gradle.properties`

    usr = username
    pwd = password