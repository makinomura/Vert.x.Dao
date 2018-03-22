package com.mekki.vertx;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.TimeoutHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App extends AbstractVerticle {
    public static Vertx vertx = Vertx.vertx();
    public static HttpServer server;
    public static Router router;
    public static JsonObject jdbcConfig = new JsonObject()
        .put("driver_class", "com.mysql.cj.jdbc.Driver")
        .put("url", "jdbc:mysql://localhost:3306/wechat?characterEncoding=utf-8&useSSL=true&serverTimezone=Asia/Shanghai")
        .put("user", "root")
        .put("password", "123456")
        .put("max_pool_size", 20)
        .put("initial_pool_size", 3)
        .put("min_pool_size", 1)
        .put("max_statements", 20)
        .put("max_idle_time", 6000);
    private static Logger logger = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        vertx.deployVerticle(new App());
    }

    @Override
    public void start() throws Exception {

        router = Router.router(vertx);
        router.route().handler(CorsHandler.create("*")
            .allowedMethod(HttpMethod.GET)
            .allowedMethod(HttpMethod.POST)
            .allowedMethod(HttpMethod.PUT)
            .allowedMethod(HttpMethod.DELETE)
            .allowCredentials(false)
        );

        router.route().handler(BodyHandler.create());
        router.route().handler(TimeoutHandler.create(5000));

        server = vertx.createHttpServer();
        server.requestHandler(router::accept).listen(8080, listenHandler -> {
            if (listenHandler.succeeded()) {
                vertx.deployVerticle(new UserVerticle());
            }
        });
    }
}
