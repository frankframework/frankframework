/*
Copyright 2017 - 2019 Integration Partners B.V., 2024 WeAreFrank!

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
package org.frankframework.http.rest;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import lombok.Setter;
import net.sf.ehcache.Cache;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;

import org.frankframework.cache.IbisCacheManager;
import org.frankframework.util.AppConstants;
import org.frankframework.util.LogUtil;

public class ApiEhcache implements IApiCache {
	private final Logger log = LogUtil.getLogger(this);

	private static final String KEY_CACHE_NAME = "etagCacheReceiver";
	private static final String KEY_PREFIX="etag.ehcache.";
	private static final String KEY_MAX_ELEMENTS_IN_MEMORY=KEY_PREFIX+"maxElementsInMemory";
	private static final String KEY_MEMORYSTORE_EVICTION_POLICY=KEY_PREFIX+"memoryStoreEvictionPolicy";
	private static final String KEY_ETERNAL=KEY_PREFIX+"eternal";
	private static final String KEY_OVERFLOW_TO_DISK=KEY_PREFIX+"overflowToDisk";
	private static final String KEY_MAX_ELEMENTS_ON_DISK=KEY_PREFIX+"maxElementsOnDisk";
	private static final String KEY_DISK_PERSISTENT=KEY_PREFIX+"diskPersistent";
	private static final String KEY_DISK_EXPIRY_THREAD_INTERVAL_SECONDS=KEY_PREFIX+"diskExpiryThreadIntervalSeconds";

	private int maxElementsInMemory=512;
	private String memoryStoreEvictionPolicy="LRU";
	@Setter private boolean eternal = true;
	@Setter private boolean overflowToDisk = false;
	@Setter private int maxElementsOnDisk = 10000;
	@Setter private boolean diskPersistent = false;
	private int diskExpiryThreadIntervalSeconds=600;

	private Ehcache cache;
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
		if (diskPersistent && !overflowToDisk) {
			log.info("setting overflowToDisk true, to support diskPersistent=true");
			setOverflowToDisk(true);
		}

		String diskStorePath = null;
		String cacheDir = ac.getProperty("etag.ehcache.dir");
		if (StringUtils.isNotEmpty(cacheDir)) {
			diskStorePath = cacheDir;
		}

		Cache configCache = new Cache(
				KEY_CACHE_NAME,
				maxElementsInMemory,
				MemoryStoreEvictionPolicy.fromString(memoryStoreEvictionPolicy),
				overflowToDisk,
				diskStorePath,
				eternal,
				0,
				0,
				diskPersistent,
				diskExpiryThreadIntervalSeconds,
				null,
				null,
				maxElementsOnDisk
			);
		cache = cacheManager.addCache(configCache);
	}

	@Override
	public void destroy() {
		if (diskPersistent) {
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
		return this.get(key) != null;
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

}
