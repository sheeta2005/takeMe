package com.me.exception;

/**
 * 登录失败
 */
public class LoginFailedException extends BaseException{
    //创建登录失败异常
    public LoginFailedException(String msg){
        super(msg);
    }
}
