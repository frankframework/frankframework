/*
   Copyright 2013 Nationale-Nederlanden

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
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.PipeRunException;

/**
 * PipelineProcessor that handles caching.
 * 
 * @author  Gerrit van Brakel
 * @since   4.11
 * @version $Id$
 */
public class CachePipeLineProcessor extends PipeLineProcessorBase {
	
	public PipeLineResult processPipeLine(PipeLine pipeLine, String messageId, String message, IPipeLineSession pipeLineSession, String firstPipe) throws PipeRunException {
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
