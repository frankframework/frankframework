/*
   Copyright 2013 Nationale-Nederlanden, 2020, 2022 WeAreFrank!

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
package org.frankframework.processors;

import java.io.IOException;

import org.frankframework.cache.ICache;
import org.frankframework.core.PipeLine;
import org.frankframework.core.PipeLine.ExitState;
import org.frankframework.core.PipeLineResult;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.stream.Message;
import org.frankframework.util.EnumUtils;

/**
 * PipelineProcessor that handles caching.
 *
 * @author  Gerrit van Brakel
 * @since   4.11
 */
public class CachePipeLineProcessor extends AbstractPipeLineProcessor {

	@Override
	public PipeLineResult processPipeLine(PipeLine pipeLine, String messageId, Message message, PipeLineSession pipeLineSession, String firstPipe) throws PipeRunException {
		ICache<String,String> cache=pipeLine.getCache();
		if (cache==null) {
			return pipeLineProcessor.processPipeLine(pipeLine, messageId, message, pipeLineSession, firstPipe);
		}

		String input;
		try {
			input = message.asString();
		} catch (IOException e) {
			throw new PipeRunException(pipeLine.getPipe(firstPipe), "cannot open stream", e);
		}
		String key=cache.transformKey(input, pipeLineSession);
		if (key==null) {
			if (log.isDebugEnabled()) log.debug("cache key is null, will not use cache");
			return pipeLineProcessor.processPipeLine(pipeLine, messageId, message, pipeLineSession, firstPipe);
		}

		if (log.isDebugEnabled()) log.debug("cache key [{}]", key);
		Message result;
		String state;
		synchronized (cache) {
			result = new Message(cache.get("r"+key));
			state = cache.get("s"+key);
		}

		if (!result.isNull() && state!=null) {
			if (log.isDebugEnabled()) log.debug("retrieved result from cache using key [{}]", key);
			PipeLineResult plr=new PipeLineResult();
			plr.setState(EnumUtils.parse(ExitState.class, state));
			plr.setResult(result);
			return plr;
		}

		if (log.isDebugEnabled()) log.debug("no cached results found using key [{}]", key);
		PipeLineResult plr=pipeLineProcessor.processPipeLine(pipeLine, messageId, message, pipeLineSession, firstPipe);
		if (log.isDebugEnabled()) log.debug("caching result using key [{}]", key);
		String cacheValue=cache.transformValue(plr.getResult(), pipeLineSession);
		synchronized (cache) {
			cache.put("r"+key, cacheValue);
			cache.put("s"+key, plr.getState().name());
		}
		return plr;
	}

}
