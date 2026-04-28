package com.serveradmin.websocket;

import jakarta.websocket.server.ServerEndpointConfig;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class SpringWebSocketConfigurator extends ServerEndpointConfig.Configurator {

    private static volatile BeanFactory beanFactory;

    public SpringWebSocketConfigurator() {
    }

    public static void setBeanFactory(BeanFactory factory) {
        beanFactory = factory;
    }

    @Override
    public <T> T getEndpointInstance(Class<T> clazz) throws InstantiationException {
        if (beanFactory == null) {
            beanFactory = SpringContextHolder.getBean(ApplicationContext.class).getAutowireCapableBeanFactory();
        }
        return beanFactory.getBean(clazz);
    }
}
