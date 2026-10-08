package com.me.exception;

/**
 * 账号被锁定异常
 */
public class AccountLockedException extends BaseException {

    //创建账号停用异常
    public AccountLockedException() {
    }

    //创建账号停用异常
    public AccountLockedException(String msg) {
        super(msg);
    }

}
