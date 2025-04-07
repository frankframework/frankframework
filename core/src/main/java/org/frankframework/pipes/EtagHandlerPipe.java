/*
   Copyright 2017, 2020, 2022 WeAreFrank!

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
package org.frankframework.pipes;

import java.io.IOException;

import lombok.Getter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarning;
import org.frankframework.core.ParameterException;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.http.rest.ApiCacheManager;
import org.frankframework.http.rest.ApiEhcache;
import org.frankframework.http.rest.IApiCache;
import org.frankframework.parameters.IParameter;
import org.frankframework.parameters.ParameterList;
import org.frankframework.parameters.ParameterValue;
import org.frankframework.parameters.ParameterValueList;
import org.frankframework.stream.Message;
import org.frankframework.util.MessageUtils;

/**
 * Pipe to manage RESTful ETag caching.
 *
 * @author Niels Meijer
 *
 */
@Deprecated
@ConfigurationWarning("Please configure eTag caching on the ApiListener")
public class EtagHandlerPipe extends FixedForwardPipe {
	private @Getter EtagAction action = null;
	private @Getter String restPath = "/rest";
	private String uriPattern = null;
	private IApiCache cache = null;

//	hash over data genereren, uit cache lezen en teruggeven, in cache updaten, verwijderen uit cache, cache naar disk wegschrijven, cache legen
	public enum EtagAction {
		GENERATE, GET, SET, DELETE, FLUSH, CLEAR
	}

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		EtagAction action = getAction();
		if (action==null) {
			throw new ConfigurationException("action must be set");
		}

		boolean hasUriPatternParameter = false;
		ParameterList parameterList = getParameterList();
		for (int i=0; i<parameterList.size(); i++) {
			IParameter parameter = parameterList.getParameter(i);
			if("uriPattern".equalsIgnoreCase(parameter.getName()))
				hasUriPatternParameter = true;
		}

		if(getUriPattern() == null && !hasUriPatternParameter) {
			throw new ConfigurationException("no uriPattern found!");
		}

		cache = ApiCacheManager.getInstance();
	}

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		if (message==null) {
			throw new PipeRunException(this, "got null input");
		}

		String uriPatternSessionKey = null;
		ParameterValueList pvl = null;
		ParameterList parameterList = getParameterList();
		try {
			pvl = parameterList.getValues(message, session);
			ParameterValue pv = pvl.get("uriPattern");
			if (pv != null) {
				uriPatternSessionKey = pv.asStringValue();
			}
		} catch (ParameterException e) {
			throw new PipeRunException(this, "exception extracting parameters", e);
		}

		//hash over data genereren, uit cache lezen en teruggeven, in cache updaten, verwijderen uit cache, cache naar disk wegschrijven, cache legen
		String cacheKey = null;
		if(uriPatternSessionKey != null && !uriPatternSessionKey.isEmpty())
			cacheKey = getRestPath()+"_"+uriPatternSessionKey.toLowerCase();
		else
			cacheKey = getRestPath()+"_"+getUriPattern();
		if(cache != null && cache.containsKey(cacheKey)) {
			Object returnCode = false;

			log.debug("found eTag cacheKey [{}] with action [{}]", cacheKey, getAction());
			switch (getAction()) {
			case GENERATE:
				String hash = MessageUtils.generateMD5Hash(message);
				if(hash != null) {
					cache.put(cacheKey, hash);
				}
				returnCode = true;
				break;
			case GET:
				returnCode = cache.get(cacheKey);
				break;
			case SET:
				try {
					cache.put(cacheKey, message.asString());
				} catch (IOException e) {
					throw new PipeRunException(this, "cannot open stream", e);
				}
				returnCode = true;
				break;
			case DELETE:
				returnCode = cache.remove(cacheKey);
				break;
			case FLUSH:
				if(cache instanceof ApiEhcache ehcache) {
					ehcache.flush();
					returnCode = true;
				}
				break;
			case CLEAR:
				cache.clear();
				returnCode = true;
				break;

			default:
				throw new PipeRunException(this, "action not found ["+getAction()+"]");
			}

			return new PipeRunResult(getSuccessForward(), returnCode);
		}
		PipeForward pipeForward = findForward(PipeForward.EXCEPTION_FORWARD_NAME);
		String msg;

		if(cache == null)
			msg = "failed to locate cache";
		else
			msg = "failed to locate eTag ["+cacheKey+"] in cache";

		if (pipeForward==null) {
			throw new PipeRunException (this, msg);
		}

		return new PipeRunResult(pipeForward, "");
	}

	public void setAction(EtagAction action) {
		this.action = action;
	}

	public void setUriPattern(String string) {
		uriPattern = string;
	}

	public String getUriPattern() {
		if(uriPattern != null) {
			return uriPattern.toLowerCase();
		}
		return null;
	}

	public void setRestPath(String string) {
		restPath = string;
	}

}
