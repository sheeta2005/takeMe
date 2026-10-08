package com.me.exception;

/**
 * 密码修改失败异常
 */
public class PasswordEditFailedException extends BaseException{

    //创建密码修改失败异常
    public PasswordEditFailedException(String msg){
        super(msg);
    }

}
