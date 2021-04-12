package nl.nn.adapterframework.configuration.digester;

import java.beans.PropertyDescriptor;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.digester3.Rule;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.springframework.util.ClassUtils;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;

import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.util.LogUtil;

public abstract class AbstractSpringPoweredDigesterRule extends Rule {
	protected Logger log = LogUtil.getLogger(this);

	/**
	 * Returns the name of the object. In case a Spring proxy is being used, 
	 * the name will be something like XsltPipe$$EnhancerBySpringCGLIB$$563e6b5d
	 * ClassUtils.getUserClass() makes sure the original class will be returned.
	 */
	private String getObjectName() {
		Object o = getBean();
		String result = ClassUtils.getUserClass(o).getSimpleName();
		if (o instanceof INamedObject) { //This assumes that setName has already been called
			String named = ((INamedObject) o).getName();
			if (StringUtils.isNotEmpty(named)) {
				return result+=" ["+named+"]";
			}
		}
		return result;
	}

	protected final void addWarning(String beanName, String message) {
		Locator loc = getDigester().getDocumentLocator();
		String msg = getObjectName()+ " on line "+loc.getLineNumber()+", col "+loc.getColumnNumber()+": "+message;
		System.out.println(msg);
	}

	protected final Object getBean() {
		return getDigester().peek();
	}

	protected final Class<?> getBeanClass() {
		return ClassUtils.getUserClass(getBean());
	}

	@Override
	public final void begin(String uri, String elementName, Attributes attributes) throws Exception {
		Object top = getBean();

		handleBean(elementName, top);

		Map<String, String> map = copyAttrsToMap(attributes);
		map.get("name")//TODO
		for (String attribute : map.keySet()) {
			if(!attribute.equals("name")) { //We must set the name first, to improve logging and configuration warnings
				if (log.isTraceEnabled()) {
					log.trace("checking attribute ["+attribute+"] on bean ["+getObjectName()+"]");
				}
				handleAttribute(attribute, map.get(attribute), map);
			}
		}
	}

	protected Map<String, String> copyAttrsToMap(Attributes attrs) {
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

	/**
	 * @param beanName
	 * @param top
	 */
	protected abstract void handleBean(String beanName, Object bean);

	/**
	 * @param pd may be null
	 */
	protected abstract void handleAttribute(String name, String value, Map<String, String> attributes) throws Exception;
}
