package com.vn.backend.utils;


import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class BeanUtils implements ApplicationContextAware {

    private static ApplicationContext context;

    public static <T> T getBean(Class<T> beanClass) {
        return context.getBean(beanClass);
    }

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) {
        context = applicationContext; // NOSONAR: Context field is only set one
    }
}