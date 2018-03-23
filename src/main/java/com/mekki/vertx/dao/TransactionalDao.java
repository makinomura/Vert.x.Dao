package com.mekki.vertx.dao;

import io.vertx.core.Handler;

/**
 * Created by Mekki on 2018/3/23.
 * 支持事务的DAO层接口
 */
public interface TransactionalDao {
    void commit(Handler<Void> handler);

    void rollback(Handler<Void> handler);

    void close(Handler<Void> handler);
}
