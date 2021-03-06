package com.mekki.vertx.dao;

import com.mekki.vertx.dao.support.PageSupport;
import io.vertx.core.Handler;

/**
 * Created by Mekki on 2018/3/23.
 * 支持分页的DAO层接口
 */
public interface PageDao {
    <E> void select(E e, PageSupport<E> ps, Handler<PageSupport<E>> handler);
}
