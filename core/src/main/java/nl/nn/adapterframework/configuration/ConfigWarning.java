package nl.nn.adapterframework.configuration;

import java.util.LinkedList;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import lombok.Setter;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;

public class ConfigWarning implements ApplicationContextAware, InitializingBean {
	private @Setter ApplicationContext applicationContext;
	private AppConstants appConstants;
	private LinkedList<String> warnings;

	@Override
	public void afterPropertiesSet() throws Exception {
		appConstants = AppConstants.getInstance(applicationContext.getClassLoader());
		warnings = new LinkedList<>();
	}

	/**
	 * Add configuration warning with INamedObject prefix
	 */
	public void add(INamedObject source, String message) {
		add(source, message, null);
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

//	2021-04-04 12:38:19,661 WARN  [localhost-startStop-1] null http.HttpSender - attribute [paramsInUrl] is deprecated: no longer required when using FORMDATA or MTOM requests
//	2021-04-04 12:38:19,661 WARN  [localhost-startStop-1] null http.HttpSender - HttpSender [null] attribute [paramsInUrl] is deprecated: no longer required when using FORMDATA or MTOM requestsThis warning can be suppressed globally by setting the property 'warnings.suppress.deprecated=true'

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
		if (t == null) {
			log.warn(logMsg);
		} else {
			log.warn(logMsg, t);
		}
		boolean onlyOnce = (t==null);
		if (!onlyOnce || !warnings.contains(msg)) {
			warnings.add(msg);
		}
	}

	public boolean isSuppressed(SuppressKeys key, IAdapter adapter) {
		if(key == null) {
			return false;
		}

		return key.isAllowGlobalSuppression() && appConstants.getBoolean(key.getKey(), false) // warning is suppressed globally, for all adapters
				|| adapter!=null && appConstants.getBoolean(key.getKey()+"."+adapter.getName(), false); // or warning is suppressed for this adapter only.
	}
}
