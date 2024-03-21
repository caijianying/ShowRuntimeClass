package com.xiaobaicai.plugin.core.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * @author caijy
 * @description
 * @date 2024/3/8 星期五 3:57 下午
 */
@Data
public class RemoteNotifyDTO implements Serializable {
    private static final long serialVersionUID = -7263234457871576607L;

    private String className;

    private String classFilePath;
}
