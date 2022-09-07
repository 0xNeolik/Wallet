# Login process

## Description
The sign in and log in processes are done through Auth0.

## Sequence diagram

[[Auth0_scheme.png]]

## Requests/Responses

### 1: Example request
```
GET /mobileauth HTTP/1.1
	Host: api.stockmind.io:9000
	Connection: keep-alive
	Pragma: no-cache
	Cache-Control: no-cache
	Upgrade-Insecure-Requests: 1
	User-Agent: Mozilla/5.0 (iPhone; CPU iPhone OS 9_1 like Mac OS X) AppleWebKit/601.1.46 (KHTML, like Gecko) Version/9.0 Mobile/13B143 Safari/601.1
	Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8
	Accept-Encoding: gzip, deflate
	Accept-Language: es-ES,es;q=0.8,en;q=0.6
```

### 2: Example redirect
```
	:authority: auth0.com
	:method: GET
	:path: prueba-stockmind.eu.auth0.com/login
	:scheme: https
	accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8
	accept-encoding: gzip, deflate, br
	accept-language: es-ES,es;q=0.8,en;q=0.6
	cache-control: no-cache
	cookie: [908 bytes were stripped]
	pragma: no-cache
	upgrade-insecure-requests: 1
	user-agent: Mozilla/5.0 (iPhone; CPU iPhone OS 9_1 like Mac OS X) AppleWebKit/601.1.46 (KHTML, like Gecko) Version/9.0 Mobile/13B143 Safari/601.1
```


### 3: Example redirect
```
	:authority: twitter.com
	:method: GET
	:path: api.twitter.com/oauth/authenticate?oauth_token=g2m7IgAAAAAAhbP7AAABZa4KZlA
	:scheme: https
	accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8
	accept-encoding: gzip, deflate, br
	accept-language: es-ES,es;q=0.8,en;q=0.6
	cache-control: no-cache
	cookie: [908 bytes were stripped]
	pragma: no-cache
	upgrade-insecure-requests: 1
	user-agent: Mozilla/5.0 (iPhone; CPU iPhone OS 9_1 like Mac OS X) AppleWebKit/601.1.46 (KHTML, like Gecko) Version/9.0 Mobile/13B143 Safari/601.1
```

### 4: Example redirect
```
	:authority: twitter.com
	:method: GET
	:path: api.stockmind.io:9000/v1/mobileauth/callback?code=Tz9kfroqE9CT7Qvf&state=_eghvSWVKbrnxEGY38UpgkELo6ap9KOo
	:scheme: https
	accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8
	accept-encoding: gzip, deflate, br
	accept-language: es-ES,es;q=0.8,en;q=0.6
	cache-control: no-cache
	cookie: [908 bytes were stripped]
	pragma: no-cache
	upgrade-insecure-requests: 1
	user-agent: Mozilla/5.0 (iPhone; CPU iPhone OS 9_1 like Mac OS X) AppleWebKit/601.1.46 (KHTML, like Gecko) Version/9.0 Mobile/13B143 Safari/601.1
```


### 5: Example response
```
HTTP/1.1 200
	status: 200
	cache-control: no-cache, no-store, must-revalidate, pre-check=0, post-check=0
	content-encoding: gzip
	content-length: 2207
	content-security-policy: default-src 'none'; connect-src 'self';
	content-type: text/html;charset=utf-8
	date: Tue, 31 Mar 1981 05:00:00 GMT
	expires: Tue, 31 Mar 1981 05:00:00 GMT
	last-modified: Tue, 31 Mar 1981 05:00:00 GMT
	pragma: no-cache
	server: tsa_f
	Set-Cookie: PLAY_SESSION=...
	status: 200 OK
	strict-transport-security: max-age=631138519
```