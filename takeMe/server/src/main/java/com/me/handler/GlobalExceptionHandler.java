package com.me.handler;

import com.me.exception.*;
import com.me.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.HashMap;
import java.util.Map;

/**
 * 全局统一异常处理器
 * 分层捕获逻辑：
 * 1. 业务自定义异常（BaseException 及其子类）
 * 2. 参数校验异常
 * 3. SQL 约束异常
 * 4. 权限异常
 * 5. 文件上传异常
 * 6. 其他未知异常
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ======================== 1. 业务自定义异常 ========================

    /**
     * 捕获登录失败异常
     */
    @ExceptionHandler(LoginFailedException.class)
    public Result<Void> handleLoginFailedException(LoginFailedException ex) {
        log.warn("【登录失败】{}", ex.getMessage());
        return Result.error(ex.getMessage());
    }

    /**
     * 捕获用户未登录异常
     */
    @ExceptionHandler(UserNotLoginException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<Void> handleUserNotLoginException(UserNotLoginException ex) {
        log.warn("【未登录】{}", ex.getMessage());
        return Result.error(401, ex.getMessage() != null ? ex.getMessage() : "请先登录");
    }

    /**
     * 捕获账号不存在异常
     */
    @ExceptionHandler(AccountNotFoundException.class)
    public Result<Void> handleAccountNotFoundException(AccountNotFoundException ex) {
        log.warn("【账号不存在】{}", ex.getMessage());
        return Result.error(ex.getMessage());
    }

    /**
     * 捕获账号被锁定异常
     */
    @ExceptionHandler(AccountLockedException.class)
    public Result<Void> handleAccountLockedException(AccountLockedException ex) {
        log.warn("【账号锁定】{}", ex.getMessage());
        return Result.error(ex.getMessage());
    }

    /**
     * 捕获密码错误异常
     */
    @ExceptionHandler(PasswordErrorException.class)
    public Result<Void> handlePasswordErrorException(PasswordErrorException ex) {
        log.warn("【密码错误】{}", ex.getMessage());
        return Result.error(ex.getMessage());
    }

    /**
     * 捕获密码修改失败异常
     */
    @ExceptionHandler(PasswordEditFailedException.class)
    public Result<Void> handlePasswordEditFailedException(PasswordEditFailedException ex) {
        log.warn("【密码修改失败】{}", ex.getMessage());
        return Result.error(ex.getMessage());
    }

    /**
     * 捕获订单业务异常
     */
    @ExceptionHandler(OrderBusinessException.class)
    public Result<Void> handleOrderBusinessException(OrderBusinessException ex) {
        log.warn("【订单异常】{}", ex.getMessage());
        return Result.error(ex.getMessage());
    }

    /**
     * 捕获购物车业务异常
     */
    @ExceptionHandler(ShoppingCartBusinessException.class)
    public Result<Void> handleShoppingCartBusinessException(ShoppingCartBusinessException ex) {
        log.warn("【购物车异常】{}", ex.getMessage());
        return Result.error(ex.getMessage());
    }

    /**
     * 捕获地址簿业务异常
     */
    @ExceptionHandler(AddressBookBusinessException.class)
    public Result<Void> handleAddressBookBusinessException(AddressBookBusinessException ex) {
        log.warn("【地址簿异常】{}", ex.getMessage());
        return Result.error(ex.getMessage());
    }

    /**
     * 捕获删除不允许异常
     */
    @ExceptionHandler(DeletionNotAllowedException.class)
    public Result<Void> handleDeletionNotAllowedException(DeletionNotAllowedException ex) {
        log.warn("【删除失败】{}", ex.getMessage());
        return Result.error(ex.getMessage());
    }

    /**
     * 捕获其他业务异常（BaseException 兜底）
     */
    @ExceptionHandler(BaseException.class)
    public Result<Void> handleBaseException(BaseException ex) {
        log.error("【业务异常】{}", ex.getMessage(), ex);
        return Result.error(ex.getMessage());
    }

    /**
     * 捕获业务运行时异常（如ServiceTimeValidator抛出的异常）
     */
    @ExceptionHandler(RuntimeException.class)
    public Result<Void> handleRuntimeException(RuntimeException ex) {
        log.warn("【业务校验失败】{}", ex.getMessage());
        return Result.error(ex.getMessage());
    }

    // ======================== 2. 参数校验异常 ========================

    /**
     * 捕获 @Valid 注解的参数校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Map<String, String>> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            if (error instanceof FieldError) {
                FieldError fieldError = (FieldError) error;
                errors.put(fieldError.getField(), fieldError.getDefaultMessage());
            }
        });

        log.warn("【参数校验失败】{}", errors);
        return Result.error(400, "参数校验失败");
    }

    /**
     * 捕获 BindException 异常
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Map<String, String>> handleBindException(BindException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            if (error instanceof FieldError) {
                FieldError fieldError = (FieldError) error;
                errors.put(fieldError.getField(), fieldError.getDefaultMessage());
            }
        });

        log.warn("【参数绑定失败】{}", errors);
        return Result.error(400, "参数绑定失败");
    }

    // ======================== 3. SQL 约束异常 ========================

    /**
     * 捕获 SQL 唯一约束异常（如用户名重复）
     */
    @ExceptionHandler(SQLIntegrityConstraintViolationException.class)
    public Result<Void> handleSQLIntegrityConstraintViolationException(SQLIntegrityConstraintViolationException ex) {
        String message = ex.getMessage();
        log.warn("【SQL约束异常】{}", message);

        if (message != null && message.contains("Duplicate entry")) {
            String[] split = message.split(" ");
            if (split.length >= 3) {
                String duplicateValue = split[2].replace("'", "");
                return Result.error(duplicateValue + " 已存在");
            }
        }

        return Result.error("数据冲突，请检查输入");
    }

    // ======================== 4. 权限异常 ========================

    /**
     * 捕获 Spring Security 访问拒绝异常
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> handleAccessDeniedException(AccessDeniedException ex) {
        log.warn("【权限不足】{}", ex.getMessage());
        return Result.error(403, "无权访问该资源");
    }

    // ======================== 5. 文件上传异常 ========================

    /**
     * 捕获文件上传大小超限异常
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public Result<Void> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex) {
        log.warn("【文件过大】{}", ex.getMessage());
        return Result.error(413, "文件大小超过限制（最大 5MB）");
    }

    // ======================== 6. 其他未知异常 ========================

    /**
     * 捕获所有未处理的异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception ex) {
        log.error("【系统异常】", ex);
        return Result.error("系统繁忙，请稍后重试");
    }
}
