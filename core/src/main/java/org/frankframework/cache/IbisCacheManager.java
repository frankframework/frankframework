/*
   Copyright 2013, 2016 Nationale-Nederlanden, 2022 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package org.frankframework.cache;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.DiskStoreConfiguration;
import net.sf.ehcache.statistics.StatisticsGateway;
import org.frankframework.core.SenderException;
import org.frankframework.statistics.HasStatistics.Action;
import org.frankframework.statistics.MetricsInitializer;
import org.frankframework.statistics.StatisticsKeeperIterationHandler;
import org.frankframework.util.AppConstants;
import org.frankframework.util.LogUtil;

/**
 * Common manager for caching.
 *
 * @author  Gerrit van Brakel
 * @since   4.11
 */
public class IbisCacheManager {
	protected Logger log = LogUtil.getLogger(this);

	private static final String CACHE_DIR_KEY="cache.dir";

	private static IbisCacheManager self;
	private CacheManager cacheManager=null;

	private IbisCacheManager() {
		Configuration cacheManagerConfig = new Configuration();
		String cacheDir = AppConstants.getInstance().getProperty(CACHE_DIR_KEY);
		if (StringUtils.isNotEmpty(cacheDir)) {
			log.debug("setting cache directory to [{}]", cacheDir);
			DiskStoreConfiguration diskStoreConfiguration = new DiskStoreConfiguration();
			diskStoreConfiguration.setPath(cacheDir);
			cacheManagerConfig.addDiskStore(diskStoreConfiguration);
		}
		CacheConfiguration defaultCacheConfig = new CacheConfiguration();
		cacheManagerConfig.addDefaultCache(defaultCacheConfig);
		cacheManager= new CacheManager(cacheManagerConfig);
	}

	public static synchronized IbisCacheManager getInstance() {
		if (self==null) {
			self=new IbisCacheManager();
		}
		return self;
	}

	public static synchronized void shutdown() {
		if (self!=null) {
			self.log.debug("shutting down cacheManager...");
			self.cacheManager.shutdown();
			self.log.info("cacheManager shutdown");
			self.cacheManager=null;
			self=null;
		}
	}

	public Ehcache addCache(Cache cache) {
		log.debug("registering cache [{}]", cache.getName());
		cacheManager.addCache(cache); //ObjectExistsException
		return cacheManager.getEhcache(cache.getName());
	}

	public void destroyCache(String cacheName) {
		log.debug("destroying cache [{}]", cacheName);
		cacheManager.removeCache(cacheName);
	}

	public Cache getCache(String cacheName) {
		return cacheManager.getCache(cacheName);
	}

	public static <D> void iterateOverStatistics(StatisticsKeeperIterationHandler<D> hski, D data, Action action) throws SenderException {
		if (self==null) {
			return;
		}
		String[] cacheNames = self.cacheManager.getCacheNames();
		for (int i=0;i<cacheNames.length;i++) {
			D subdata=hski.openGroup(data, cacheNames[i], "cache");
			Ehcache cache=self.cacheManager.getEhcache(cacheNames[i]);
			if (hski instanceof MetricsInitializer) {
				((MetricsInitializer)hski).configureCache(cache);
			} else {
				StatisticsGateway stats = cache.getStatistics();
				hski.handleScalar(subdata, "CacheHits", stats.cacheHitCount());
				hski.handleScalar(subdata, "CacheMisses", stats.cacheMissCount());
				hski.handleScalar(subdata, "EvictionCount", stats.cacheEvictedCount());
				hski.handleScalar(subdata, "InMemoryHits", stats.localHeapHitCount());
				hski.handleScalar(subdata, "ObjectCount", cache.getSize());
				hski.handleScalar(subdata, "OnDiskHits", stats.localDiskHitCount());
				hski.closeGroup(subdata);
			}
		}
	}

}
