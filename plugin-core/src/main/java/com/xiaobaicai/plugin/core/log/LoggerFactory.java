package com.xiaobaicai.plugin.core.log;

/**
 * @author caijy
 * @description
 * @date 2024/10/17 星期四 09:30
 */
public interface LoggerFactory {
    static Logger getLogger(Class<?> clazz) {
        return new DefaultLogger(clazz);
    }
}
