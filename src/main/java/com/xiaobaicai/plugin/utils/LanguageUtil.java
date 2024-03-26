/*
 * Copyright (c) 2022-2032 xiaobaicai.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information
 * of xiaobaicai. ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered
 * into with xiaobaicai
 */
package com.xiaobaicai.plugin.utils;

import com.xiaobaicai.plugin.constants.Constant;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * @author caijy
 * @description TODO
 * @date 2023/3/19 星期日 8:44 上午
 */
public class LanguageUtil {

    public static String getLocalMessages(String key) {
        Locale locale = Locale.getDefault();
        if (locale == null) {
            locale = Locale.ENGLISH;
        }
        if (!Locale.CHINESE.getLanguage().equals(locale.getLanguage())) {
            locale = Locale.ENGLISH;
        }
        return ResourceBundle.getBundle(Constant.PLUGIN_RESOURCE, Locale.forLanguageTag(locale.getLanguage())).getString(key);
    }
}
