package com.hellblazer.anubis.satellite;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.io.PrintStream;
import java.rmi.Remote;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.hellblazer.anubis.util.Base64Coder;

public class Bootstrap {
    // take full control of System.out
    private static final PrintStream systemOut;
    static {
        systemOut = System.out;
        System.setOut(System.err);
    }

    public static void main(String[] argv) throws Exception {

        if (argv.length != 1) {
            System.err.println("usage <configurationPackage>");
            System.exit(1);
        }
        // Decode the reference to the client locator
        ByteArrayInputStream bais = new ByteArrayInputStream(
                                                             Base64Coder.decodeLines(argv[1]));
        ObjectInputStream ois = new ObjectInputStream(bais);
        @SuppressWarnings("unused")
        Remote clientLocatorRef = (Remote) ois.readObject();

        ApplicationContext context = new AnnotationConfigApplicationContext(
                                                                            argv[0]);
        String encodedLocatorRef = context.getBean(SPLocatorAdapterExporter.class).getRef();
        int length = encodedLocatorRef.length();
        System.out.println("%" + length + "%" + encodedLocatorRef);
        System.out.println();
        System.out.println();
        System.setOut(systemOut);
    }
}
