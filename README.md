# arangodb-dbg-user-401

     * before executing:
     * - start a local cluster reachable at coordinator1:8529 and coordinator2:8529, with root password "test"
     * - create a db user:
     * $ curl -u root:test http://coordinator1:8529/_api/user -d '{"user": "user", "passwd": "test"}'
     *
     * expected behavior:
     * {"error":false,"code":201,"result":true}
     * {"error":false,"code":200, ...
     *
     * actual behavior:
     * {"error":false,"code":201,"result":true}
     * {"error":true,"errorNum":11,"errorMessage":"not authorized to execute this request","code":401}
     *


```shell script
mvn compile
mvn exec:java -Dexec.mainClass=App
```