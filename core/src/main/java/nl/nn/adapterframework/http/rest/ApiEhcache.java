/*
Copyright 2017 Integration Partners B.V.

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

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

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
		String cacheDir = ac.getResolvedProperty("etag.ehcache.dir");
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

	public void put(String key, Object value, int ttl) {
		Element element = new Element(key,value);
		element.setTimeToLive(ttl);
		cache.put(element);
	}

	public boolean remove(String key) {
		return cache.remove(key);
	}

	public boolean containsKey(String string) {
		//workaround to avoid NPE after a full reload (/adapterHandlerAsAdmin.do?action=fullreload)
		//TODO: fix this in a proper way
		if (cache.getStatus().equals(Status.STATUS_SHUTDOWN)) {
			log.info("cache ["+KEY_CACHE_NAME+"] has status shutdown, so returning false");
			return false;
		}
		return cache.isKeyInCache(string);
	}

	public void flush() {
		cache.flush();
	}

	public void clear() {
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
