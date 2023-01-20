package org.slj.mqtt.sn.utils;

import org.slj.mqtt.sn.spi.IMqttsnService;
import org.slj.mqtt.sn.spi.MqttsnService;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ServiceUtils {

    public static Class<? extends IMqttsnService> getServiceBind(IMqttsnService instance){

        Class<? extends IMqttsnService> bindCls = null;
        //try and bind against the annotated interface
        List<Class<?>> list = new ArrayList<>();
        Class<?> searchClass = instance.getClass();
        while (searchClass != null) {
            Class<?>[] a = searchClass.getInterfaces();
            if(a != null && a.length > 0){
                list.addAll(Arrays.asList(a));
            }
            // recurse point
            searchClass = searchClass.getSuperclass();
        }
        Class[] clss = list.toArray(new Class[list.size()]);
        for (Class c : clss){
            Annotation[] anno = c.getAnnotations();
            if (anno.length == 0) {
                continue;
            }
            for (Annotation a : anno){
                if(a.annotationType() == MqttsnService.class){
                    bindCls = c;
                }
            }
        }

        if(bindCls == null){
            bindCls = instance.getClass();
        }
        return bindCls;
    }

    public static MqttsnService getAnnotationFromBind(IMqttsnService instance){
        Class c = getServiceBind(instance);
        return (MqttsnService) c.getAnnotation(MqttsnService.class);
    }
}
