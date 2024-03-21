package com.xiaobaicai.plugin.app;

import com.xiaobaicai.plugin.core.service.RemoteService;
import com.xiaobaicai.plugin.core.utils.RemoteUtil;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.concurrent.TimeUnit;

/**
 * @author caijy
 * @description
 * @date 2024/3/8 星期五 3:24 下午
 */
public class Test {

    @org.junit.jupiter.api.Test
    public void testRmi(){
        RemoteService service = RemoteUtil.getRemoteService(1099);
    }

    @org.junit.jupiter.api.Test
    public void testServer(){

    }

}
