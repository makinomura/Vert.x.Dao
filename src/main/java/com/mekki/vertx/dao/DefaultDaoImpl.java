package com.mekki.vertx.dao;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.jdbc.impl.JDBCClientImpl;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by Mekki on 2018/3/21.
 * DAO层实现
 */
@SuppressWarnings("unchecked")
public class DefaultDaoImpl implements SimpleCurdDao {

    private static Logger logger = LoggerFactory.getLogger(DefaultDaoImpl.class);

    /**
     * 实体类缓存
     */
    private static Map<String, EntityDesc> entityDescCache = new HashMap<>();
    private JsonObject jdbcConfig;
    private Vertx vertx;
    private JDBCClientImpl sqlClient;

    private DefaultDaoImpl(Vertx vertx, JsonObject jdbcConfig) {
        this.jdbcConfig = jdbcConfig;
        this.vertx = vertx;

        sqlClient = (JDBCClientImpl) JDBCClient.createShared(this.vertx, this.jdbcConfig);
        logger.info("jdbc config -> {}", jdbcConfig.toString());
    }

    private static <E> E convert(JsonObject obj, Class<E> clazz) {
        return Json.decodeValue(obj.toString(), clazz);
    }

    private static <T> EntityDesc<T> getEntityDesc(Class<T> clazz) {
        if (!entityDescCache.containsKey(clazz.getName())) {
            entityDescCache.put(clazz.getName(), new EntityDesc(clazz));
        }

        return entityDescCache.get(clazz.getName());
    }

