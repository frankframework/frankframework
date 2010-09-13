/*
 * $Log: CachePipeLineProcessor.java,v $
 * Revision 1.1  2010-09-13 13:50:51  L190409
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
			log.debug("cache key is null, will not use cache");
			return pipeLineProcessor.processPipeLine(pipeLine, messageId, message, pipeLineSession);
		}
		log.debug("cache key ["+key+"]");
		PipeLineResult prr=(PipeLineResult)cache.get(key);
		if (prr==null) {
			log.debug("no cached results found using key ["+key+"]");
			prr=pipeLineProcessor.processPipeLine(pipeLine, messageId, message, pipeLineSession);
			log.debug("caching result using key ["+key+"]");
			cache.put(key, prr);
		} else {
			log.debug("retrieved result from cache using key ["+key+"]");
		}
		return prr;
	}

}
