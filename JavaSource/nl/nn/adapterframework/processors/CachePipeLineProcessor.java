/*
 * $Log: CachePipeLineProcessor.java,v $
 * Revision 1.2  2010-12-13 13:29:01  L190409
 * optimize debugging
 *
 * Revision 1.1  2010/09/13 13:50:51  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * created cache processors
 *
 */
package nl.nn.adapterframework.processors;

import nl.nn.adapterframework.cache.ICacheAdapter;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;

/**
 * PipelineProcessor that handles caching.
 * 
 * @author  Gerrit van Brakel
 * @since   4.11
 * @version Id
 */
public class CachePipeLineProcessor extends PipeLineProcessorBase {
	
	public PipeLineResult processPipeLine(PipeLine pipeLine, String messageId, String message, PipeLineSession pipeLineSession) throws PipeRunException {
		ICacheAdapter cache=pipeLine.getCache();
		if (cache==null) {
			return pipeLineProcessor.processPipeLine(pipeLine, messageId, message, pipeLineSession);
		}
		
		String key=cache.transformKey(message);
		if (key==null) {
			if (log.isDebugEnabled()) log.debug("cache key is null, will not use cache");
			return pipeLineProcessor.processPipeLine(pipeLine, messageId, message, pipeLineSession);
		}
		if (log.isDebugEnabled()) log.debug("cache key ["+key+"]");
		PipeLineResult prr=(PipeLineResult)cache.get(key);
		if (prr==null) {
			if (log.isDebugEnabled()) log.debug("no cached results found using key ["+key+"]");
			prr=pipeLineProcessor.processPipeLine(pipeLine, messageId, message, pipeLineSession);
			if (log.isDebugEnabled()) log.debug("caching result using key ["+key+"]");
			cache.put(key, prr);
		} else {
			if (log.isDebugEnabled()) log.debug("retrieved result from cache using key ["+key+"]");
		}
		return prr;
	}

}
