package com.xiaobaicai.plugin.model;

import lombok.Data;

import java.io.Serializable;

/**
 * @author caijy
 * @description
 * @date 2024/3/5 星期二 3:54 下午
 */
@Data
public class MatchedVmModel implements Serializable {

    private static final long serialVersionUID = 2849171878560728981L;

    private String moduleName;

    private String mainClass;

    private String pid;

    /**
     * 1= running,0=not
     **/
    private Integer running;
}