/*
   Copyright 2017, 2020 Integration Partners

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
package nl.nn.adapterframework.pipes;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.http.RestListenerUtils;
import nl.nn.adapterframework.http.rest.ApiCacheManager;
import nl.nn.adapterframework.http.rest.ApiEhcache;
import nl.nn.adapterframework.http.rest.IApiCache;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.stream.Message;

/**
 * Pipe to manage RESTFUL etag caching
 * 
 * @author Niels Meijer
 *
 */
public class EtagHandlerPipe extends FixedForwardPipe {
	private String action = null;
//	hash over data genereren, uit cache lezen en teruggeven, in cache updaten, verwijderen uit cache, cache naar disk wegschrijven, cache legen
	List<String> actions = Arrays.asList("generate", "get", "set", "delete", "flush", "clear");
	private String restPath = "/rest";
	private String uriPattern = null;
	private IApiCache cache = null;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		String action = getAction();
		if (action==null) {
			throw new ConfigurationException("action must be set");
		}
		if (!actions.contains(action)) {
			throw new ConfigurationException("illegal value for action ["+action+"], must be one of " + actions.toString());
		}

		boolean hasUriPatternParameter = false;
		ParameterList parameterList = getParameterList();
		for (int i=0; i<parameterList.size(); i++) {
			Parameter parameter = parameterList.getParameter(i);
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
			throw new PipeRunException(this, getLogPrefix(session)+"got null input");
		}

		String uriPatternSessionKey = null;
		ParameterValueList pvl = null;
		ParameterList parameterList = getParameterList();
		if (parameterList != null) {
			try {
				pvl = parameterList.getValues(message, session);
				if (pvl != null) {
					String uriPattern = (String)pvl.getValue("uriPattern");
					if (uriPattern!=null) {
						uriPatternSessionKey = uriPattern;
					}
				}
			} catch (ParameterException e) {
				throw new PipeRunException(this, getLogPrefix(session) + "exception extracting parameters", e);
			}
		}

		//hash over data genereren, uit cache lezen en teruggeven, in cache updaten, verwijderen uit cache, cache naar disk wegschrijven, cache legen
		String cacheKey = null;
		if(uriPatternSessionKey != null && !uriPatternSessionKey.isEmpty())
			cacheKey = getRestPath()+"_"+uriPatternSessionKey.toLowerCase();
		else
			cacheKey = getRestPath()+"_"+getUriPattern();
		if(cache != null && cache.containsKey(cacheKey)) {
			Object returnCode = false;

			if(getAction().equalsIgnoreCase("generate")) {
				try {
					cache.put(cacheKey, RestListenerUtils.formatEtag(getRestPath(), getUriPattern(), message.asString().hashCode()));
				} catch (IOException e) {
					throw new PipeRunException(this, getLogPrefix(session)+"cannot open stream", e);
				}
				returnCode = true;
			}
			else if(getAction().equalsIgnoreCase("get")) {
				returnCode = cache.get(cacheKey);
			}
			else if(getAction().equalsIgnoreCase("set")) {
				try {
					cache.put(cacheKey, message.asString());
				} catch (IOException e) {
					throw new PipeRunException(this, getLogPrefix(session)+"cannot open stream", e);
				}
				returnCode = true;
			}
			else if(getAction().equalsIgnoreCase("delete")) {
				returnCode = cache.remove(cacheKey);
			}
			else if(getAction().equalsIgnoreCase("flush")) {
				if(cache instanceof ApiEhcache) {
					((ApiEhcache) cache).flush();
					returnCode = true;
				}
			}
			else if(getAction().equalsIgnoreCase("clear")) {
				cache.clear();
				returnCode = true;
			}
			else {
				throw new PipeRunException(this, getLogPrefix(session)+"action not found ["+getAction()+"]"); 
			}
			if(log.isDebugEnabled()) log.debug("found eTag cacheKey ["+cacheKey+"] with action ["+getAction()+"]");

			return new PipeRunResult(getSuccessForward(), returnCode);
		}
		else {
			PipeForward pipeForward = findForward(PipeForward.EXCEPTION_FORWARD_NAME);
			String msg;

			if(cache == null)
				msg = "failed to locate cache";
			else
				msg = "failed to locate eTag ["+cacheKey+"] in cache";

			if (pipeForward==null) {
				throw new PipeRunException (this, getLogPrefix(session)+msg);
			}

			return new PipeRunResult(pipeForward, "");
		}
	}

	public void setAction(String string) {
		action = string;
	}

	public String getAction() {
		if(action != null)
			return action.toLowerCase();
		return null;
	}

	public void setUriPattern(String string) {
		uriPattern = string;
	}

	public String getUriPattern() {
		if(uriPattern != null)
			return uriPattern.toLowerCase();
		else {
			return null;
		}
	}

	public String getRestPath() {
		return restPath;
	}
	public void setRestPath(String string) {
		restPath = string;
	}

}
