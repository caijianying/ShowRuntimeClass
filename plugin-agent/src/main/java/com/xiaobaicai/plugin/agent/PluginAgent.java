package com.xiaobaicai.plugin.agent;

import cn.hutool.json.JSONUtil;
import com.xiaobaicai.plugin.agent.service.RemoteAppService;
import com.xiaobaicai.plugin.agent.utils.RuntimeMXBeanUtil;
import com.xiaobaicai.plugin.core.boot.AgentPkgNotFoundException;
import com.xiaobaicai.plugin.core.boot.AgentPkgPath;
import com.xiaobaicai.plugin.core.dto.AttachVmInfoDTO;
import com.xiaobaicai.plugin.core.log.Logger;
import com.xiaobaicai.plugin.core.log.LoggerFactory;
import com.xiaobaicai.plugin.core.service.RemoteService;
import lombok.SneakyThrows;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

/**
 * @author caijy
 * @description
 * @date 2024/3/6 星期三 3:04 下午
 */
public class PluginAgent {

    private static final Logger logger = LoggerFactory.getLogger(PluginAgent.class);

    public static void premain(String agentArgs, Instrumentation inst) {

    }

    public static void agentmain(String agentArgs, Instrumentation inst) {
        logger.info("Plugin agent attached.");
        AttachVmInfoDTO infoDTO = JSONUtil.toBean(agentArgs, AttachVmInfoDTO.class);
        Integer port = infoDTO.getPort();
        Class<?>[] canBeRetransformedClasses = Arrays.stream(inst.getAllLoadedClasses()).filter(c -> inst.isModifiableClass(c) && !c.isArray()).toArray(Class[]::new);
        Set<Class> classSet = Arrays.stream(canBeRetransformedClasses).collect(Collectors.toSet());
        RemoteService remoteService = startServer(port, classSet, inst);

        logger.info("Pid is " + RuntimeMXBeanUtil.getPid());
        DumpClassFileTransformer transformer = new DumpClassFileTransformer(RuntimeMXBeanUtil.getPid() + "", remoteService);
        inst.addTransformer(transformer, true);

        // 添加到启动类加载器，为了让所有classloader能加载到agent包中的类
        String clazzName = PluginAgent.class.getName().replace(".", "/") + ".class";
        URL resource = ClassLoader.getSystemClassLoader().getResource(clazzName);
        if (resource.getProtocol().equals("jar")) {
            int index = resource.getPath().indexOf("!/");
            if (index > -1) {
                String jarFile = resource.getPath().substring("file:".length(), index);
                try {
                    inst.appendToBootstrapClassLoaderSearch(new JarFile(new File(jarFile)));
                } catch (IOException e) {

                }
            }
        }
    }

    public static class DumpClassFileTransformer implements ClassFileTransformer {

        private String pid;
        private RemoteService remoteService;

        public String getPid() {
            return pid;
        }

        public DumpClassFileTransformer(String pid, RemoteService remoteService) {
            this.pid = pid;
            this.remoteService = remoteService;
        }

        @SneakyThrows
        @Override
        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
            if (classfileBuffer != null && className != null) {
                String path;
                try {
                    String dir = AgentPkgPath.getPath().getParent() + "/dump";
                    path = dir + File.separator + getPid() + File.separator + className + ".class";
                } catch (AgentPkgNotFoundException e) {
                    return classfileBuffer;
                }

                File tmpFile = new File(path);
                try {
                    if (!tmpFile.exists()) {
                        File parent = tmpFile.getParentFile();
                        if (!parent.exists()) {
                            parent.mkdirs();
                        }
                        tmpFile.createNewFile();
                    }
                    // 写入文件
                    FileOutputStream fileOutputStream = new FileOutputStream(tmpFile);
                    fileOutputStream.write(classfileBuffer);
                    fileOutputStream.flush();

                    // 调用远程方法
                    if (this.remoteService != null) {
                        this.remoteService.setClassPath(className, path);
                    }
                } catch (IOException ignored) {

                }
            }
            return classfileBuffer;
        }
    }

    private static RemoteService startServer(Integer port, Set<Class> classes, Instrumentation inst) {

        Registry registry = null;
        RemoteService remoteService = null;
        try {
            remoteService = new RemoteAppService();
            remoteService.setInstrument(inst);
            // 导出远程对象并绑定到 RMI Registry
            registry = LocateRegistry.createRegistry(port);
            registry.rebind("RemoteService", remoteService);
            logger.info("RMI Server is running.");
        } catch (Exception e) {
            logger.error("RMI Server is running error.");
        }

        if (remoteService == null) {
            return null;
        }

        if (registry != null) {
            try {
                remoteService.setAllAvailableClasses(classes);
            } catch (RemoteException ignored) {

            }
        }
        return remoteService;
    }
}
