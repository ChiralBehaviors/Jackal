/** (C) Copyright 2010 Hal Hildebrand, all rights reserved.

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
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
