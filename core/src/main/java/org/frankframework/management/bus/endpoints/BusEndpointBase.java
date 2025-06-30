/*
   Copyright 2022-2024 WeAreFrank!

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
package org.frankframework.management.bus.endpoints;

import jakarta.annotation.Nonnull;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.http.MediaType;
import org.springframework.util.MimeType;

import lombok.extern.log4j.Log4j2;

import org.frankframework.configuration.Configuration;
import org.frankframework.configuration.IbisManager;
import org.frankframework.core.Adapter;
import org.frankframework.core.IPipe;
import org.frankframework.management.bus.BusException;
import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.receivers.Receiver;
import org.frankframework.util.LogUtil;
import org.frankframework.util.SpringUtils;

@Log4j2
public class BusEndpointBase implements ApplicationContextAware, InitializingBean {
	private final Logger secLog = LogUtil.getLogger("SEC");
	private ApplicationContext applicationContext;
	private IbisManager ibisManager;

	@Override
	public final void setApplicationContext(ApplicationContext ac) {
		this.applicationContext = ac;
	}

	protected final ApplicationContext getApplicationContext() {
		return applicationContext;
	}

	protected final IbisManager getIbisManager() {
		return ibisManager;
	}

	protected final <T> T createBean(Class<T> beanClass) {
		return SpringUtils.createBean(applicationContext, beanClass);
	}

	protected final <T> T getBean(String beanName, Class<T> beanClass) {
		return applicationContext.getBean(beanName, beanClass);
	}

	@Override
	public final void afterPropertiesSet() {
		if(applicationContext == null) {
			throw new BusException("ApplicationContext not set");
		}

		ibisManager = applicationContext.getBean("ibisManager", IbisManager.class);
		doAfterPropertiesSet();
	}

	protected void doAfterPropertiesSet() {
		// Override to initialize bean
	}

	protected final void log2SecurityLog(String message, String issuedBy) {
		String logMessage = StringUtils.isEmpty(issuedBy) ? message : message+" issued by "+issuedBy;
		log.info(logMessage);
		secLog.info(logMessage);
	}

	@Nonnull
	protected MimeType getMediaTypeFromName(String name) {
		String ext = FilenameUtils.getExtension(name);
		switch (ext) {
		case "xml":
			return MediaType.APPLICATION_XML;
		case "json":
			return MediaType.APPLICATION_JSON;
		default:
			return MediaType.TEXT_PLAIN;
		}
	}

	@Nonnull
	protected Configuration getConfigurationByName(String configurationName) {
		if(StringUtils.isEmpty(configurationName)) {
			throw new BusException("no configuration name specified");
		}
		Configuration configuration = getIbisManager().getConfiguration(configurationName);
		if(configuration == null) {
			throw new BusException("configuration [" + configurationName + "] does not exists", 404);
		}
		return configuration;
	}

	@Nonnull
	protected Adapter getAdapterByName(String configurationName, String adapterName) {
		if(StringUtils.isEmpty(adapterName)) {
			throw new BusException("no adapter name specified");
		}

		if(BusMessageUtils.ALL_CONFIGS_KEY.equals(configurationName)) {
			for (Configuration configuration : getIbisManager().getConfigurations()) {
				if(configuration.isActive()) {
					for (Adapter adapter : configuration.getRegisteredAdapters()) {
						if (adapterName.equals(adapter.getName())) {
							return adapter;
						}
					}
				}
			}
			throw new BusException("adapter not found");
		}

		Configuration config = getConfigurationByName(configurationName);
		Adapter adapter = config.getRegisteredAdapter(adapterName);

		if(adapter == null){
			throw new BusException("adapter [" + adapterName + "] does not exist", 404);
		}

		return adapter;
	}

	@Nonnull
	protected Receiver<?> getReceiverByName(Adapter adapter, String receiverName) {
		Receiver<?> receiver = adapter.getReceiverByName(receiverName);
		if(receiver == null) {
			throw new BusException("receiver [" + receiverName + "] does not exist", 404);
		}
		return receiver;
	}

	@Nonnull
	protected IPipe getPipeByName(Adapter adapter, String pipeName) {
		IPipe pipe = adapter.getPipeLine().getPipe(pipeName);
		if(pipe == null) {
			throw new BusException("pipe [" + pipeName + "] does not exist", 404);
		}
		return pipe;
	}
}
