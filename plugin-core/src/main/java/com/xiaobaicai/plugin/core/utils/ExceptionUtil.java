package com.xiaobaicai.plugin.core.utils;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * @author caijy
 * @description
 * @date 2024/4/2 星期二 10:19
 */
public class ExceptionUtil {

    public static String exceptionToString(Throwable throwable) {
        // 使用StringWriter捕获异常信息
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        throwable.printStackTrace(printWriter);
        printWriter.flush();

        // 返回捕获的异常信息字符串
        return stringWriter.toString();
    }
}
