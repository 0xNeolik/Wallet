# API KEYS

## Description
This page demonstrates the requests needed to obtain or remove an API KEY. Each user manages their own API keys.


## Add API key
To add an API key the user must be log in previously on stockmind. It will be necessary to pass as parameter the access authentication key.
It will generate a ramdom API key (UUID).

### 1: Example request
```
GET /v1/users/apikey?api_key=33db777e-896c-44be-8b27-6c3d4d0c2a39 HTTP/1.1
	Host: api.stockmind.io:9000
	Connection: keep-alive
	Pragma: no-cache
	Cache-Control: no-cache
	Upgrade-Insecure-Requests: 1
	User-Agent: Mozilla/5.0 (iPhone; CPU iPhone OS 9_1 like Mac OS X) AppleWebKit/601.1.46 (KHTML, like Gecko) Version/9.0 Mobile/13B143 Safari/601.1
	Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8
	Referer: http://10.0.16.20:8000/
	Accept-Encoding: gzip, deflate
	Accept-Language: es-ES,es;q=0.8,en;q=0.6
```

### 1: Example response
```
	HTTP/1.1 200 OK
	Vary: Origin
	Date: Tue, 28 Aug 2018 11:11:54 GMT
	Content-Type: application/json
	Content-Length: 50
	
    "api_key": "4bb246b3-c5a2-4d3e-a8be-016f613a1a81"
```




## Remove API key
To remove an API key the user must be log in previously on stockmind. It will be necessary to pass as parameter the access authentication key.
It will remove the API key pass as parameter.
```
DELETE /v1/users/apikey/**cb528415-f44a-4bfc-bdf6-fbe02340bbc7**?api_key=33db777e-896c-44be-8b27-6c3d4d0c2a39 HTTP/1.1
	Host: api.stockmind.io:9000
	Connection: keep-alive
	Pragma: no-cache
	Cache-Control: no-cache
	Upgrade-Insecure-Requests: 1
	User-Agent: Mozilla/5.0 (iPhone; CPU iPhone OS 9_1 like Mac OS X) AppleWebKit/601.1.46 (KHTML, like Gecko) Version/9.0 Mobile/13B143 Safari/601.1
	Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8
	Referer: http://10.0.16.20:8000/
	Accept-Encoding: gzip, deflate
	Accept-Language: es-ES,es;q=0.8,en;q=0.6
```

### 1: Example response
```
	HTTP/1.1 200 OK
	Vary: Origin
	Date: Tue, 28 Aug 2018 11:11:54 GMT
	Content-Type: application/json
	Content-Length: 50
	
    "deleted_key": "cb528415-f44a-4bfc-bdf6-fbe02340bbc7"
```