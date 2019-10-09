/*
   Copyright 2013, 2017 Nationale-Nederlanden

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
package nl.nn.adapterframework.processors;

import nl.nn.adapterframework.cache.ICacheAdapter;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.senders.SenderWrapperBase;

/**
 * SenderWrapperProcessor that handles caching.
 * 
 * @author  Gerrit van Brakel
 * @since   4.11
 */
public class CacheSenderWrapperProcessor extends SenderWrapperProcessorBase {
	
	public String sendMessage(SenderWrapperBase senderWrapperBase, String correlationID, String message, ParameterResolutionContext prc) throws SenderException, TimeOutException {
		ICacheAdapter<String,String> cache=senderWrapperBase.getCache();
		if (cache==null) {
			return senderWrapperProcessor.sendMessage(senderWrapperBase, correlationID, message, prc);
		}
		
		String key=cache.transformKey(message, prc.getSession());
		if (key==null) {
			if (log.isDebugEnabled()) log.debug("cache key is null, will not use cache");
			return senderWrapperProcessor.sendMessage(senderWrapperBase, correlationID, message, prc);
		}
		if (log.isDebugEnabled()) log.debug("cache key ["+key+"]");
		String result=cache.getString(key);
		if (result==null) {
			if (log.isDebugEnabled()) log.debug("no cached results found using key ["+key+"]");
			result=senderWrapperProcessor.sendMessage(senderWrapperBase, correlationID, message, prc);
			if (log.isDebugEnabled()) log.debug("caching result using key ["+key+"]");
			String cacheValue=cache.transformValue(result, prc.getSession());
			if (cacheValue==null) {
				if (log.isDebugEnabled()) log.debug("transformed cache value is null, will not cache");
				return result;
			}
			result = cacheValue;
			cache.putString(key, result);
		} else {
			if (log.isDebugEnabled()) log.debug("retrieved result from cache using key ["+key+"]");
		}
		return result;
	}

}
