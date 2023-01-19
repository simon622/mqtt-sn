package org.slj.mqtt.sn.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public abstract class AbstractOptions {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    public void processFromSystemPropertyOverrides(){

        try {
            Field[] f = getClass().getFields();
            for (int i =0;i<f.length;i++){
                if(Modifier.isStatic(f[i].getModifiers())){
                    continue;
                }
                String fieldName = f[i].getName();
                String value = System.getProperty(fieldName, null);
                if(value != null){
                    boolean modified = false;

                    Class<?> t = f[i].getType();
                    if(String.class.isAssignableFrom(t)){
                        f[i].set(this, value);
                        modified = true;
                    } else if(Boolean.class.isAssignableFrom(t) || boolean.class.isAssignableFrom(t)){
                        f[i].setBoolean(this, Boolean.parseBoolean(value));
                        modified = true;
                    }
                    else if(Integer.class.isAssignableFrom(t) || int.class.isAssignableFrom(t)){
                        f[i].setInt(this, Integer.parseInt(value));
                        modified = true;
                    }
                    else if(Long.class.isAssignableFrom(t) || long.class.isAssignableFrom(t)){
                        f[i].setLong(this, Long.parseLong(value));
                        modified = true;
                    }
                    else if(Short.class.isAssignableFrom(t) || short.class.isAssignableFrom(t)){
                        f[i].setShort(this, Short.parseShort(value));
                        modified = true;
                    }
                    else if(Double.class.isAssignableFrom(t) || double.class.isAssignableFrom(t)){
                        f[i].setDouble(this, Double.parseDouble(value));
                        modified = true;
                    }
                    else if(Float.class.isAssignableFrom(t) || float.class.isAssignableFrom(t)){
                        f[i].setFloat(this, Float.parseFloat(value));
                        modified = true;
                    }
                    else if(Character.class.isAssignableFrom(t) || char.class.isAssignableFrom(t)){
                        f[i].setChar(this, value.charAt(0));
                        modified = true;
                    }
                    if(modified){
                        logger.info("found overriding system property for {} -> {}", fieldName, value);
                    }
                }
            }
        } catch(Exception e){
            logger.error("error attempting to apply system property overrides", e);
        }
    }
}
