/*
   Copyright 2017, 2021-2022, 2024 WeAreFrank!

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

import org.frankframework.http.rest.ApiListener.HttpMethod;
import org.frankframework.util.AppConstants;

public class ApiCacheManager {

	private static IApiCache cache = null;
	private static final AppConstants appConstants = AppConstants.getInstance();
	private static final String etagCacheType = appConstants.getProperty("etag.cache.type", "ehcache");
	private static final String instanceName = appConstants.getProperty("instance.name");
	private static final String dtapStage = appConstants.getProperty("dtap.stage");

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
	 * @return cachePrefix 'instanceName_dtapStage_'
	 */
	public static String buildCacheKey(String uriPattern) {
		return instanceName + "_" + dtapStage.toUpperCase() + "_" + uriPattern;
	}

	public static String getParentCacheKey(ApiListener listener, String uri, HttpMethod method) {
		String pattern = listener.getCleanPattern();
		// Not only remove the eTag for the selected resources but also the collection
		if((method == HttpMethod.PUT || method == HttpMethod.PATCH || method == HttpMethod.DELETE) && pattern != null && pattern.endsWith("/*")) {
			//Check the amount of asterisks, if there is only 1, this will return false
			if(listener.getCleanPattern().indexOf("*") < listener.getCleanPattern().lastIndexOf("*")) {
				//Get collection uri
				String parentUri = uri.substring(0, uri.lastIndexOf("/"));
				return buildCacheKey(parentUri);
			}
		}
		return null;
	}
}
