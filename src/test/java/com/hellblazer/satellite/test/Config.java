package com.hellblazer.satellite.test;

import java.io.IOException;

import org.smartfrog.services.anubis.BasicConfiguration;
import org.smartfrog.services.anubis.locator.subprocess.SPLocatorAdapterImpl;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hellblazer.anubis.satellite.SPLocatorAdapterExporter;

@Configuration
public class Config extends BasicConfiguration {

    @Override
    public int getMagic() {
        try {
            return Identity.getMagicFromLocalIpAddress();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public int heartbeatGroupTTL() {
        return 0;
    }

    @Override
    public int node() {
        return 0;
    }

    @Bean
    SPLocatorAdapterImpl adapter() {
        SPLocatorAdapterImpl adapter = new SPLocatorAdapterImpl();
        adapter.setHeartbeatInterval(heartbeatInterval());
        adapter.setHeartbeatTimeout(heartbeatTimeout());
        adapter.setLocator(locator());
        return adapter;
    }

    @Bean
    public SPLocatorAdapterExporter locatorAdapterExporter() {
        SPLocatorAdapterExporter exporter = new SPLocatorAdapterExporter();
        exporter.setAdapter(adapter());
        return exporter;
    }
}
