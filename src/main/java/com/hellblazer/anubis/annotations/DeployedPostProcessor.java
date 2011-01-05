package com.hellblazer.anubis.annotations;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.MethodCallback;

public class DeployedPostProcessor implements BeanPostProcessor, Ordered {

    @Override
    public int getOrder() {
        return LOWEST_PRECEDENCE;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName)
                                                                              throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessBeforeInitialization(final Object bean,
                                                  String beanName)
                                                                  throws BeansException {
        Class<?> targetClass = AopUtils.getTargetClass(bean);
        ReflectionUtils.doWithMethods(targetClass, new MethodCallback() {
            @Override
            public void doWith(Method method) throws IllegalArgumentException,
                                             IllegalAccessException {
                Deployed annotation = AnnotationUtils.getAnnotation(method,
                                                                    Deployed.class);
                if (annotation != null) {
                    Assert.isTrue(void.class.equals(method.getReturnType()),
                                  "Only void-returning methods may be annotated with @Deployed.");
                    Assert.isTrue(method.getParameterTypes().length == 0,
                                  "Only no-arg methods may be annotated with @Deployed.");
                    try {
                        method.invoke(bean);
                    } catch (InvocationTargetException e) {
                        throw new IllegalStateException(
                                                        "failed to deploy bean",
                                                        e.getTargetException());
                    }
                }
            }
        });
        return bean;
    }

}
