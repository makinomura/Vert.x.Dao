package com.mekki.vertx.dao;

import io.vertx.core.Handler;

/**
 * Created by Mekki on 2018/3/23.
 * 基础操作增强 DAO层接口
 */
public interface EnhancedDao {

    <E> void selectOne(E e, Handler<E> handler);

    <E> void selectCount(E e, Handler<Long> handler);

    <E> void insertSelective(E e, Handler<Integer> handler);

    <E> void updateSelective(E e, Handler<Integer> handler);

}
