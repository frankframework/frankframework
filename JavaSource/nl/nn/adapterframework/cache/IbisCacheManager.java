/*
 * $Log: IbisCacheManager.java,v $
 * Revision 1.5  2011-11-30 13:51:48  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:51  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.3  2011/05/25 10:00:56  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fixed resolution of cache directory
 *
 * Revision 1.2  2011/05/25 07:32:48  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * collect statistics
 *
 * Revision 1.1  2010/09/13 13:28:19  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added cache facility
 *
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

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * Common manager for caching.
 * 
 * @author  Gerrit van Brakel
 * @since   4.11
 * @version Id
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
