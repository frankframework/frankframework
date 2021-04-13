/*
   Copyright 2013, 2016 Nationale-Nederlanden

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
package nl.nn.adapterframework.cache;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Statistics;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.DiskStoreConfiguration;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.statistics.StatisticsKeeperIterationHandler;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

/**
 * Common manager for caching.
 * 
 * @author  Gerrit van Brakel
 * @since   4.11
 */
public class IbisCacheManager {
	protected Logger log = LogUtil.getLogger(this);

	private final String CACHE_DIR_KEY="cache.dir";
	
	private static IbisCacheManager self;
	private CacheManager cacheManager=null;
	
	private IbisCacheManager() {
		Configuration cacheManagerConfig = new Configuration();
		String cacheDir = AppConstants.getInstance().getResolvedProperty(CACHE_DIR_KEY);
		if (StringUtils.isNotEmpty(cacheDir)) {
			log.debug("setting cache directory to ["+cacheDir+"]");
			DiskStoreConfiguration diskStoreConfiguration = new DiskStoreConfiguration();
			diskStoreConfiguration.setPath(cacheDir);
			cacheManagerConfig.addDiskStore(diskStoreConfiguration);
		} 
		CacheConfiguration defaultCacheConfig = new CacheConfiguration();
		cacheManagerConfig.addDefaultCache(defaultCacheConfig);
		cacheManager= new CacheManager(cacheManagerConfig);
	}
	
	public synchronized static IbisCacheManager getInstance() {
		if (self==null) {
			self=new IbisCacheManager();
		}
		return self;
	}
	
	public synchronized static void shutdown() {
		if (self!=null) {
			self.log.debug("shutting down cacheManager...");
			self.cacheManager.shutdown();
			self.log.info("cacheManager shutdown");
			self.cacheManager=null;
			self=null;
		}
	}
	
	public Ehcache addCache(Cache cache) {
		log.debug("registering cache ["+cache.getName()+"]");
		cacheManager.addCache(cache);
		return cacheManager.getEhcache(cache.getName());
	}

	public void removeCache(String cacheName) {
		log.debug("deregistering cache ["+cacheName+"]");
		cacheManager.removeCache(cacheName);
	}

	public Cache getCache(String cacheName) {
		return cacheManager.getCache(cacheName);
	}

	public static void iterateOverStatistics(StatisticsKeeperIterationHandler hski, Object data, int action) throws SenderException {
		if (self==null) {
			return;
		}
		String cacheNames[]=self.cacheManager.getCacheNames();
		for (int i=0;i<cacheNames.length;i++) {
			Object subdata=hski.openGroup(data, cacheNames[i], "cache");
			Ehcache cache=self.cacheManager.getEhcache(cacheNames[i]);
			Statistics stats = cache.getStatistics();
			stats.getAverageGetTime();
			hski.handleScalar(subdata, "CacheHits", stats.getCacheHits());
			hski.handleScalar(subdata, "CacheMisses", stats.getCacheMisses());
			hski.handleScalar(subdata, "EvictionCount", stats.getEvictionCount());
			hski.handleScalar(subdata, "InMemoryHits", stats.getInMemoryHits());
			hski.handleScalar(subdata, "ObjectCount", stats.getObjectCount());
			hski.handleScalar(subdata, "OnDiskHits", stats.getOnDiskHits());
			hski.closeGroup(subdata);
		}
	}
	
}