    /**
     * 构造 DefaultDaoImpl 对象
     *
     * @param vertx      Vert.x对象
     * @param jdbcConfig jdbc 配置
     * @param handler    DefaultDaoImpl 对象
     */
    public static void create(Vertx vertx, JsonObject jdbcConfig, Handler<DefaultDaoImpl> handler) {
        handler.handle(new DefaultDaoImpl(vertx, jdbcConfig));
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
     * 获取Sql链接
     *
     * @param handler
     */
    protected void getSQLConnection(Handler<SQLConnection> handler) {
        sqlClient.getConnection(connectionHandler -> {
            requireSucceed(connectionHandler);

            handler.handle(connectionHandler.result());
        });
    }

    public JDBCClientImpl getSqlClient() {
        return sqlClient;
    }

    /**
     * 查询
     *
     * @param e       实体
     * @param handler 查询结果
     * @param <E>     实体类型
     */
    @Override
    public <E> void select(E e, Handler<List<E>> handler) {

        String sql = getEntityDesc((Class<E>) e.getClass()).buildSelectSql(e);
        logger.info("select: {}", sql);

        getSQLConnection(connection -> {
            connection.query(sql, asyncResult -> {
                requireSucceed(asyncResult);

                handler.handle(asyncResult.result().getRows().stream().map(i -> convert(i, (Class<E>) e.getClass())).collect(Collectors.toList()));
            });
        });
    }

    /**
     * 新增（NULL字段会忽略）
     *
     * @param e       实体
     * @param handler 影响行数
     * @param <E>     实体类型
     */
    @Override
    public <E> void insert(E e, Handler<Integer> handler) {
        EntityDesc<E> entityDesc = getEntityDesc((Class<E>) e.getClass());

        String sql = entityDesc.buildInsertSql(e);
        logger.info("insert: {}", sql);

        getSQLConnection(connection -> {
            connection.update(sql, asyncResult -> {
                requireSucceed(asyncResult);

                UpdateResult result = asyncResult.result();

                entityDesc.rewritePkValue(e, result);
                handler.handle(result.getUpdated());
            });
        });
    }

    /**
     * 更新
     *
     * @param e       实体
     * @param handler 影响行数
     * @param <E>     实体类型
     */
    @Override
    public <E> void update(E e, Handler<Integer> handler) {
        EntityDesc<E> entityDesc = getEntityDesc((Class<E>) e.getClass());

        String sql = entityDesc.buildUpdateSql(e);
        logger.info("update: {}", sql);

        getSQLConnection(connection -> {
            connection.update(sql, asyncResult -> {
                requireSucceed(asyncResult);

                UpdateResult result = asyncResult.result();
                handler.handle(result.getUpdated());
            });
        });
    }

    /**
     * 删除
     *
     * @param e       实体
     * @param handler 影响行数
     * @param <E>     实体类型
     */
    @Override
    public <E> void delete(E e, Handler<Integer> handler) {
        EntityDesc<E> entityDesc = getEntityDesc((Class<E>) e.getClass());

        String sql = entityDesc.buildDeleteSql(e);
        logger.info("delete: {}", sql);

        getSQLConnection(connection -> {
            connection.update(sql, asyncResult -> {
                requireSucceed(asyncResult);

                UpdateResult result = asyncResult.result();
                handler.handle(result.getUpdated());
            });
        });
    }

    protected <T> void requireSucceed(AsyncResult<T> asyncResult) {
        if (!asyncResult.succeeded()) {
            throw new RuntimeException(asyncResult.cause());
        }
    }

    /**
     * 支持事务的DAO层实现
     */
    public static class TransactionalDaoImpl extends DefaultDaoImpl {

        private SQLConnection sqlConnection;

        private TransactionalDaoImpl(Vertx vertx, JsonObject jdbcConfig) {
            super(vertx, jdbcConfig);
        }

        /**
         * 加载sql连接并打开事务
         *
         * @param handler TransactionalDaoImpl 对象
         */
        protected void init(Handler<TransactionalDaoImpl> handler) {

            getSqlClient().getConnection(connectionHandler -> {
                requireSucceed(connectionHandler);

                sqlConnection = connectionHandler.result();
                sqlConnection.setAutoCommit(false, resultHandler -> {
                    handler.handle(this);
                });
            });
        }

        @Override
        protected void getSQLConnection(Handler<SQLConnection> handler) {
            handler.handle(sqlConnection);
        }

        /**
         * 提交事务
         *
         * @param handler
         */
        public void commit(Handler<Void> handler) {
            sqlConnection.commit(ar -> {
                super.requireSucceed(ar);
                logger.info("commit {}" + sqlConnection.toString());
                handler.handle(ar.result());
            });
        }

        /**
         * 回滚事务
         *
         * @param handler
         */
        public void rollback(Handler<Void> handler) {
            sqlConnection.rollback(ar -> {
                super.requireSucceed(ar);
                logger.info("rollback {}", sqlConnection.toString());
                handler.handle(ar.result());
            });
        }

        /**
         * 关闭连接
         *
         * @param handler
         */
        public void close(Handler<Void> handler) {
            sqlConnection.close(ar -> {
                super.requireSucceed(ar);
                logger.info("close {}", sqlConnection.toString());
                handler.handle(null);
            });
        }

        @Override
        public <E> void select(E e, Handler<List<E>> handler) {
            try {
                super.select(e, handler);
            } catch (Exception ex) {
                rollback(
                    ar -> {
                        close(h -> {
                            throw new RuntimeException(ex);
                        });
                    }
                );
            }
        }

        @Override
        public <E> void insert(E e, Handler<Integer> handler) {
            try {
                super.insert(e, handler);
            } catch (Exception ex) {
                rollback(
                    ar -> {
                        close(h -> {
                            throw new RuntimeException(ex);
                        });
                    }
                );
            }
        }

        @Override
        public <E> void update(E e, Handler<Integer> handler) {
            try {
                super.update(e, handler);
            } catch (Exception ex) {
                rollback(
                    ar -> {
                        close(h -> {
                            throw new RuntimeException(ex);
                        });
                    }
                );
            }
        }

        @Override
        public <E> void delete(E e, Handler<Integer> handler) {
            try {
                super.delete(e, handler);
            } catch (Exception ex) {
                rollback(
                    ar -> {
                        close(h -> {
                            throw new RuntimeException(ex);
                        });
                    }
                );
            }
        }

        @Override
        protected <T> void requireSucceed(AsyncResult<T> asyncResult) {
            if (!asyncResult.succeeded()) {
                rollback(
                    ar -> {
                        close(h -> {
                            throw new RuntimeException(asyncResult.cause());
                        });
                    }
                );
            }
        }
    }
}
