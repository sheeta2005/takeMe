package com.me.exception;

/**
 * 密码错误异常
 */
public class PasswordErrorException extends BaseException {

    //创建密码错误异常
    public PasswordErrorException() {
    }

    //创建密码错误异常
    public PasswordErrorException(String msg) {
        super(msg);
    }

}
