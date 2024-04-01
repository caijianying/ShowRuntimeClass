package com.xiaobaicai.plugin.model;

import lombok.Data;

import java.io.Serializable;

/**
 * @author caijy
 * @description
 * @date 2024/3/5 星期二 8:12 下午
 */
@Data
public class ClassInfoModel implements Serializable {
    private static final long serialVersionUID = 7782111242518168766L;

    private String className;

    private String moduleName;
}
