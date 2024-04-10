package com.xiaobaicai.plugin.core.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * @author caijy
 * @description
 * @date 2024/3/6 星期三 2:51 下午
 */
@Data
public class AttachVmInfoDTO implements Serializable {
    private static final long serialVersionUID = -8880806212939427112L;

    private Integer port;

    private String pid;

}
