/*
Copyright 2021 WeAreFrank!

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
package nl.nn.adapterframework.configuration;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;

import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;

public class ConfigWarning extends ApplicationWarnings {

	/**
	 * Add configuration warning with INamedObject prefix
	 */
	public void add(INamedObject source, String message) {
		add(source, message, (Throwable) null);
	}

	public static void add(ApplicationContext configuration, INamedObject source, String message) {
		if(configuration != null) {
			ConfigWarning cw = configuration.getBean("configurationWarnings", ConfigWarning.class);
			cw.add(source, message);
		} else {
			throw new IllegalStateException("cannot add INamedObject warning without ApplicationContext");
		} 
	}

	/**
	 * Add configuration warning with INamedObject prefix and log the exception stack
	 */
	public void add(INamedObject source, String message, Throwable t) {
		addWithNamedObjectPrefix(source, getLogger(source), message, null, t);
	}

	private Logger getLogger(INamedObject source) {
		return LogUtil.getLogger(source); //HashTable key lookup
	}

	public void add(INamedObject source, String message, SuppressKeys suppressionKey) {
		add(source, message, suppressionKey, null);
	}

	public void add(INamedObject source, String message, SuppressKeys suppressionKey, IAdapter adapter) {
		if(!isSuppressed(suppressionKey, adapter)) {
			Logger log = getLogger(source);
			// provide suppression hint as info 
			String hint = null;
			if(log.isInfoEnabled()) {
				if(adapter != null) {
					hint = ". This warning can be suppressed by setting the property '"+suppressionKey.getKey()+"."+adapter.getName()+"=true'";
					if(suppressionKey.isAllowGlobalSuppression()) {
						hint += ", or globally by setting the property '"+suppressionKey.getKey()+"=true'";
					}
				} else if(suppressionKey.isAllowGlobalSuppression()) {
					hint = ". This warning can be suppressed globally by setting the property '"+suppressionKey.getKey()+"=true'";
				}
			}
			addWithNamedObjectPrefix(source, log, message, hint, null);
		}
	}

	private void addWithNamedObjectPrefix(INamedObject source, Logger log, String message, String messageSuffixForLog, Throwable t) {
		String msg = (source==null?"":ClassUtils.nameOf(source) +" ["+source.getName()+"]")+" "+message;
		doAdd(log, msg, messageSuffixForLog, t);
	}

	private void doAdd(Logger log, String msg, String logHint, Throwable t) {
		String logMsg = StringUtils.isNotEmpty(logHint) ? msg + logHint : msg;
		doAdd(log, logMsg, t);
	}

	public boolean isSuppressed(SuppressKeys key, IAdapter adapter) {
		if(key == null) {
			return false;
		}

		return key.isAllowGlobalSuppression() && getAppConstants().getBoolean(key.getKey(), false) // warning is suppressed globally, for all adapters
				|| adapter!=null && getAppConstants().getBoolean(key.getKey()+"."+adapter.getName(), false); // or warning is suppressed for this adapter only.
	}
}
