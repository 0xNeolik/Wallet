
We need to generate the keys and convert them to format .DER to be manage with Scala

Below command to generate pair of key:

```
$openssl genrsa -out mykey.pem 2048
```
This command to generate the private key:

```
$openssl pkcs8 -topk8 -inform PEM -outform DER -in mykey.pem -out private_key.der -nocrypt
```
And this command to get the public key:

```
$ openssl rsa -in mykey.pem -pubout -outform DER -out public_key.der

```
- The public_key.der will go on StockMind root path

- The private_key.der will be stored on Bankia in the LicenseGenerator root path, and executing the "entry.scala" will request the day, month and year and then output the License for the given date.

Finally, to generate the License (example):
```
$ sbt "run --day 01 --month 12 --year 2020"

```