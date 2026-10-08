package com.me.exception;

/**
 * 业务异常
 */
public class BaseException extends RuntimeException {

    //创建基础业务异常
    public BaseException() {
    }

    //创建基础业务异常
    public BaseException(String msg) {
        super(msg);
    }

}
