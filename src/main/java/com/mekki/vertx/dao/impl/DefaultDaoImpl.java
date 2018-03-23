package com.mekki.vertx.dao.impl;

import com.mekki.vertx.dao.support.AbstractSQLConnectionSupport;
import com.mekki.vertx.dao.EnhancedDao;
import com.mekki.vertx.dao.support.EntitySQLSupport;
import com.mekki.vertx.dao.SimpleCurdDao;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.jdbc.impl.JDBCClientImpl;
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
public class DefaultDaoImpl extends AbstractSQLConnectionSupport implements SimpleCurdDao, EnhancedDao {

    private static Logger logger = LoggerFactory.getLogger(DefaultDaoImpl.class);

    /**
     * SQL类缓存
     */
    private static Map<String, EntitySQLSupport> sqlSupportCache = new HashMap<>();
    private JsonObject jdbcConfig;
    private Vertx vertx;
    private JDBCClientImpl sqlClient;

    protected DefaultDaoImpl(Vertx vertx, JsonObject jdbcConfig) {
        this.jdbcConfig = jdbcConfig;
        this.vertx = vertx;

        sqlClient = (JDBCClientImpl) JDBCClient.createShared(this.vertx, this.jdbcConfig);
        logger.info("jdbc config -> {}", jdbcConfig.toString());
    }

    private static <E> E convert(JsonObject obj, Class<E> clazz) {
        return Json.decodeValue(obj.toString(), clazz);
    }

    private static <T> EntitySQLSupport<T> getSQLSupport(Class<T> clazz) {
        if (!sqlSupportCache.containsKey(clazz.getName())) {
            sqlSupportCache.put(clazz.getName(), new EntitySQLSupport(clazz));
        }

        return sqlSupportCache.get(clazz.getName());
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
     * 查询
     *
     * @param e       实体
     * @param handler 查询结果
     * @param <E>     实体类型
     */
    @Override
    public <E> void select(E e, Handler<List<E>> handler) {

        String sql = getSQLSupport((Class<E>) e.getClass()).buildSelectSql(e);
        logger.info("select: {}", sql);

        doQuery(sql, rs -> handler.handle(rs.getRows().stream()
            .map(i -> convert(i, (Class<E>) e.getClass()))
            .collect(Collectors.toList())));
    }

    /**
     * 新增
     *
     * @param e       实体
     * @param handler 影响行数
     * @param <E>     实体类型
     */
    @Override
    public <E> void insert(E e, Handler<Integer> handler) {
        EntitySQLSupport<E> sqlSupport = getSQLSupport((Class<E>) e.getClass());

        String sql = sqlSupport.buildInsertSql(e, true);
        logger.info("insert: {}", sql);

        doUpdate(sql, ur -> {
            sqlSupport.rewritePkValue(e, ur);
            handler.handle(ur.getUpdated());
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
        EntitySQLSupport<E> sqlSupport = getSQLSupport((Class<E>) e.getClass());

        String sql = sqlSupport.buildUpdateSql(e, true);
        logger.info("update: {}", sql);

        doUpdate(sql, ur -> handler.handle(ur.getUpdated()));
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
        EntitySQLSupport<E> sqlSupport = getSQLSupport((Class<E>) e.getClass());

        String sql = sqlSupport.buildDeleteSql(e);
        logger.info("delete: {}", sql);

        doUpdate(sql, ur -> handler.handle(ur.getUpdated()));
    }

    public JDBCClientImpl getSqlClient() {
        return sqlClient;
    }

    /**
     * 查询一个（返回多个会抛出异常）
     *
     * @param e       实体
     * @param handler 结果
     * @param <E>     实体类型
     */
    @Override
    public <E> void selectOne(E e, Handler<E> handler) {
        select(e, h -> {
            if (h.size() > 1) {
                throw new RuntimeException("Expect one, but found " + h.size());
            }

            if (h.size() == 0) {
                handler.handle(null);
            } else {
                handler.handle(h.get(0));
            }
        });
    }

    /**
     * 查询数量
     *
     * @param e       实体
     * @param handler 数量
     * @param <E>     实体类型
     */
    @Override
    public <E> void selectCount(E e, Handler<Long> handler) {
        String sql = getSQLSupport((Class<E>) e.getClass()).buildSelectCountSql(e);
        logger.info("selectCount: {}", sql);

        doQuery(sql, rs -> handler.handle(rs.getRows().get(0).getLong("count")));
    }

    /**
     * 新增（NULL字段会忽略）
     *
     * @param e       实体
     * @param handler 影响行数
     * @param <E>     实体类型
     */
    @Override
    public <E> void insertSelective(E e, Handler<Integer> handler) {
        EntitySQLSupport<E> sqlSupport = getSQLSupport((Class<E>) e.getClass());

        String sql = sqlSupport.buildInsertSql(e, false);
        logger.info("insertSelective: {}", sql);

        doUpdate(sql, ur -> {
            sqlSupport.rewritePkValue(e, ur);
            handler.handle(ur.getUpdated());
        });
    }

    /**
     * 更新（NULL字段会忽略）
     *
     * @param e       实体
     * @param handler 影响行数
     * @param <E>     实体类型
     */
    @Override
    public <E> void updateSelective(E e, Handler<Integer> handler) {
        EntitySQLSupport<E> sqlSupport = getSQLSupport((Class<E>) e.getClass());

        String sql = sqlSupport.buildUpdateSql(e, false);
        logger.info("updateSelective: {}", sql);

        doUpdate(sql, ur -> handler.handle(ur.getUpdated()));
    }
}
