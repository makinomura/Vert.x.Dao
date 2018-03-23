package com.mekki.vertx.dao;

import com.mekki.vertx.dao.impl.DefaultDaoImpl;
import com.mekki.vertx.dao.impl.TransactionalDaoImpl;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

/**
 * Created by Mekki on 2018/3/23.
 * Vert.x.DAO
 */
public interface Dao {

    /**
     * 构造 DefaultDaoImpl 对象
     *
     * @param vertx      Vert.x对象
     * @param jdbcConfig jdbc 配置
     * @param handler    DefaultDaoImpl 对象
     */
    static void create(Vertx vertx, JsonObject jdbcConfig, Handler<DefaultDaoImpl> handler) {
        TransactionalDaoImpl.create(vertx, jdbcConfig, handler);
    }

    /**
     * 构造 TransactionalDaoImpl 对象
     *
     * @param vertx      Vert.x对象
     * @param jdbcConfig jdbc 配置
     * @param handler    TransactionalDaoImpl 对象
     */
    static void createTransactional(Vertx vertx, JsonObject jdbcConfig, Handler<TransactionalDaoImpl> handler) {
        TransactionalDaoImpl.createTransactional(vertx, jdbcConfig, handler);
    }
}
