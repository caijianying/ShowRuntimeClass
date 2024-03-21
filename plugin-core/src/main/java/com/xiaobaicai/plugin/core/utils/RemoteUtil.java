package com.xiaobaicai.plugin.core.utils;

import com.xiaobaicai.plugin.core.service.RemoteService;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Set;

/**
 * @author caijy
 * @description
 * @date 2024/3/19 星期二 8:20 下午
 */
public class RemoteUtil {

    public static RemoteService getRemoteService(Integer port) {
        RemoteService remoteAppService = null;
        try {
            Registry registry = LocateRegistry.getRegistry("127.0.0.1", port);
            remoteAppService = (RemoteService) registry.lookup("RemoteService");
            return remoteAppService;
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (NotBoundException e) {
            e.printStackTrace();
        }
        return null;
    }
}
