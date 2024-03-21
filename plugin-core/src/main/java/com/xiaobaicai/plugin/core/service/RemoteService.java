package com.xiaobaicai.plugin.core.service;

import java.lang.instrument.Instrumentation;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Set;

/**
 * @author caijy
 * @description
 * @date 2024/3/8 星期五 2:29 下午
 */
public interface RemoteService extends Remote {

    String getAllAvailableClasses() throws RemoteException;

    void setAllAvailableClasses(Set<Class> classes) throws RemoteException;

    void setClassPath(String className, String filePath) throws RemoteException;

    /**
     * @return path
     **/
    String retransFormClass(String targetClass) throws RemoteException;

    void setInstrument(Instrumentation instrument) throws RemoteException;
}
