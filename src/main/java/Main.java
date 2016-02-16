import io.vertx.core.Vertx;

/**
 * Created by zhaoboliu on 2/15/16.
 */
public class Main {
    public static void main(String[] args) {
        // Create an HTTP server which simply returns "Hello World!" to each request.
        Vertx.vertx()
                .createHttpServer()
                .requestHandler(req -> req.response().end("Hello World!"))
                .listen(8080);
    }
}
