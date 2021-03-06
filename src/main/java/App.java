import io.netty.handler.codec.http.HttpHeaderNames;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufFlux;
import reactor.netty.http.client.HttpClient;

import java.util.UUID;

/**
 * @author Michele Rastelli
 */
public class App {

    // root:test
    static HttpClient rootClient = createClient("coordinator2", 8529, "Basic cm9vdDp0ZXN0");

    // user:test
    static HttpClient userClient1 = createClient("coordinator1", 8529, "Basic dXNlcjp0ZXN0");

    // user:test
    static HttpClient userClient2 = createClient("coordinator2", 8529, "Basic dXNlcjp0ZXN0");

    static String errMsg = "not authorized to execute this request";

    /**
     * before executing:
     * - start a local cluster reachable at coordinator1:8529 and coordinator2:8529, with root password "test"
     * - create a db user:
     * $ curl -u root:test http://coordinator1:8529/_api/user -d '{"user": "user", "passwd": "test"}'
     * <p>
     * expected behavior:
     * {"error":false,"code":201,"result":true}
     * {"error":false,"code":200, ...
     * <p>
     * actual behavior:
     * {"error":false,"code":201,"result":true}
     * {"error":true,"errorNum":11,"errorMessage":"not authorized to execute this request","code":401}
     */
    public static void main(String[] args) throws InterruptedException {

        rootClient.warmup().block();
        userClient1.warmup().block();

        for (int i = 0; i < 100; i++) {
            System.out.println();

            String dbName = "db-" + UUID.randomUUID().toString();

            // create db giving permissions to user
            System.out.println("Sending root request to create database to coordinator2: " + dbName);
            String createDbRes = sendCreateDbRequest(dbName);
            System.out.println("--> " + createDbRes);

            // create collection within the created db, with user credentials
            System.out.println("Sending user request to create collection to coordinator1 ...");
            String createCollectionRes = sendUserRequest(dbName, userClient1);
            System.out.println("--> " + createCollectionRes);

            if (createCollectionRes.contains(errMsg)) {
                System.out.println("retrying in 5 seconds...");
                Thread.sleep(5000);
                System.out.println("Sending user request to create collection to coordinator1 ...");
                String c1Res = sendUserRequest(dbName, userClient1);
                System.out.println("--> " + c1Res);

                if (c1Res.contains(errMsg)) {
                    System.out.println("Sending user request to create collection to coordinator2 ...");
                    String c2Res = sendUserRequest(dbName, userClient2);
                    System.out.println("--> " + c2Res);
                    if (c2Res.contains(errMsg)) {
                        System.out.println("giving up...");
                        System.exit(1);
                    }
                }
            }
        }
    }

    static String sendCreateDbRequest(String dbName) {
        String createDbBody = "{\"name\": \"" + dbName + "\", \"users\": [{\"username\": \"user\"}]}";
        return rootClient
                .headers(h -> h.set(HttpHeaderNames.CONTENT_LENGTH, createDbBody.length()))
                .post()
                .uri("/_api/database")
                .send(ByteBufFlux.fromString(Mono.just(createDbBody)))
                .responseContent()
                .aggregate()
                .asString()
                .block();
    }

    static String sendUserRequest(String dbName, HttpClient httpClient) {
        String createCollectionBody = "{\"name\": \"myCol\"}";
        return httpClient
                .headers(h -> h.set(HttpHeaderNames.CONTENT_LENGTH, createCollectionBody.length()))
                .post()
                .uri("/_db/" + dbName + "/_api/collection")
                .send(ByteBufFlux.fromString(Mono.just(createCollectionBody)))
                .responseContent()
                .aggregate()
                .asString()
                .block();
    }

    static HttpClient createClient(String host, int port, String authorization) {
        return HttpClient.create()
                .headers(h -> {
                    h.set(HttpHeaderNames.AUTHORIZATION, authorization);
                    h.add(HttpHeaderNames.CONTENT_TYPE, "application/json");
                    h.add(HttpHeaderNames.ACCEPT, "application/json");
                })
                .wiretap(true)
                .host(host)
                .port(port);
    }
}

