/**
 * Michelin CERT 2020.
 */

package com.michelin.cert.redscan;

import com.michelin.cert.redscan.utils.caching.CacheManager;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Configure Cache manager.
 *
 * @author Maxime ESCOURBIAC
 */
@Configuration
public class CacheConfig extends CacheManager {

  @Value("${cache.manager.url}")
  private String cacheManagerUrlProperty;

  @Autowired
  public CacheConfig() {
    super();
  }

  @PostConstruct
  public void initCacheManager() {
    cacheManagerUrl = cacheManagerUrlProperty;
    applicationName = "Redscan-Urlcrazy";
  }

}
