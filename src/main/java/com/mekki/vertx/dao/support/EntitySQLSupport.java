package com.mekki.vertx.dao.support;

import io.vertx.ext.sql.UpdateResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.*;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by Mekki on 2018/3/21.
 * 实体->SQL工具类
 */
public class EntitySQLSupport<T> {

    private static Logger logger = LoggerFactory.getLogger(EntitySQLSupport.class);

    private Class<T> entityClass;

    private String tableName;

    private String pkName;

    private Field pkField;

    private String selectAllSql;

    private Map<String, String> columns;

    public EntitySQLSupport(Class<T> clazz) {
        entityClass = clazz;

        if (entityClass.getDeclaredFields().length == 0) {
            throw new RuntimeException("No fields in class " + clazz.getName());
        }

        resolveTableName();
        resolvePk();
        resolveColumns();
        resolveSelectSql();
        logger.info("built {}", clazz.getName());
    }

    /**
     * 驼峰转下划线
     *
     * @param camel
     * @return
     */
    private static String camel2Underline(String camel) {
        return camel.replaceAll("[A-Z]", "_$0").toLowerCase();
    }

    /**
     * 主键
     */
    private void resolvePk() {
        Optional<Field> fieldOptional = Stream.of(entityClass.getDeclaredFields())
            .filter(field -> field.getAnnotation(Id.class) != null)
            .findFirst();

        if (fieldOptional.isPresent()) {
            pkField = fieldOptional.get();

            pkName = resolveFieldName(pkField);
        }
    }

    /**
     * 表名
     */
    private void resolveTableName() {
        Table table = entityClass.getAnnotation(Table.class);

        if (table != null && table.name().length() > 0) {
            tableName = table.name();
        } else {
            tableName = camel2Underline(entityClass.getSimpleName());
        }
    }

    /**
     * 字段->数据库字段名
     *
     * @param field
     * @return
     */
    private String resolveFieldName(Field field) {
        field.setAccessible(true);

        Column column = field.getAnnotation(Column.class);

        String columnName;
        if (column != null && column.name().length() > 0) {
            columnName = column.name();
        } else {
            columnName = camel2Underline(field.getName());
        }

        return columnName;
    }

    /**
     * 字段
     */
    private void resolveColumns() {
        columns = Stream.of(entityClass.getDeclaredFields())
            .filter(field -> field.getAnnotation(Transient.class) == null)
            .collect(Collectors.toMap(Field::getName, this::resolveFieldName));
    }

    /**
     * SELECT ALL SQL
     */
    private void resolveSelectSql() {
        String columns = this.columns.entrySet().stream()
            .map(i -> "`" + i.getValue() + "` AS `" + i.getKey() + "`")
            .reduce((l, r) -> l + "," + r).orElse("");

        selectAllSql = "SELECT " + columns + " FROM `" + tableName + "`";
    }

