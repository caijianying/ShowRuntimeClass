package com.xiaobaicai.plugin.agent.service;

import com.xiaobaicai.plugin.core.service.RemoteService;

import java.lang.instrument.Instrumentation;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author caijy
 * @description
 * @date 2024/3/8 星期五 2:30 下午
 */
public class RemoteAppService extends UnicastRemoteObject implements RemoteService {

    private Set<Class> availableClasses;

    private Map<String, String> availableClassesMap = new HashMap<>();

    private Instrumentation inst;

    public RemoteAppService() throws RemoteException {
        super();
    }


    @Override
    public String getAllAvailableClasses() {
        Set<String> list = availableClasses.stream().map(c -> c.getName()).collect(Collectors.toSet());
        return String.join(",", list);
    }

    @Override
    public void setAllAvailableClasses(Set<Class> classes) {
        availableClasses = classes;
    }

    @Override
    public void setClassPath(String className, String filePath) {
        className = className.replace("/", ".");
        availableClassesMap.put(className, filePath);
    }

    @Override
    public String retransFormClass(String targetClass) {
        Class matchedClass = availableClasses.stream().filter(c -> c.getName().replace("/", ".").equals(targetClass)).findFirst().orElse(null);
        if (matchedClass != null) {
            try {
                inst.retransformClasses(matchedClass);
            } catch (Throwable ex) {
                ex.printStackTrace();
            }
        }
        return availableClassesMap.get(targetClass);
    }

    @Override
    public void setInstrument(Instrumentation instrument) {
        this.inst = instrument;
    }


}
