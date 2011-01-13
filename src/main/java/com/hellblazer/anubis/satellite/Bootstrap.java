package com.hellblazer.anubis.satellite;

import java.rmi.Naming;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class Bootstrap {
    public static void main(String[] argv) throws Exception {
        if (argv.length != 2) {
            System.err.println("Satellite Launch usage: <configuration package> <handshake name>");
            System.exit(1);
        }

        System.out.println("Satellite Launch Configuration Package: " + argv[0]);
        System.out.println("Satellite Launch Handshake Name: " + argv[1]);
        Handshake handshake = (Handshake) Naming.lookup("//localhost:1099/"
                                                        + argv[1]);

        ApplicationContext context = new AnnotationConfigApplicationContext(
                                                                            argv[0]);
        handshake.setAdapter(context.getBean(SPLocatorAdapterExporter.class).getAdapter());
    }
}
