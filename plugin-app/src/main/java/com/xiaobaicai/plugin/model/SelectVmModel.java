package com.xiaobaicai.plugin.model;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * @author caijy
 * @description
 * @date 2024/3/5 星期二 7:54 下午
 */
@Data
public class SelectVmModel {

    private List<String> showNames;

    private Map<String,String> showNamePidMap;
}
