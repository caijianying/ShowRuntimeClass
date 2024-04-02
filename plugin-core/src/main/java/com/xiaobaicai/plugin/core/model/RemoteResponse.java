package com.xiaobaicai.plugin.core.model;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author caijy
 * @description
 * @date 2024/4/2 星期二 10:28
 */
@AllArgsConstructor
@Data
public class RemoteResponse<T> {

    private T data;

    private String message;

    private Integer code;

    public static <T> RemoteResponse<T> Ok(T data) {
        return new RemoteResponse<>(data, "", 0);
    }

    public static RemoteResponse fail(String message) {
        return new RemoteResponse<>(null, message, -1);
    }

    public Boolean isSuccess() {
        return 0 == this.getCode();
    }
}
