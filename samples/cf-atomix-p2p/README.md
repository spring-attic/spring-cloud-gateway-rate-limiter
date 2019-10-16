# Summary

This is a PoC of using [Atomix](https://atomix.io/) peer-to-peer Raft cluster to implement Spring Cloud Gateway Rate Limiter for horizontally scaled CF app.

# Build and deploy

```
$ cd samples/cf-hazelcast-p2p
$ ../../gradlew assemble
$ cf push --var cf-apps-domain=<your cf env domain>
$ ./configure-network.sh
```
# Test

```
$ curl -vv -k https://peer.<your cf env domain>/google
# should return 403 - no header
$ curl -vv -k https://peer.<your cf env domain>/google -H "X-API-Key: foo"
# response from Google (404) + X-Remaining-Limit header
```
