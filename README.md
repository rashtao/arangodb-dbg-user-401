# arangodb-dbg-user-401

Before executing:
- start a local cluster reachable at `coordinator1:8529` and `coordinator2:8529`, with root password `test`
- create a db user:
```shell script
$ curl -u root:test http://coordinator1:8529/_api/user -d '{"user": "user", "passwd": "test"}'
```

expected behavior:
```shell script
{"error":false,"code":201,"result":true}
{"error":false,"code":200, ...
```

actual behavior:
```shell script
{"error":false,"code":201,"result":true}
{"error":true,"errorNum":11,"errorMessage":"not authorized to execute this request","code":401}
```

## execute

```shell script
mvn compile
mvn exec:java -Dexec.mainClass=App
```
