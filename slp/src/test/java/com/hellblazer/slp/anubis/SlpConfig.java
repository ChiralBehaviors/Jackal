package com.hellblazer.slp.anubis;

import java.util.Random;
import java.util.concurrent.Executors;

import org.smartfrog.services.anubis.partition.Partition;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;

import com.fasterxml.uuid.NoArgGenerator;
import com.fasterxml.uuid.impl.RandomBasedGenerator;
import com.hellblazer.slp.ServiceScope;

public class SlpConfig {

    @Bean
    @Autowired
    public ServiceScope anubisScope(Identity partitionIdentity,
                                    Partition partitionManager) {
        return new AnubisScope(partitionIdentity,
                               Executors.newCachedThreadPool(),
                               uuidGenerator(partitionIdentity.id),
                               partitionManager);
    }

    protected NoArgGenerator uuidGenerator(int node) {
        return new RandomBasedGenerator(new Random(node));
    }
}