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
/*
 * $Log: CachePipeLineProcessor.java,v $
 * Revision 1.7  2012-06-01 10:52:49  m00f069
 * Created IPipeLineSession (making it easier to write a debugger around it)
 *
 * Revision 1.6  2011/11/30 13:51:54  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:50  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.4  2011/08/22 14:29:59  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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
