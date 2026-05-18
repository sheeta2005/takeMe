package com.me.vo;

import lombok.Data;

@Data
public class ResultVO<T> {
    private Integer code;
    private String msg;
    private T data;

    public static <T> ResultVO<T> success(T data) {
        ResultVO<T> result = new ResultVO<>();
        result.setCode(200);
        result.setMsg("操作成功");
        result.setData(data);
        return result;
    }

    public static <T> ResultVO<T> success() {
        return success(null);
    }

    public static <T> ResultVO<T> error(String msg) {
        ResultVO<T> result = new ResultVO<>();
        result.setCode(500);
        result.setMsg(msg);
        return result;
    }
}