package nl.nn.adapterframework.testtool.queues;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Properties;

import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.configuration.ClassLoaderException;
import nl.nn.adapterframework.receivers.JavaListener;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;

public class QueueUtils {
	private static final Logger LOG = LogUtil.getLogger(QueueUtils.class);

	public static <T extends IQueue> T createInstance(Class<T> clazz) throws Exception {
		return (T) createInstance(clazz.getCanonicalName());
	}

	public static IQueue createInstance(String className) throws Exception {
		LOG.debug("instantiating queue [{}]", className);
		try {
			Class<?> clazz = ClassUtils.loadClass(className);
			Constructor<?> con = ClassUtils.getConstructorOnType(clazz, new Class[] {});
			return (IQueue) con.newInstance();
		}
		catch (ClassCastException e) {
			throw new Exception("Queue ["+className+"] does not implement IQueue", e);
		}
		catch (Exception e) {
			throw new Exception("error initializing Queue ["+className+"]", e);
		}
	}

	public static Properties getSubProperties(Properties properties, String keyBase) {
		if(!keyBase.endsWith("."))
			keyBase +=".";

		Properties filteredProperties = new Properties();
		for(Object objKey: properties.keySet()) {
			String key = (String) objKey;
			if(key.startsWith(keyBase)) {
				filteredProperties.put(key.substring(keyBase.length()), properties.get(key));
			}
		}

		return filteredProperties;
	}

	public static void invokeSetters(IQueue queue, Properties queueProperties) throws Exception {
		for(Method method: queue.getClass().getMethods()) {
			if(!method.getName().startsWith("set") || method.getParameterTypes().length != 1)
				continue;

			String setter = firstCharToLower(method.getName().substring(3));
			String value = queueProperties.getProperty(setter);
			if(value == null)
				continue;

			//Only always grab the first value because we explicitly check method.getParameterTypes().length != 1
			Object castValue = getCastValue(method.getParameterTypes()[0], value);
			LOG.debug("trying to set property ["+setter+"] with value ["+value+"] of type ["+castValue.getClass().getCanonicalName()+"] on ["+ClassUtils.nameOf(queue)+"]");

			try {
				method.invoke(queue, castValue);
			} catch (Exception e) {
				throw new Exception("error while calling method ["+setter+"] on classloader ["+ClassUtils.nameOf(queue)+"]", e);
			}
		}
	}

	private static Object getCastValue(Class<?> class1, String value) {
		String className = class1.getName().toLowerCase();
		if("boolean".equals(className))
			return Boolean.parseBoolean(value);
		else if("int".equals(className) || "integer".equals(className))
			return Integer.parseInt(value);
		else
			return value;
	}

	private static String firstCharToLower(String input) {
		return input.substring(0, 1).toLowerCase() + input.substring(1);
	}

}
