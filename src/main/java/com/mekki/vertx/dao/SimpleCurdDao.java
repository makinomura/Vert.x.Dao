package com.mekki.vertx.dao;

import io.vertx.core.Handler;

import java.util.List;

/**
 * Created by Mekki on 2018/3/21.
 * DAO接口
 */
public interface SimpleCurdDao {
    <E> void select(E e, Handler<List<E>> handler);

    <E> void insert(E e, Handler<Integer> handler);

    <E> void update(E e, Handler<Integer> handler);

    <E> void delete(E e, Handler<Integer> handler);
}
