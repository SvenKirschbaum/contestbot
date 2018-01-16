package de.elite12.contestbot;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

public class AuthProvider {
    private static LoadingCache<String, Boolean> cache = CacheBuilder.newBuilder().concurrencyLevel(2)
            .maximumSize(1000000).expireAfterWrite(24, TimeUnit.HOURS).build(new CacheLoader<String, Boolean>() {
                @Override
                public Boolean load(String key) throws Exception {
                    return Boolean.FALSE;
                };
            });
    
    public static void MarkPrivileged(String user) {
        cache.put(user.toLowerCase(), Boolean.TRUE);
    }

    public static Boolean checkPrivileged(String user) {
        try {
            return cache.get(user.toLowerCase());
        } catch (ExecutionException e) {
            Logger.getLogger(AuthProvider.class).fatal("This Error is impossible to trigger", e);
            return false;
        }
    }
}
