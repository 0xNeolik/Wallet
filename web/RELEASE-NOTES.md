# [Unreleased]

## Breaking changes

## New features

* Add support for CAS via the endpoint `GET /mobileauth/cas`.

## Bug fixes

## Internal changes

## Migration from the previous version

Run the contents of the "database/tables.sql" file on your database. There's a new table.

# 2.0.0
## Breaking changes

* Allow transactions retrieval using just typical offset and limit parameters; and treating seamlessly both completed and pending transactions.

## Internal changes

* Add support for ETH transactions.

* Add caches to speed up the user transactions retrieval endpoint response time.

# 1.3.0

## New features

## Bug fixes

* Fix some erratas in Swagger docs.

## Internal changes

* Optimize tests by disabling Akka shutdown coordination

* Some more comments on code to improve readability (bad smell if we have to do this).

# 1.2.0

## New features

* Allow consumers of the authentication endpoint to provide as a query string parameter (under the 
schema name) that points to the redirection URL in which they want to receive the corresponding JWT.

* Include CORS headers to allow usage of the API from a web or external site.

## Bug fixes

* Fix ethereum transaction hash parsing after emitting an on-chain transaction (withdrawal).

## Internal changes

* Generate hashes in tests always lower case.

* Code formatter switched from Scalariform to scalafmt

# 1.1.5

## Bug fixes

* Bug parsing ethereum log events; an hexadecimal number was tried to be parsed as a decimal one.

# Internal changes

* Updated Circe to 0.8.0

* Updated Doobie to 0.4.4

* Updated Play to 2.6.6

* Updated Silhouette to 5.0.2

* Log some errors on the event watcher that were previously silent

# 1.1.4

## Bug fixes

* Fix bug in withdrawals. The on-chain operation didn't work because we were using the password of the withdrawal
  issuer's ethereum account on Stockmind. We have to use the master account password instead (that is the account
  that holds the funds for outgoing on-chain transactions).

# 1.1.3

## Bug fixes

* Fix error checking existing tokens by name in database (the query was wrong).

## Internal changes

* Improve test coverage (postgres secondary adapter, find token by name).

# 1.1.2

## Bug fixes

* Eliminate an issue that could cause some token creation events to be processed twice. 

* Use 409 (conflict) instead of 403 (Forbidden) for tokens transfers requests that return some kind of business validation error. We don't consider that a breaking change.

* Add an Allowed Hosts filter to make sure that we can access the server, no matter we try to reach it by DNS, IP address or locally (using localhost).

* Prevent creation of duplicated tokens (either by symbol or name).

## Internal changes

* More readable arrangement of configuration properties.

* Fix several typos.

* Add information to payload of error responses for the token creation endpoint.

* Refactor to ease the process of giving custom status and messages to error responses.

# 1.1.1

## Internal changes

* Increment amount of logs.
* Reduce log level from info to debug for frequent, not very significant logs.
* Rename TransactionHash case class to EthereumHash, and include the block hash in the LoggedEvent case class as an EthereumHash instance.

# 1.1.0

## Breaking changes

## New features

* New endpoint: `GET /v1/tokens`. Shows a list of currently supported tokens

* New endpoint: `POST /v1/tokens`. Allows a user to create a new token.

## Internal changes

* Remove implicit conversions between WSResponse objects coming from responses to JSON RPC calls to Parity node and NodeInfo instances extracting the information we are actually going to propagate as part of our API.

* Fix a bug concerning Ethereum method encoding when one of the parameters was a `string`.

* Increase test coverage regarding controllers.

* Better validation logic & error reporting while retrieving transactions by id, and while canceling pending transactions.

* Make authentication logic slightly more flexible regarding how to reply and return the JWT.

# 1.0.2

## Breaking changes

## New features

## Internal changes

* Fix a bug in withdrawals: we were calling the wrong method in the Ethereum node.

* Incoming and outgoing transactions show the Ethereum Address of the true
origin or destination.

# 1.0.1

## Internal changes

* Migrated to Scala 2.12.3

* Updated sbt-scalariform to 1.8.0

* Updated sbt-native-packager to 1.2.2

* Updated sbt-scoverage to 1.5.1

* Updated Silhouette to 5.0.0

* Updated Play to 2.6.3

* Updated Akka to 2.5.4

* Updated sbt to 0.13.16

* Fix a bug in build.sbt: the Jcenter repo wasn't added on the projects that
needed it, our current compilations worked because they had all the needed
packages in cache.

* Several bug fixes in the send tokens feature when it involves a not 100% happy path.

* Increased test coverage regarding send tokens feature.

* Relatively major code design changes
  - Single model for core component, implemented in the model sub-project.
  - Entity conversions only in adapters, when needed. The core works always with the single model (the truth).
  - Unify in one API all the operations related to off-chain transfers, withdrawals and pending transfers settlement.
  - Flatten return types of business logic functions to the minimum (now we can as we have just one API that operates under the same higher kinded type).
  - Reduce the number of traits involved in the tokens transfer feature. Reduce the number of nested functions and simplify code base.

