package io.quarkus.cache.runtime;

import java.util.concurrent.Executor;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CacheExecutor {

    private Executor executor;

    public Executor getExecutor() {
        return executor;
    }

    public void setExecutor(Executor executor) {
        this.executor = executor;
    }
}
