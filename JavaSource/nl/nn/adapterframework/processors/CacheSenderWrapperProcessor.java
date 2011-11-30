/*
 * $Log: CacheSenderWrapperProcessor.java,v $
 * Revision 1.5  2011-11-30 13:51:54  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:50  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.3  2011/05/31 15:30:35  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * support for new cache features
 *
 * Revision 1.2  2010/12/13 13:29:01  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * optimize debugging
 *
 * Revision 1.1  2010/09/13 13:50:51  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * created cache processors
 *
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
 * @version Id
 */
public class CacheSenderWrapperProcessor extends SenderWrapperProcessorBase {
	
	public String sendMessage(SenderWrapperBase senderWrapperBase, String correlationID, String message, ParameterResolutionContext prc) throws SenderException, TimeOutException {
		ICacheAdapter cache=senderWrapperBase.getCache();
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
			cache.putString(key, cacheValue);
		} else {
			if (log.isDebugEnabled()) log.debug("retrieved result from cache using key ["+key+"]");
		}
		return result;
	}

}
