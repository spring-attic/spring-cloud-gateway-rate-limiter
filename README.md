# Summary

This is a PoC of using Hazelcast peer-to-peer cluster to implement Spring Cloud Gateway Rate Limiter for horizontally scaled CF app.

# Build and deploy

```
$ ./gradlew assemble
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
