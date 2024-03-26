package com.xiaobaicai.plugin.model;

import lombok.Data;

/**
 * @author caijy
 * @description
 * @date 2024/3/6 星期三 11:13 上午
 */
@Data
public class SelectCallBackModel {

    private String targetClassName;

    private MatchedVmModel vmModel;

}
