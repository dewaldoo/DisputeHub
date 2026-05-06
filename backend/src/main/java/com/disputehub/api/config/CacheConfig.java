package com.disputehub.api.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Cache Configuration using Caffeine.
 *
 * CACHING STRATEGY:
 * - Cache is invalidated on writes (event-driven via @CacheEvict)
 * - 24-hour TTL as safety net only (data doesn't change without explicit writes)
 * - Maximum 1000 entries per cache
 *
 * WHY THIS APPROACH:
 * - Data only changes on dispute creation/updates
 * - Event-driven invalidation is more efficient than short TTLs
 *
 * CACHES:
 * - transactions: User transaction lists
 * - disputeableTransactions: Transactions without disputes
 * - myDisputes: Customer dispute lists
 * - allDisputes: Admin dashboard all disputes
 * - auditLogs: Dispute audit log history
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                "transactions",
                "disputeableTransactions",
                "myDisputes",
                "allDisputes",
                "auditLogs"
        );

        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(24, TimeUnit.HOURS)  // Safety net TTL
                .maximumSize(1000)                      // Max entries per cache
                .recordStats());                        // Enable cache statistics

        return cacheManager;
    }
}
