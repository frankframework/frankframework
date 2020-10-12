package nl.nn.adapterframework.configuration.digester;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.digester3.ObjectCreationFactory;
import org.apache.commons.lang3.StringUtils;

import nl.nn.adapterframework.util.ClassUtils;

@XmlRootElement(name = "rule")
public class DigesterRule {

	@XmlAttribute(name = "pattern")
	private String pattern;

	public String getPattern() {
		return pattern;
	}

	@XmlAttribute(name = "factory")
	private String factory;

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public ObjectCreationFactory<Object> getFactory() {
		if(StringUtils.isNotEmpty(factory)) {
			Object object;
			try {
				object = ClassUtils.newInstance(factory);
			} catch (Exception e) {
				throw new IllegalArgumentException("factory ["+factory+"] not found", e);
			}
			if(object instanceof ObjectCreationFactory) {
				return (ObjectCreationFactory) object;
			} else {
				throw new IllegalArgumentException("factory type must implement ObjectCreationFactory");
			}
		}
		return null;
	}

	@XmlAttribute(name = "object")
	private String objectClassName;

	public String getObject() {
		if(StringUtils.isNotEmpty(objectClassName)) {
			return objectClassName;
		}
		return null;
	}

	@XmlAttribute(name = "nextRule")
	private String nextRule;

	public String getNext() {
		return nextRule;
	}

	@XmlAttribute(name = "parameterType")
	private String parameterType;

	public Class<?> getParameterType() {
		if(StringUtils.isNotEmpty(parameterType)) {
			try {
				return ClassUtils.loadClass(parameterType);
			} catch (ClassNotFoundException e) {
				throw new IllegalArgumentException("class ["+parameterType+"] not found", e);
			}
		}
		return null;
	}
}