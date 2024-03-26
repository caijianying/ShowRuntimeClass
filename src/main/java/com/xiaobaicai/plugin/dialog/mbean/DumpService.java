package com.xiaobaicai.plugin.dialog.mbean;


public class DumpService implements DumpServiceMBean {

    private String clazz;

    public DumpService() {

    }
    @Override
    public String setClazz(String clazz) {
        this.clazz = clazz;

        try {
            System.out.println("setClazz："+clazz);
        } catch (Exception e) {
            return null;
        }
        return "OK";
    }
}
