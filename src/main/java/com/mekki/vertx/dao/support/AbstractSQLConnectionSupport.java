package com.mekki.vertx.dao.support;

import com.mekki.vertx.dao.support.exception.UnhandledException;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.ext.jdbc.impl.JDBCClientImpl;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Mekki on 2018/3/23.
 * 对DAO层SQL连接操作进行封装
 */
public abstract class AbstractSQLConnectionSupport {

    private static Logger logger = LoggerFactory.getLogger(AbstractSQLConnectionSupport.class);
    protected Handler<Exception> defaultExceptionHandler = ex -> {
        throw new UnhandledException(ex);
    };

    public abstract JDBCClientImpl getSqlClient();

    /**
     * 设置异常处理
     *
     * @param eh 异常处理回调
     */
    public void onException(Handler<Exception> eh) {
        defaultExceptionHandler = eh;
    }

    /**
     * 获取Sql链接
     *
     * @param handler
     */
    protected void getSQLConnection(Handler<SQLConnection> handler) {
        getSqlClient().getConnection(connectionHandler -> {
            requireSucceed(connectionHandler);
            SQLConnection connection = connectionHandler.result();

            logger.info("establish : {}", connection.toString());
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
     * 处理异常
     *
     * @param vh  操作
     */
    protected void handleIfException(Handler<Void> vh) {
        try {
            vh.handle(null);
        } catch (Exception ex) {
            defaultExceptionHandler.handle(ex);
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
            connection.query(sql, ar -> {
                handleIfException(v -> {
                    requireSucceed(ar);
                    handler.handle(ar.result());
                });
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
            connection.update(sql, ar -> {
                handleIfException(v -> {
                    requireSucceed(ar);
                    handler.handle(ar.result());
                });
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
