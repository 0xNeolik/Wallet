# DEPLOY FACTORY QUORUM

## Description
This page demonstrates the process to deploy the factory contract in to Quorum.


## SEND TRANSACTION TO NODE
First step, we need to send a transaction with the data information, we can get this param with remix.ethereum,
compiling and running the Factory SmartContract. The account that sends the transaction must be unlocked.

Important: For compatibility with Quorum Alastria, extract data input, compile using version 0.4.15 of Solidity.
### 1: Example request
```
curl -X POST --data '
{
	"jsonrpc" : "2.0",
	"method" : "eth_sendTransaction",
	"params" : [{
			"gas": "0x1000000000000000",
			"from" : "0xdcce1968e17a01266de10e4c081f2464febefdc9", //masterAccount
			"data" : "0x..."
		}
	],
	"id" : 42
}'
```

### 1: Example response
```
{
    "jsonrpc": "2.0",
    "id": 42,
    "result": "0x1f69bfef2ae60b385d62c3de48fc75e3389629bb26df0156b3e46dd7b02c585f"
}
```




## Retrieve Factory Contract Address
Second step, we need to retrieve our contract address direction.

### 2: Example request
```
curl -X POST --data '
{
    "jsonrpc":"2.0",
    "method":"eth_getTransactionReceipt",
    "params":["0x1f69bfef2ae60b385d62c3de48fc75e3389629bb26df0156b3e46dd7b02c585f"],
    "id":1}
```

### 2: Example response
```
{
    "jsonrpc": "2.0",
    "id": 1,
    "result": {
        "blockHash": "0x6529b0b6b76806c2229753653b4963592ea1c36f0163ca122512272cdecd61b7",
        "blockNumber": "0xba4597",
        "contractAddress": "0x7f059614819ac8e2af77d5b9e86076a66eaac704",
        "cumulativeGasUsed": "0x106ee9",
        "from": "0x4d8bb8b5dfa3b40565903a638439334ae8bdb37e",
        "gasUsed": "0x106ee9",
        "logs": [...],
        "logsBloom": "0x00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000",
        "root": "0x57b633b456d9f9dd7b9301d3c12ae27342c98db3abb6e7d9c9c8f7b0d6617c32",
        "to": null,
        "transactionHash": "0x60d5669b0fd80cd976cd83e40f9ae2e76ae0661583d552af9fad6b9dbdf366cb",
        "transactionIndex": "0x0"
    }
}
```