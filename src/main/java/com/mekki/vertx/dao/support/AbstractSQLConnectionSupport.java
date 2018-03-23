package com.mekki.vertx.dao.support;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.ext.jdbc.impl.JDBCClientImpl;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

/**
 * Created by Mekki on 2018/3/23.
 * 对DAO层SQL连接操作进行封装
 */
public abstract class AbstractSQLConnectionSupport {

    private static Logger logger = LoggerFactory.getLogger(AbstractSQLConnectionSupport.class);

    public abstract JDBCClientImpl getSqlClient();

    /**
     * 获取Sql链接
     *
     * @param handler
     */
    protected void getSQLConnection(Handler<SQLConnection> handler) {
        getSqlClient().getConnection(connectionHandler -> {
            requireSucceed(connectionHandler);
            SQLConnection connection = connectionHandler.result();

            logger.info("establish : {}",connection.toString());
            handler.handle(connection);
        });
    }

    /**
     * 执行完操作关闭Sql链接
     *
     * @param connection SQL连接
     */
    protected void closeSQLConnectionAfterExecute(SQLConnection connection) {
        connection.close(ar -> {
            requireSucceed(ar);
            logger.info("auto close {}", connection.toString());
        });
    }

    /**
     * 确保SQL连接发生异常时能够关闭
     *
     * @param action     操作
     * @param handler    操作
     * @param connection SQL连接
     * @param <E>        实体类型
     */
    protected <E> void closeSQLConnectionOnException(Consumer<E> action, E handler, SQLConnection connection) {
        try {
            action.accept(handler);
        } catch (Exception ex) {
            closeSQLConnectionAfterExecute(connection);
            throw new RuntimeException(ex);
        }
    }


    /**
     * 使用SQL执行查询操作
     *
     * @param sql     SQL
     * @param handler 更新结果
     */
    protected void doQuery(String sql, Handler<ResultSet> handler) {
        getSQLConnection(connection -> {
            connection.query(sql, asyncResult -> {
                closeSQLConnectionOnException(this::requireSucceed, asyncResult, connection);
                closeSQLConnectionOnException(handler::handle, asyncResult.result(), connection);

                closeSQLConnectionAfterExecute(connection);
            });
        });
    }

    /**
     * 使用SQL执行更新操作
     *
     * @param sql     SQL
     * @param handler 更新结果
     */
    protected void doUpdate(String sql, Handler<UpdateResult> handler) {
        getSQLConnection(connection -> {
            connection.update(sql, asyncResult -> {
                closeSQLConnectionOnException(this::requireSucceed, asyncResult, connection);
                closeSQLConnectionOnException(handler::handle, asyncResult.result(), connection);

                closeSQLConnectionAfterExecute(connection);
            });
        });
    }

    protected <T> void requireSucceed(AsyncResult<T> asyncResult) {
        if (!asyncResult.succeeded()) {
            throw new RuntimeException(asyncResult.cause());
        }
    }
}
