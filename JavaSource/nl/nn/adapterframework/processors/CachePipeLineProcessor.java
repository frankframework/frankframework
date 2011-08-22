/*
 * $Log: CachePipeLineProcessor.java,v $
 * Revision 1.4  2011-08-22 14:29:59  L190409
 * added first pipe to interface
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
	
	public PipeLineResult processPipeLine(PipeLine pipeLine, String messageId, String message, PipeLineSession pipeLineSession, String firstPipe) throws PipeRunException {
		ICacheAdapter cache=pipeLine.getCache();
		if (cache==null) {
			return pipeLineProcessor.processPipeLine(pipeLine, messageId, message, pipeLineSession, firstPipe);
		}
		
		String key=cache.transformKey(message, pipeLineSession);
		if (key==null) {
			if (log.isDebugEnabled()) log.debug("cache key is null, will not use cache");
			return pipeLineProcessor.processPipeLine(pipeLine, messageId, message, pipeLineSession, firstPipe);
		}
		if (log.isDebugEnabled()) log.debug("cache key ["+key+"]");
		String result;
		String state;
		synchronized (cache) {
			result = cache.getString("r"+key);
			state = cache.getString("s"+key);
		}
		if (result!=null && state!=null) {
			if (log.isDebugEnabled()) log.debug("retrieved result from cache using key ["+key+"]");
			PipeLineResult prr=new PipeLineResult();
			prr.setState(state);
			prr.setResult(result);
			return prr;
		}
		if (log.isDebugEnabled()) log.debug("no cached results found using key ["+key+"]");
		PipeLineResult prr=pipeLineProcessor.processPipeLine(pipeLine, messageId, message, pipeLineSession, firstPipe);
		if (log.isDebugEnabled()) log.debug("caching result using key ["+key+"]");
		String cacheValue=cache.transformValue(prr.getResult(), pipeLineSession);
		synchronized (cache) {
			cache.putString("r"+key, cacheValue);
			cache.putString("s"+key, prr.getState());
		}
		return prr;
	}

}