    /**
     * 回写主键值
     *
     * @param item   实体
     * @param result 更新结果
     */
    public void rewritePkValue(T item, UpdateResult result) {

        if (pkField != null) {
            pkField.setAccessible(true);
            try {
                if (pkField.getType().equals(Integer.class)) {
                    pkField.set(item, result.getKeys().getInteger(0));
                } else if (pkField.getType().equals(Long.class)) {
                    pkField.set(item, result.getKeys().getLong(0));
                } else {
                    throw new RuntimeException("cannot rewrite pk value");
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 构造查询语句
     *
     * @param item
     * @return
     */
    public String buildSelectSql(T item) {
        return selectAllSql + buildWhereCondition(item) + ";";
    }

    /**
     * 构造Where条件
     *
     * @param item
     * @return
     */
    private String buildWhereCondition(T item) {
        Optional<String> whereName = columns.entrySet().stream().map(
            entry -> {
                try {
                    String fieldName = entry.getKey();
                    String columnName = entry.getValue();

                    Field field = entityClass.getDeclaredField(fieldName);

                    field.setAccessible(true);
                    Object v = field.get(item);
                    if (v != null) {
                        String condition = convert(v);
                        return "`" + columnName + "` = '" + condition + "' ";
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        ).filter(Objects::nonNull)
            .reduce((l, r) -> l + " AND " + r);

        return whereName.map(s -> " WHERE " + s).orElse("");
    }

    /**
     * 构造查询数量语句
     *
     * @param item
     * @return
     */
    public String buildSelectCountSql(T item) {
        return "SELECT COUNT(*) AS count FROM `" + tableName + "`" + buildWhereCondition(item) + ";";
    }

    /**
     * 构造新增语句
     *
     * @param item             实体
     * @param includeNullField 是否包括NULL字段
     * @return
     */
    public String buildInsertSql(T item, boolean includeNullField) {

        StringBuilder columnNames = new StringBuilder();
        StringBuilder values = new StringBuilder();

        columns.forEach(
            (fieldName, columnName) -> {
                try {
                    Field field = entityClass.getDeclaredField(fieldName);

                    field.setAccessible(true);
                    Object value = field.get(item);
                    if (value != null) {
                        columnNames.append(",`").append(columnName).append("`");
                        values.append(",'").append(convert(value)).append("'");

                    } else if (field.getAnnotation(GeneratedValue.class) != null) {
                        columnNames.append(",`").append(columnName).append("`");
                        values.append(", 0");
                    } else if (includeNullField) {
                        columnNames.append(",`").append(columnName).append("`");
                        values.append(", NULL");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        );

        columnNames.deleteCharAt(0);
        values.deleteCharAt(0);

        return "INSERT INTO " + tableName + " " + "(" + columnNames + ") VALUES (" + values + ");";
    }

    /**
     * 构造更新语句
     *
     * @param item
     * @return
     */
    public String buildUpdateSql(T item, boolean includeNullField) {

        if (pkField == null) {
            throw new RuntimeException("update without pk value  is forbidden!");
        }

        pkField.setAccessible(true);
        Object o = null;
        try {
            o = pkField.get(item);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        if (o == null) {
            throw new RuntimeException("update without pk value  is forbidden!");
        }

        Optional<String> updateValue = columns.entrySet().stream().map(
            entry -> {
                try {

                    String fieldName = entry.getKey();
                    String columnName = entry.getValue();

                    Field field = entityClass.getDeclaredField(fieldName);

                    field.setAccessible(true);
                    Object v = field.get(item);
                    if (v != null) {
                        String condition = convert(v);
                        return "`" + columnName + "` = '" + condition + "' ";
                    } else if (includeNullField) {
                        return "`" + columnName + "` = NULL ";
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        ).filter(Objects::nonNull)
            .reduce((l, r) -> l + " , " + r);

        if (!updateValue.isPresent()) {
            throw new RuntimeException("nothing to update!");
        }

        return "UPDATE " + tableName + " SET " + updateValue.get() + " WHERE `" + pkName + "` = '" + convert(o) + "';";
    }

    /**
     * 构造删除语句
     *
     * @param item
     * @return
     */
    public String buildDeleteSql(T item) {

        if (pkField == null) {
            throw new RuntimeException("delete without pk value  is forbidden!");
        }

        pkField.setAccessible(true);
        Object o = null;
        try {
            o = pkField.get(item);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        if (o == null) {
            throw new RuntimeException("delete without pk value is forbidden!");
        }

        String deleteCondition = columns.entrySet().stream().map(
            entry -> {
                try {

                    String fieldName = entry.getKey();
                    String columnName = entry.getValue();

                    Field field = entityClass.getDeclaredField(fieldName);

                    field.setAccessible(true);
                    Object v = field.get(item);
                    if (v != null) {
                        String condition = convert(v);
                        return "`" + columnName + "` = '" + condition + "' ";
                    } else {
                        return "`" + columnName + "` IS NULL ";

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        ).filter(Objects::nonNull)
            .reduce((l, r) -> l + " AND " + r)
            .orElse("`" + pkName + "` = '" + convert(o) + "'");

        return "DELETE FROM  " + tableName + " WHERE " + deleteCondition + ";";
    }

    /**
     * 构造分页语句
     *
     * @param item     实体
     * @param startRow 开始行数
     * @param size     分页大小
     * @param orderBy  排序条件
     * @return
     */
    public String buildPageSql(T item, Integer startRow, Integer size, String orderBy) {
        return selectAllSql + buildWhereCondition(item) + (orderBy != null ? " ORDER BY " + orderBy : "") + " LIMIT " + startRow + ", " + size + ";";
    }

    /**
     * 对象转字符串
     *
     * @param source 对象
     * @return
     */
    private String convert(Object source) {
        if (source instanceof String) {
            return (String) source;
        }

        if (source instanceof Number) {
            return source.toString();
        }

        if (source instanceof Date) {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(source);
        }

        logger.warn("unchecked convert for {}", source.toString());

        return source.toString();

    }
}
