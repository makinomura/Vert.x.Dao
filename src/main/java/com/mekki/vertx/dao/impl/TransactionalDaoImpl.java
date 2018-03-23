package com.mekki.vertx.dao.impl;

import com.mekki.vertx.dao.TransactionalDao;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.SQLConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

/**
 * Created by Mekki on 2018/3/23.
 * 支持事务的DAO层实现
 */
public class TransactionalDaoImpl extends DefaultDaoImpl implements TransactionalDao {

    private static Logger logger = LoggerFactory.getLogger(TransactionalDaoImpl.class);

    private SQLConnection connection;

    private TransactionalDaoImpl(Vertx vertx, JsonObject jdbcConfig) {
        super(vertx, jdbcConfig);
    }

    /**
     * 构造 TransactionalDaoImpl 对象
     *
     * @param vertx      Vert.x对象
     * @param jdbcConfig jdbc 配置
     * @param handler    TransactionalDaoImpl 对象
     */
    public static void createTransactional(Vertx vertx, JsonObject jdbcConfig, Handler<TransactionalDaoImpl> handler) {
        new TransactionalDaoImpl(vertx, jdbcConfig).init(handler);
    }

    /**
     * 加载sql连接并打开事务
     *
     * @param handler TransactionalDaoImpl 对象
     */
    private void init(Handler<TransactionalDaoImpl> handler) {
        super.getSQLConnection(connection -> {
            this.connection = connection;
            connection.setAutoCommit(false, resultHandler -> {
                handler.handle(this);
            });
        });
    }

    @Override
    protected void getSQLConnection(Handler<SQLConnection> handler) {
        handler.handle(connection);
    }

    /**
     * 需要手动关闭
     * @param connection SQL连接
     */
    @Override
    protected void closeSQLConnectionAfterExecute(SQLConnection connection) {}

    @Override
    protected <E> void closeSQLConnectionOnException(Consumer<E> action, E handler, SQLConnection connection) {
        try {
            action.accept(handler);
        } catch (Exception ex) {
            rollback(ex);
            throw new RuntimeException(ex);
        }
    }

    /**
     * 提交事务
     *
     * @param handler 完成后回调
     */
    @Override
    public void commit(Handler<Void> handler) {
        getSQLConnection(connection -> {
            connection.commit(ar -> {
                super.requireSucceed(ar);
                logger.info("commit {}", connection.toString());
                handler.handle(ar.result());
            });
        });
    }

    /**
     * 提交事务
     */
    public void commit() {
        commit(c -> {
        });
    }

    /**
     * 提交事务并关闭链接
     */
    public void commitAndClose() {
        commit(c -> {
            close();
        });
    }

    /**
     * 提交事务并关闭链接
     *
     * @param handler 完成后回调
     */
    public void commitAndClose(Handler<Void> handler) {
        commit(c -> {
            close(handler);
        });
    }

    /**
     * 回滚事务
     *
     * @param handler 完成后回调
     */
    @Override
    public void rollback(Handler<Void> handler) {
        getSQLConnection(connection -> {
            connection.rollback(ar -> {
                super.requireSucceed(ar);
                logger.info("rollback {}", connection.toString());
                handler.handle(ar.result());
            });
        });
    }

    /**
     * 回滚事务
     */
    public void rollback() {
        rollback(r -> {
        });
    }

    /**
     * 提交事务并关闭链接
     */
    public void rollbackAndClose() {
        rollback(c -> {
            close();
        });
    }

    /**
     * 提交事务并关闭链接
     *
     * @param handler 完成后回调
     */
    public void rollbackAndClose(Handler<Void> handler) {
        rollback(c -> {
            close(handler);
        });
    }


    /**
     * 回滚关闭事务
     *
     * @param throwable 异常
     */
    private void rollback(Throwable throwable) {
        rollback(
            ar -> {
                close(h -> {
                    logger.warn("auto rollback and close cause : {}", throwable.getMessage());
                });
            }
        );
    }

    /**
     * 关闭SQL连接
     *
     * @param handler 回调
     */
    @Override
    public void close(Handler<Void> handler) {
        getSQLConnection(connection -> {
            connection.close(ar -> {
                super.requireSucceed(ar);
                logger.info("close {}", connection.toString());
                handler.handle(null);
            });
        });
    }

    /**
     * 关闭SQL连接
     */
    public void close() {
        close(c -> {
        });
    }
}
