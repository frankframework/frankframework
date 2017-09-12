package nl.nn.adapterframework.http.rest;

import nl.nn.adapterframework.util.AppConstants;

public class EtagCacheManager {

	private static IRestEtagCache cache = null;
	private static AppConstants appConstants = AppConstants.getInstance();
	private static String etagCacheType = appConstants.getProperty("etag.cache.type", "ehcache");
	private static String instanceName = appConstants.getResolvedProperty("instance.name");
	private static String otapStage = appConstants.getResolvedProperty("otap.stage");

	/**
	 * Get the etagCache, defaults to EhCache when no type has been specified.
	 * @return IRestEtagCache
	 */
	public static synchronized IRestEtagCache getInstance() {
		if( cache == null ) {
			if(etagCacheType.equalsIgnoreCase("memcached")) {
				cache = new RestEtagMemcached();
			}
			else {
				cache = new RestEtagEhcache();
			}
		}
		return cache;
	}

	/**
	 * Creates an IBIS independent cachePrefix so multiple IBIS can connect to the same cache
	 * @return cachePrefix 'instanceName_otapStage_'
	 */
	public static String buildCacheKey(String etag) {
		return instanceName + "_" + otapStage.toUpperCase() + "_" + etag;
	}
}
