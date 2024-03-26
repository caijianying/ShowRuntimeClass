package com.xiaobaicai.plugin.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Set;

/**
 * @author caijy
 * @description
 * @date 2024/3/5 星期二 3:54 下午
 */
@Data
public class MatchedVmReturnModel implements Serializable {

    private static final long serialVersionUID = 2849171878560728981L;

    private Set<String> classes;

    private Integer port;
}