package com.lojapp.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(
            @Value("${lojapp.dashboard.cache-ttl-minutes:5}") long dashboardCacheTtlMinutes) {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setCacheNames(
                List.of(
                        CacheNames.DASHBOARD_BRANDS,
                        CacheNames.DASHBOARD_PRODUCT_ABC,
                        CacheNames.DASHBOARD_INVENTORY_KPIS));
        manager.setCaffeine(
                Caffeine.newBuilder()
                        .maximumSize(10_000)
                        .expireAfterWrite(Duration.ofMinutes(dashboardCacheTtlMinutes)));
        return manager;
    }
}
