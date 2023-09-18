package org.slj.mqtt.tree.ui;

import javax.swing.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MqttViewerUtils {

    public static ImageIcon loadIcon(String iconName) throws IOException {

        byte[] a = loadResource(iconName);
        ImageIcon imageIcon = new ImageIcon(a);
        return imageIcon;
    }

    public static byte[] loadResource(String resourceName) throws IOException {
        if(resourceName == null) throw new IOException("cannot load <null> resource");
        if(resourceName.startsWith("/")) {
            resourceName = resourceName.substring(1);
        }
        InputStream is = MqttViewerUtils.class.getResourceAsStream(resourceName);
        if(is == null){
            is = MqttViewerUtils.class.getResourceAsStream("/" + resourceName);
        }

        if(is == null) throw new IOException("resource not found on class path " + resourceName);
        return read(is, 1024);
    }

    public static byte[] read(InputStream is, int bufSize) throws IOException {
        try(ByteArrayOutputStream baos
                    = new ByteArrayOutputStream()){
            byte[] buf = new byte[bufSize];
            int length;
            while ((length = is.read(buf)) != -1) {
                baos.write(buf, 0, length);
            }
            return baos.toByteArray();
        }
    }
}
