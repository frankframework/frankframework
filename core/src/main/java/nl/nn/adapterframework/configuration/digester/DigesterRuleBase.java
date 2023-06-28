/*
   Copyright 2021, 2022 WeAreFrank!

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
package nl.nn.adapterframework.configuration.digester;

import java.beans.PropertyDescriptor;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.digester3.Rule;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXParseException;

import lombok.Setter;
import nl.nn.adapterframework.configuration.ApplicationWarnings;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.configuration.SuppressKeys;
import nl.nn.adapterframework.configuration.classloaders.IConfigurationClassLoader;
import nl.nn.adapterframework.core.CanShareResource;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.core.IbisException;
import nl.nn.adapterframework.core.ShareableResource;
import nl.nn.adapterframework.scheduler.job.IJob;
import nl.nn.adapterframework.scheduler.job.IbisActionJob;
import nl.nn.adapterframework.scheduler.job.Job;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.StringResolver;

/**
 * @author Niels Meijer
 */
public abstract class DigesterRuleBase extends Rule implements ApplicationContextAware {
	protected Logger log = LogUtil.getLogger(this);
	private @Setter ApplicationContext applicationContext;
	private @Setter ConfigurationWarnings configurationWarnings;
	private @Setter ApplicationWarnings applicationWarnings;
	private boolean preparse = AppConstants.getInstance().getBoolean("configurations.preparse", false);
	private boolean includeLineInformation = AppConstants.getInstance().getBoolean("configuration.warnings.linenumbers", preparse);//True when pre-parsed

	/**
	 * The current adapter-instance being parsed by the digester. This is needed for the configurable suppression of deprecation-warnings.
	 */
	private IAdapter currentAdapter = null;

	/**
	 * Returns the name of the object. In case a Spring proxy is being used,
	 * the name will be something like XsltPipe$$EnhancerBySpringCGLIB$$563e6b5d
	 * ClassUtils.getUserClass() makes sure the original class will be returned.
	 */
	protected String getObjectName() {
		return ClassUtils.nameOf(getBean());
	}

	/**
	 * Add a configuration warning message to the current configuration.
	 * Display location information conform {@link IbisException} when the cause is a {@link SAXParseException}.
	 */
	protected final void addLocalWarning(String msg) {
		configurationWarnings.add(getBean(), log, getLocationString() + msg);
	}

	/**
	 * Add a global message to the application
	 */
	protected final void addGlobalWarning(String message) {
		applicationWarnings.add(getBean(), log, message);
	}

	/**
	 * Add a warning message to the current configuration, unless the suppression key is
	 * supporessed in the configuration.
	 *
	 * @param msg Message to add
	 * @param suppressionKey {@link SuppressKeys} to check.
	 */
	protected final void addSuppressableWarning(String msg, SuppressKeys suppressionKey) {
		configurationWarnings.add(getBean(), log, getLocationString() + msg, suppressionKey, currentAdapter);
	}

	private String getLocationString() {
		if (!includeLineInformation) {
			return "";
		}
		Locator loc = getDigester().getDocumentLocator();
		return "on line ["+loc.getLineNumber()+"] column ["+loc.getColumnNumber()+"] ";

	}

	/**
	 * @return an {@link IConfigurationClassLoader}.
	 */
	protected final ClassLoader getClassLoader() {
		if(applicationContext == null) {
			throw new IllegalStateException("ApplicationContext not set");
		}

		return applicationContext.getClassLoader();
	}

	/**
	 * @return the currently handled object, aka TOP object
	 */
	protected Object getBean() {
		return getDigester().peek();
	}

	/**
	 * @return the resolved class of the current object
	 */
	protected final Class<?> getBeanClass() {
		return org.springframework.util.ClassUtils.getUserClass(getBean());
	}

	/**
	 * @param value or values to resolve
	 * @return The resolved value(s) using the configuration AppConstants
	 */
	protected final String resolveValue(String value) {
		String result = StringResolver.substVars(value, AppConstants.getInstance(getClassLoader()));
		log.trace("resolved [{}] to [{}] using ClassLoader [{}]", ()->value, ()->result, ()->getClassLoader());
		return result;
	}

	@Override
	public final void begin(String uri, String elementName, Attributes attributes) throws Exception {
		Object top = getBean();

		Map<String, String> map = copyAttrsToMap(attributes);
		if(top instanceof INamedObject) { //We must set the name first, to improve logging and configuration warnings
			String name = map.remove("name");
			if(StringUtils.isNotEmpty(name)) {
				ClassUtils.invokeSetter(top, "setName", name);
			}
		}

		if (top instanceof IAdapter) {
			currentAdapter = (IAdapter) top;
		}

		//Since we are directly instantiating the correct job (by className), functions are no longer required by the digester's attribute handler.
		//They are however still required for the JobFactory to determine the correct job class, in order to avoid ConfigurationWarnings.
		if(top instanceof IJob && !(top instanceof Job) && !(top instanceof IbisActionJob)) {
			map.remove("function");
		}

		handleBean();

		if(top instanceof CanShareResource && map.containsKey("sharedResourceName")) {
			String sharedResourceName = ShareableResource.SHARED_RESOURCE_PREFIX + map.get("sharedResourceName");
			if(applicationContext.containsBean(sharedResourceName)) {
				ShareableResource<?> container = applicationContext.getBean(sharedResourceName, ShareableResource.class);
				dontSetSharedResourceAttributes(container, map);
			} else {
				addLocalWarning("shared resource ["+map.get("sharedResourceName")+"] does not exist");
			}
		}

		for (Entry<String, String> entry : map.entrySet()) {
			String attribute = entry.getKey();
			if (log.isTraceEnabled()) {
				log.trace("checking attribute ["+attribute+"] on bean ["+getObjectName()+"]");
			}
			handleAttribute(attribute, entry.getValue(), map);
		}
	}

	/** Check if attribute-'map' contains attributes (methods) that also exist in 'sharedResource'. */
	private void dontSetSharedResourceAttributes(ShareableResource<?> sharedResource, Map<String, String> map) {
		PropertyDescriptor[] pds = BeanUtils.getPropertyDescriptors(sharedResource.getClass());
		for(PropertyDescriptor pd : pds) {
			String attributeName = pd.getName();
			if(map.containsKey(attributeName)) {
				addLocalWarning("ignoring attribute ["+attributeName+"] as it is managed by the shared resource ["+sharedResource.getName()+"]");
			}
		}
	}

	@Override
	public void end(String namespace, String name) throws Exception {
		if ("adapter".equalsIgnoreCase(name)) {
			currentAdapter = null;
		}
	}

	private Map<String, String> copyAttrsToMap(Attributes attrs) {
		Map<String, String> map = new LinkedHashMap<>(attrs.getLength());
		for (int i = 0; i < attrs.getLength(); ++i) {
			String name = attrs.getLocalName(i);
			if ("".equals(name)) {
				name = attrs.getQName(i);
			}
			if(name != null && !name.equals("className")) {
				String value = attrs.getValue(i);
				map.put(name, value);
			}
		}
		return map;
	}

	protected abstract void handleBean();

	protected abstract void handleAttribute(String name, String value, Map<String, String> attributes) throws Exception;
}
