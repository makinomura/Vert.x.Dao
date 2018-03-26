package com.mekki.vertx.dao.support.exception;

/**
 * Created by Mekki on 2018/3/26.
 * 未处理的异常
 */
public class UnhandledException extends RuntimeException {
    public UnhandledException(Throwable cause) {
        super(cause);
    }
}
