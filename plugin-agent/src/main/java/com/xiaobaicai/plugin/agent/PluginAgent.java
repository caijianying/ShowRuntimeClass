package com.xiaobaicai.plugin.agent;

import cn.hutool.json.JSONUtil;
import com.xiaobaicai.plugin.core.boot.AgentPkgPath;
import com.xiaobaicai.plugin.core.dto.AttachVmInfoDTO;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarFile;

/**
 * @author caijy
 * @description
 * @date 2024/3/6 星期三 3:04 下午
 */
public class PluginAgent {

    public static void premain(String agentArgs, Instrumentation inst) {

    }

    public static void agentmain(String agentArgs, Instrumentation inst) {
        System.out.println("PluginAgent....agentmain");

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
                    e.printStackTrace();
                }
            }
        }

        AttachVmInfoDTO vmInfoDTO = null;
        try {
            vmInfoDTO = JSONUtil.toBean(agentArgs, AttachVmInfoDTO.class);
            DumpClassFileTransformer transformer = new DumpClassFileTransformer(vmInfoDTO.getPid());
            inst.addTransformer(transformer, true);

        } catch (Throwable ex) {
            ex.printStackTrace();
        }
        Map<String, ClassLoader> classLoaderMap = new HashMap<>();
        for (Class loadedClass : inst.getAllLoadedClasses()) {
            classLoaderMap.put(loadedClass.getName(), loadedClass.getClassLoader());
        }

        if (vmInfoDTO == null) {
            return;
        }

        classLoaderMap.keySet().stream().forEach(System.out::println);

        try {
            String className = vmInfoDTO.getTargetClassName();
            ClassLoader loader = classLoaderMap.get(className);
            if (loader != null) {
                Class<?> loadClass = loader.loadClass(className);
                inst.retransformClasses(loadClass);
            }
        } catch (UnmodifiableClassException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static class DumpClassFileTransformer implements ClassFileTransformer {

        private String pid;

        public String getPid() {
            return pid;
        }

        public DumpClassFileTransformer(String pid) {
            this.pid = pid;
        }

        @Override
        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
            if (classfileBuffer != null && className != null) {
                String dir = "/Users/jianyingcai/IdeaProjects/practice/Service-Invocation-Monitor-Demo/lib/dump";
                String path = dir + File.separator + getPid() + File.separator + className + ".class";
                System.out.println(className + "=>" + path);
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
                    System.out.println("transform class " + className + " success!");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return classfileBuffer;
        }
    }
}
