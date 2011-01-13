package com.hellblazer.anubis.satellite;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.hellblazer.anubis.util.Base64Coder;

public class Bootstrap {
    public static void main(String[] argv) throws Exception {

        if (argv.length != 1) {
            System.err.println("usage <configurationPackage>");
            System.exit(1);
        }
        // Decode the reference to the launch handshake
        ByteArrayInputStream bais = new ByteArrayInputStream(
                                                             Base64Coder.decodeLines(argv[1]));
        ObjectInputStream ois = new ObjectInputStream(bais);
        Handshake handshake = (Handshake) ois.readObject();

        ApplicationContext context = new AnnotationConfigApplicationContext(
                                                                            argv[0]);
        handshake.setAdapter(context.getBean(SPLocatorAdapterExporter.class).getAdapter());
    }
}
