package com.mekki.vertx;

import com.mekki.vertx.dao.DefaultDaoImpl;
import com.mekki.vertx.entity.User;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.json.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.mekki.vertx.App.jdbcConfig;

/**
 * Created by Mekki on 2018/3/21.
 */
public class UserVerticle extends AbstractVerticle {
    private static Logger logger = LoggerFactory.getLogger(UserVerticle.class);

    @Override
    public void start() throws Exception {

        App.router.get("/user").handler(routingContext -> {
                list(routingContext.request().params(), hr -> {
                    routingContext.response().end(hr);
                });
            }
        );

        App.router.get("/user/:id").handler(routingContext -> {
                one(routingContext.request().params(), hr -> {
                    routingContext.response().end(hr);
                });
            }
        );
    }

    private void list(MultiMap params, Handler<String> handler) {
        User user = new User();

        DefaultDaoImpl.createTransactional(vertx, jdbcConfig, th ->
            th.select(user, z ->
                handler.handle(Json.encode(z))
            )
        );
    }

    private void one(MultiMap params, Handler<String> handler) {
        User user = new User();

        user.setId(Integer.valueOf(params.get("id")));
        DefaultDaoImpl.createTransactional(vertx, jdbcConfig, th ->
            th.select(user, z ->
                handler.handle(Json.encode(z.size() > 0 ? z.get(0) : null))
            )
        );
    }
}
