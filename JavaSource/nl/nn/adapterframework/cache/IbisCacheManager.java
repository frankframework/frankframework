/*
 * $Log: IbisCacheManager.java,v $
 * Revision 1.1  2010-09-13 13:28:19  L190409
 * added cache facility
 *
 */
package nl.nn.adapterframework.cache;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.DiskStoreConfiguration;
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
		String cacheDir = AppConstants.getInstance().getProperty(CACHE_DIR_KEY,null);
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
	
}
