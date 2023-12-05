/*
Copyright 2017 - 2019 Integration Partners B.V.

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
package nl.nn.adapterframework.http.rest;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;
import nl.nn.adapterframework.cache.IbisCacheManager;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.LogUtil;

public class ApiEhcache implements IApiCache {
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

	public ApiEhcache() {
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
			createCache(ac);
		}
	}

	private void createCache(AppConstants ac) {
		if (isDiskPersistent() && !isOverflowToDisk()) {
			log.info("setting overflowToDisk true, to support diskPersistent=true");
			setOverflowToDisk(true);
		}

		String DiskStorePath = null;
		String cacheDir = ac.getProperty("etag.ehcache.dir");
		if (StringUtils.isNotEmpty(cacheDir)) {
			DiskStorePath = cacheDir;
		}

		Cache configCache = new Cache(
				KEY_CACHE_NAME,
				maxElementsInMemory,
				MemoryStoreEvictionPolicy.fromString(memoryStoreEvictionPolicy),
				isOverflowToDisk(),
				DiskStorePath,
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

	@Override
	public void destroy() {
		if (isDiskPersistent()) {
			log.debug("cache ["+KEY_CACHE_NAME+"] flushing to disk");
			cache.flush();
		} else {
			log.debug("cache ["+KEY_CACHE_NAME+"] clearing data");
			cache.removeAll();
		}
		if (cacheManager!=null) {
			cacheManager.destroyCache(cache.getName());
			cacheManager=null;
		}
		cache=null;
	}

	/**
	 * The cache can only check if a key exists if it's state is ALIVE
	 */
	private boolean isCacheAlive() {
		if(cache == null)
			return false;

		return Status.STATUS_ALIVE.equals(cache.getStatus());
	}

	/**
	 * Workaround to avoid NPE after a full reload (/adapterHandlerAsAdmin.do?action=fullreload)
	 * get() and isKeyInCache() are not synchronized methods and do not contain any state checking.
	 */
	@Override
	public Object get(String key) {
		if(!isCacheAlive())
			return null;

		Element element = cache.get(key);
		if (element==null) {
			return null;
		}
		return element.getObjectValue();
	}

	@Override
	public void put(String key, Object value) {
		if(!isCacheAlive())
			return;

		Element element = new Element(key,value);
		cache.put(element);
	}

	@Override
	public void put(String key, Object value, int ttl) {
		if(!isCacheAlive())
			return;

		Element element = new Element(key,value);
		element.setTimeToLive(ttl);
		cache.put(element);
	}

	@Override
	public boolean remove(String key) {
		if(!isCacheAlive())
			return false;

		return cache.remove(key);
	}

	@Override
	public boolean containsKey(String key) {
		return (this.get(key) != null);
	}

	public void flush() {
		if(!isCacheAlive())
			return;

		cache.flush();
	}

	@Override
	public void clear() {
		if(!isCacheAlive())
			return;

		cache.removeAll();
	}

	private boolean isEternal() {
		return eternal;
	}
	public void setEternal(boolean eternal) {
		this.eternal = eternal;
	}

	private boolean isOverflowToDisk() {
		return overflowToDisk;
	}
	public void setOverflowToDisk(boolean overflowToDisk) {
		this.overflowToDisk = overflowToDisk;
	}

	private int getMaxElementsOnDisk() {
		return maxElementsOnDisk;
	}
	public void setMaxElementsOnDisk(int maxElementsOnDisk) {
		this.maxElementsOnDisk = maxElementsOnDisk;
	}

	private boolean isDiskPersistent() {
		return diskPersistent;
	}
	public void setDiskPersistent(boolean diskPersistent) {
		this.diskPersistent = diskPersistent;
	}
}
