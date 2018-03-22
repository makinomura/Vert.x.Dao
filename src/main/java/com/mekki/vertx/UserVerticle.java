package com.mekki.vertx;

import com.mekki.vertx.dao.DefaultDaoImpl;
import com.mekki.vertx.entity.User;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.json.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

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

        App.router.post("/user").handler(routingContext -> {
                add(routingContext.request().params(), hr -> {
                    routingContext.response().end(hr);
                });
            }
        );

        App.router.put("/user/:id").handler(routingContext -> {
                update(routingContext.request().params(), hr -> {
                    routingContext.response().end(hr);
                });
            }
        );

        App.router.delete("/user/:id").handler(routingContext -> {
                delete(routingContext.request().params(), hr -> {
                    routingContext.response().end(hr);
                });
            }
        );
    }

    private void list(MultiMap params, Handler<String> handler) {
        User user = new User();

        DefaultDaoImpl.create(vertx, jdbcConfig, th ->
            th.select(user, z ->
                handler.handle(Json.encode(z))
            )
        );
    }

    private void one(MultiMap params, Handler<String> handler) {
        User user = new User();

        user.setId(Integer.valueOf(params.get("id")));

        DefaultDaoImpl.create(vertx, jdbcConfig, th ->
            th.select(user, z ->
                handler.handle(Json.encode(z.size() > 0 ? z.get(0) : null))
            )
        );
    }

    private void add(MultiMap params, Handler<String> handler) {
        User user = new User();

        user.setOpenId(params.get("openId"));
        user.setSpcCardNum(params.get("spcCardNum"));
        user.setSubscribeTime(new Date());
        user.setStatus(1);

        DefaultDaoImpl.createTransactional(vertx, jdbcConfig, th ->
            th.insert(user, z -> {
                user.setStatus(1);

                th.update(user, u -> {
                    th.commit(v -> {
                        th.close(c -> {
                            handler.handle(Json.encode(user));
                        });
                    });
                });
            })
        );
    }

    private void update(MultiMap params, Handler<String> handler) {
        User user = new User();

        user.setId(Integer.valueOf(params.get("id")));

        DefaultDaoImpl.create(vertx, jdbcConfig, th ->
            th.select(user, s -> {
                if (s.size() == 0) {
                    handler.handle("id " + user.getId() + " does not exists.");
                } else {
                    User one = s.get(0);

                    one.setOpenId(params.get("openId"));
                    one.setSpcCardNum(params.get("spcCardNum"));
                    th.update(one, z -> {
                        handler.handle(Json.encode(one));
                    });
                }
            })
        );
    }

    private void delete(MultiMap params, Handler<String> handler) {
        User user = new User();

        user.setId(Integer.valueOf(params.get("id")));

        DefaultDaoImpl.create(vertx, jdbcConfig, th ->
            th.select(user, s -> {
                if (s.size() == 0) {
                    handler.handle("id " + user.getId() + " does not exists.");
                } else {
                    User one = s.get(0);
                    th.delete(one, z -> {
                        handler.handle(Json.encode(z));
                    });
                }
            })
        );
    }
}
