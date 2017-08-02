package nl.nn.adapterframework.http;

import org.apache.log4j.Logger;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;
import nl.nn.adapterframework.cache.IbisCacheManager;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.LogUtil;

public class RestEtagEhcache implements IRestEtagCache {
	private Logger log = LogUtil.getLogger(this);

	private final String KEY_CACHE_NAME="etagCacheReceiver";
	private final String KEY_PREFIX="etag.ehcache.";
	private final String KEY_MAX_ELEMENTS_IN_MEMORY=KEY_PREFIX+"maxElementsInMemory";
	private final String KEY_MEMORYSTORE_EVICTION_POLICY=KEY_PREFIX+"memoryStoreEvictionPolicy";
	private final String KEY_ETERNAL=KEY_PREFIX+"eternal";
	private final String KEY_OVERFLOW_TO_DISK=KEY_PREFIX+"overflowToDisk";
	private final String KEY_MAX_ELEMENTS_ON_DISK=KEY_PREFIX+"maxElementsOnDisk";
	private final String KEY_DISK_PERSISTENT=KEY_PREFIX+"diskPersistent";
	private final String KEY_DISK_EXPIRY_THREAD_INTERVAL_SECONDS=KEY_PREFIX+"diskExpiryThreadIntervalSeconds";

	private int maxElementsInMemory=512;
	private String memoryStoreEvictionPolicy="LRU";
	private boolean eternal=true;
	private boolean overflowToDisk=false;
	private int maxElementsOnDisk=10000;
	private boolean diskPersistent=false;
	private int diskExpiryThreadIntervalSeconds=600;

	private Ehcache cache=null;
	private IbisCacheManager cacheManager=null;

	public RestEtagEhcache() {
		cacheManager = IbisCacheManager.getInstance();

		AppConstants ac = AppConstants.getInstance();
		maxElementsInMemory=ac.getInt(KEY_MAX_ELEMENTS_IN_MEMORY, maxElementsInMemory);
		memoryStoreEvictionPolicy=ac.getProperty(KEY_MEMORYSTORE_EVICTION_POLICY, memoryStoreEvictionPolicy);
		eternal=ac.getBoolean(KEY_ETERNAL, eternal);
		overflowToDisk=ac.getBoolean(KEY_OVERFLOW_TO_DISK, overflowToDisk);
		maxElementsOnDisk=ac.getInt(KEY_MAX_ELEMENTS_ON_DISK, maxElementsOnDisk);
		diskPersistent=ac.getBoolean(KEY_DISK_PERSISTENT, diskPersistent);
		diskExpiryThreadIntervalSeconds=ac.getInt(KEY_DISK_EXPIRY_THREAD_INTERVAL_SECONDS, diskExpiryThreadIntervalSeconds);

		cache = cacheManager.getCache(KEY_CACHE_NAME);
		if(cache == null) {
			createCache();
		}
	}

	private void createCache() {
		if (isDiskPersistent() && !isOverflowToDisk()) {
			log.info("setting overflowToDisk true, to support diskPersistent=true");
			setOverflowToDisk(true);
		}

		Cache configCache = new Cache(
				KEY_CACHE_NAME,
				maxElementsInMemory,
				MemoryStoreEvictionPolicy.fromString(memoryStoreEvictionPolicy),
				isOverflowToDisk(),
				null,
				isEternal(),
				0,
				0,
				isDiskPersistent(),
				diskExpiryThreadIntervalSeconds,
				null,
				null,
				getMaxElementsOnDisk()
			);
		cache = cacheManager.addCache(configCache);
	}

	public void destroy() {
		if (isDiskPersistent()) {
			log.debug("cache ["+KEY_CACHE_NAME+"] flushing to disk");
			cache.flush();
		} else {
			log.debug("cache ["+KEY_CACHE_NAME+"] clearing data");
			cache.removeAll();
		}
		if (cacheManager!=null) {
			cacheManager.removeCache(cache.getName());
			cacheManager=null;
		}
		cache=null;
	}


	public Object get(String key) {
		Element element = cache.get(key);
		if (element==null) {
			return null;
		}
		return element.getObjectValue();
	}

	public void put(String key, Object value) {
		Element element = new Element(key,value);
		cache.put(element);
	}

	public boolean remove(String key) {
		return cache.remove(key);
	}

	public boolean containsKey(String string) {
		return cache.isKeyInCache(string);
	}

	public void flush() {
		cache.flush();
	}

	public void removeAll() {
		cache.removeAll();
	}




	public boolean isEternal() {
		return eternal;
	}
	public void setEternal(boolean eternal) {
		this.eternal = eternal;
	}

	public boolean isOverflowToDisk() {
		return overflowToDisk;
	}
	public void setOverflowToDisk(boolean overflowToDisk) {
		this.overflowToDisk = overflowToDisk;
	}

	public int getMaxElementsOnDisk() {
		return maxElementsOnDisk;
	}
	public void setMaxElementsOnDisk(int maxElementsOnDisk) {
		this.maxElementsOnDisk = maxElementsOnDisk;
	}

	public boolean isDiskPersistent() {
		return diskPersistent;
	}
	public void setDiskPersistent(boolean diskPersistent) {
		this.diskPersistent = diskPersistent;
	}
}
