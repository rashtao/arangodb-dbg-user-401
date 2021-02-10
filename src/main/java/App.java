import io.netty.handler.codec.http.HttpHeaderNames;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufFlux;
import reactor.netty.http.client.HttpClient;

import java.util.UUID;

/**
 * @author Michele Rastelli
 */
public class App {

    /**
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
     */
    public static void main(String[] args) {

        // root:test
        HttpClient rootClient = createClient("coordinator1", 8529, "Basic cm9vdDp0ZXN0");
        rootClient.warmup().block();

        // user:test
        HttpClient userClient = createClient("coordinator2", 8529, "Basic dXNlcjp0ZXN0");
        userClient.warmup().block();

        for (int i = 0; i < 100; i++) {
            System.out.println();

            String dbName = "db-" + UUID.randomUUID().toString();

            // create db giving permissions to user
            String createDbBody = "{\"name\": \"" + dbName + "\", \"users\": [{\"username\": \"user\"}]}";
            String createDbRes = rootClient
                    .headers(h -> h.set(HttpHeaderNames.CONTENT_LENGTH, createDbBody.length()))
                    .post()
                    .uri("/_api/database")
                    .send(ByteBufFlux.fromString(Mono.just(createDbBody)))
                    .responseContent()
                    .aggregate()
                    .asString()
                    .block();

            System.out.println(createDbRes);

            // create collection within the created db, with user credentials
            String createCollectionBody = "{\"name\": \"myCol\"}";
            String createCollectionRes = userClient
                    .headers(h -> h.set(HttpHeaderNames.CONTENT_LENGTH, createCollectionBody.length()))
                    .post()
                    .uri("/_db/" + dbName + "/_api/collection")
                    .send(ByteBufFlux.fromString(Mono.just(createCollectionBody)))
                    .responseContent()
                    .aggregate()
                    .asString()
                    .block();

            System.out.println(createCollectionRes);
        }
    }

    private static HttpClient createClient(String host, int port, String authorization) {
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

