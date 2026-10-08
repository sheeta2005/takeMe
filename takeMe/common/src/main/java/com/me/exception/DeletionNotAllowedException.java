package com.me.exception;

public class DeletionNotAllowedException extends BaseException {

    //创建禁止删除异常
    public DeletionNotAllowedException(String msg) {
        super(msg);
    }

}
