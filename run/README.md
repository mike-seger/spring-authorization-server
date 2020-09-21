# run utilities

These utilities are a shortcut and debugging enhancements for this: [README.adoc](../samples/boot/oauth2-integration/README.adoc)

## running all applications
`./runall.sh`

logs are written into "../build"

*IMPORTANT:* Make sure to modify your `/etc/hosts` described in: [README.adoc](../samples/boot/oauth2-integration/README.adoc)

## curl calls
### client credentials flow

```
curl -v -b /tmp/cookie -c /tmp/cookie "http://localhost:39001/authorize?grant_type=client_credentials"
```

### authorization code flow

```
curl -v -b /tmp/cookie -c /tmp/cookie \
	"http://localhost:39001/authorize?grant_type=authorization_code"
# ---> 302
# cookies
#HttpOnly_localhost	FALSE	/	FALSE	0	JSESSIONID	4BF7F1430034D73CCC5DE33A0A1B287A

curl -v -b /tmp/cookie -c /tmp/cookie \
"http://auth-server:39003/oauth2/authorize?response_type=code&client_id=messaging-client&scope=message.read%20message.write&state=8pXT0n956RZodBxoIoLWI1hhgcTZuhfPbmxZ_zxsHEU%3D&redirect_uri=http://localhost:39001/authorized"
# ---> 302
# cookies
#HttpOnly_auth-server	FALSE	/	FALSE	0	JSESSIONID	A8E2C3B26F0CC6FDA21963809FF52D6D
#HttpOnly_localhost	FALSE	/	FALSE	0	JSESSIONID	4BF7F1430034D73CCC5DE33A0A1B287A

curl -v -b /tmp/cookie -c /tmp/cookie "http://auth-server:39003/login"
# ---> 200 (form with _csrf from hidden input)

curl -v -b /tmp/cookie -c /tmp/cookie -d \
 'username=user1&password=password&_csrf=0e3feb7f-269b-4e63-a83a-fa013e0ef065' \
 http://auth-server:39003/login
# ---> 302
# cookies
#HttpOnly_auth-server	FALSE	/	FALSE	0	JSESSIONID	517416654F5B693783DB0FF0CD46CEB7
#HttpOnly_localhost	FALSE	/	FALSE	0	JSESSIONID	4BF7F1430034D73CCC5DE33A0A1B287A

curl -v -b /tmp/cookie -c /tmp/cookie \
 "http://auth-server:39003/oauth2/authorize?response_type=code&client_id=messaging-client&scope=message.read%20message.write&state=8pXT0n956RZodBxoIoLWI1hhgcTZuhfPbmxZ_zxsHEU%3D&redirect_uri=http://localhost:39001/authorized"
# ---> 302

curl -v -b /tmp/cookie -c /tmp/cookie \
 "http://localhost:39001/authorized?code=c5LN_DFLYAgv8i93-5npDP8u4xBBAdD8dnb7_XujkAk%3D&state=8pXT0n956RZodBxoIoLWI1hhgcTZuhfPbmxZ_zxsHEU%3D"
# ---> 302

curl -v -b /tmp/cookie -c /tmp/cookie "http://localhost:39001/authorize?grant_type=authorization_code"
# ---> 200 (resource result)
```

## logging TCP through tcptunnel (used by runall.sh)
tcptunnel-1.2.0.jar - see https://github.com/mike-seger/java-tcp-tunnel

## TCP Dump

### mac OS
`sudo tcpdump -i lo0 -A -s 0 "portrange 39001-39003" | tee ../build/tcpdump.log`

### Linux
`sudo tcpdump -i lo -A -s 0 "portrange 39001-39003" | tee ../build/tcpdump.log`
