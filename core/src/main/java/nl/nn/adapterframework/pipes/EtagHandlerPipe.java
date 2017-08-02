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
package nl.nn.adapterframework.pipes;

import java.util.Arrays;
import java.util.List;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.http.RestEtagCache;
import nl.nn.adapterframework.http.RestListenerUtils;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.parameters.ParameterValueList;

public class EtagHandlerPipe extends FixedForwardPipe {
	private String action="";
//	hash over data genereren, uit cache lezen en teruggeven, in cache updaten, verwijderen uit cache, cache naar disk wegschrijven, cache legen
	List<String> actions = Arrays.asList("generate", "get", "set", "delete", "flush", "clear");
	private String restPath = "/rest";
	private String uriPattern = null;
	private RestEtagCache cache = null;

	public void configure() throws ConfigurationException {
		super.configure();
		String action = getAction();
		if (action==null) {
			throw new ConfigurationException(getLogPrefix(null)+"action must be set");
		}
		if (!actions.contains(action)) {
			throw new ConfigurationException(getLogPrefix(null)+"illegal value for action ["+action+"], must be one of " + actions.toString());
		}

		boolean hasUriPatternParameter = false;
		ParameterList<Parameter> parameterList = getParameterList();
		for (int i=0; i<parameterList.size(); i++) {
			Parameter parameter = parameterList.getParameter(i);
			if("uriPattern".equalsIgnoreCase(parameter.getName()))
				hasUriPatternParameter = true;
		}

		if(getUriPattern() == null && !hasUriPatternParameter) {
			throw new ConfigurationException(getLogPrefix(null)+"no uriPattern found!");
		}
		cache = new RestEtagCache();
	}

	public PipeRunResult doPipe(Object input, IPipeLineSession session) throws PipeRunException {
		if (input==null) {
			throw new PipeRunException(this, getLogPrefix(session)+"got null input");
		}
		if (!(input instanceof String)) {
			throw new PipeRunException(this, getLogPrefix(session)+"got an invalid type as input, expected String, got "+ input.getClass().getName());
		}

		String uriPatternSessionKey = null;
		ParameterValueList pvl = null;
		ParameterList<Parameter> parameterList = getParameterList();
		if (parameterList != null) {
			ParameterResolutionContext prc = new ParameterResolutionContext((String) input, session);
			try {
				pvl = prc.getValues(getParameterList());
				if (pvl != null) {
					for (int i = 0; i < parameterList.size(); i++) {
						Parameter parameter = parameterList.getParameter(i);
						if("uriPattern".equalsIgnoreCase(parameter.getName()))
							uriPatternSessionKey = (String) parameter.getValue(pvl, prc);
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
				cache.put(cacheKey, RestListenerUtils.formatEtag(getRestPath(), getUriPattern(), input.hashCode()));
				returnCode = true;
			}
			else if(getAction().equalsIgnoreCase("get")) {
				returnCode = cache.get(cacheKey);
			}
			else if(getAction().equalsIgnoreCase("set")) {
				cache.put(cacheKey, input.toString());
				returnCode = true;
			}
			else if(getAction().equalsIgnoreCase("delete")) {
				returnCode = cache.remove(cacheKey);
			}
			else if(getAction().equalsIgnoreCase("flush")) {
				cache.flush();
				returnCode = true;
			}
			else if(getAction().equalsIgnoreCase("clear")) {
				cache.removeAll();
				returnCode = true;
			}
			else {
				throw new PipeRunException(this, getLogPrefix(session)+"action not found ["+getAction()+"]"); 
			}
			if(log.isDebugEnabled()) log.debug("found eTag cacheKey ["+cacheKey+"] with action ["+getAction()+"]");

			return new PipeRunResult(getForward(), returnCode);
		}
		else {
			PipeForward pipeForward = findForward("exception");
			if (pipeForward==null) {
				throw new PipeRunException (this, getLogPrefix(session)+"failed to locate eTag ["+cacheKey+"] in cache");
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
