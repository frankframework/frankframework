package nl.nn.adapterframework.http.rest;

import nl.nn.adapterframework.util.AppConstants;

public class ApiCacheManager {

	private static IApiCache cache = null;
	private static AppConstants appConstants = AppConstants.getInstance();
	private static String etagCacheType = appConstants.getProperty("etag.cache.type", "ehcache");
	private static String instanceName = appConstants.getResolvedProperty("instance.name");
	private static String otapStage = appConstants.getResolvedProperty("otap.stage");

	/**
	 * Get the etagCache, defaults to EhCache when no type has been specified.
	 * @return IRestEtagCache
	 */
	public static synchronized IApiCache getInstance() {
		if( cache == null ) {
			if(etagCacheType.equalsIgnoreCase("memcached")) {
				cache = new ApiMemcached();
			}
			else {
				cache = new ApiEhcache();
			}
		}
		return cache;
	}

	/**
	 * Creates an IBIS independent cachePrefix so multiple IBIS can connect to the same cache
	 * @return cachePrefix 'instanceName_otapStage_'
	 */
	public static String buildCacheKey(String uriPattern) {
		return instanceName + "_" + otapStage.toUpperCase() + "_" + uriPattern;
	}

	public static String buildEtag(String uriPattern, int hash) {
		return Integer.toOctalString(instanceName.hashCode()) + "_" +Integer.toHexString(uriPattern.hashCode()) + "_" + hash;
	}
}
