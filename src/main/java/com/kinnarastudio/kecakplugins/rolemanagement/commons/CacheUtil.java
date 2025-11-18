package com.kinnarastudio.kecakplugins.rolemanagement.commons;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.StringUtil;
import org.joget.workflow.util.WorkflowUtil;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Optional;

public class CacheUtil {
    public static String getCacheKey(Class<?> caller, String... args) {
        return String.join("_", caller.getName(), getSessionId(), StringUtil.md5Base16(String.join("", args)));
    }

    public static String getSessionId() {
        HttpServletRequest request = WorkflowUtil.getHttpServletRequest();
        return Optional.ofNullable(request)
                .map(HttpServletRequest::getCookies)
                .stream()
                .flatMap(Arrays::stream)
                .filter(c -> "JSESSIONID".equals(c.getName()))
                .findFirst()
                .map(Cookie::getValue)
                .orElse("");
    }

    public static Object getCached(String key) {
        final Cache cache = (Cache) AppUtil.getApplicationContext().getBean("formOptionsCache");
        return Optional.ofNullable(cache)
                .map(c -> c.get(key))
                .map(Element::getObjectValue)
                .orElse(null);
    }

    public static <T> T putAndReturnCache(String key, T value) {
        final Cache cache = (Cache) AppUtil.getApplicationContext().getBean("formOptionsCache");
        if(cache != null && value != null) {
            cache.put(new Element(key, value));
        }
        return value;
    }
}
