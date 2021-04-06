package nl.nn.adapterframework.configuration;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;

import lombok.Setter;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.core.IConfigurable;
import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.http.HttpSender;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;

public class ConfigWarning implements ApplicationEventPublisherAware, ApplicationContextAware, InitializingBean {
	private ApplicationEventPublisher applicationEventPublisher;
	private @Setter ApplicationContext applicationContext;
	private AppConstants appConstants;

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		appConstants = AppConstants.getInstance(applicationContext.getClassLoader());
	}

	public void add(INamedObject source, String message) {
		add(source, message, null);
//		ApplicationEvent event = null;
//		applicationEventPublisher.publishEvent(event);
	}

//	2021-04-04 12:38:19,661 WARN  [localhost-startStop-1] null http.HttpSender - attribute [paramsInUrl] is deprecated: no longer required when using FORMDATA or MTOM requests
//	2021-04-04 12:38:19,661 WARN  [localhost-startStop-1] null http.HttpSender - HttpSender [null] attribute [paramsInUrl] is deprecated: no longer required when using FORMDATA or MTOM requestsThis warning can be suppressed globally by setting the property 'warnings.suppress.deprecated=true'

	public void add(INamedObject source, String message, SuppressKeys deprecationSuppressKey) {
		String msg = (source==null?"":ClassUtils.nameOf(source) +" ["+source.getName()+"]")+" "+message;
		System.out.println(source + " - "+ msg);
		Logger log = LogUtil.getLogger(source);
		log.warn(msg);
	}

	private void add(INamedObject object, String message, Logger log, SuppressKeys suppressionKey, IAdapter adapter) {
		if(!isSuppressed(suppressionKey, adapter)) {
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
//			add(object, log, message, null, hint);
		}
	}

	private static void add(IConfigurable object, Logger log, String message, Throwable t, String messageSuffixForLog) {
		
	}


	public boolean isSuppressed(SuppressKeys key, IAdapter adapter) {
		if(key == null) {
			return false;
		}

		return key.isAllowGlobalSuppression() && appConstants.getBoolean(key.getKey(), false) // warning is suppressed globally, for all adapters
				|| adapter!=null && appConstants.getBoolean(key.getKey()+"."+adapter.getName(), false); // or warning is suppressed for this adapter only.
	}
}
