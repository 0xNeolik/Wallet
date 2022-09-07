## Server-Sent Events (SSE)

A server-sent event is when a web page automatically gets updates from a server.

**Stockmind SSE**

Every time a new transaction arrives to the server, a new event is throwed through SSE to all the participants involved in the transaction. This allows to update users with the new information. 

**Stockmind SSE endpoint**

Each user has a endpoint that represents them based on the user ethereum address. This is necessary to only get events related to them.

```
ApiEndpoint/ApiVersion/stream/UserEthAddr
```

Example:

```
api.stockmind.io/v1/stream/0x437b69ba17197094d4237e061977138e71d318c5
```

**Get SSE in javascript**

EventSource can be used to get SSE from the server.

The following code shows an example:

```
evtSource = new EventSource(ENDPOINT);
        evtSource.onmessage = function(e) {
               /// Actions to do when a new event arrives
        };
```