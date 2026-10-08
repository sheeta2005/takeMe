package com.me.exception;

public class UserNotLoginException extends BaseException {

    //创建用户未登录异常
    public UserNotLoginException() {
    }

    //创建用户未登录异常
    public UserNotLoginException(String msg) {
        super(msg);
    }

}
