package nl.nn.adapterframework.doc;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import nl.nn.adapterframework.doc.objects.SpringBean;

public final class Utils {
	private Utils() {
	}

	/**
	 * @param interfaceName The interface for which we want SpringBean objects.
	 * @return All classes implementing interfaceName, ordered by their full class name.
	 */
	public static List<SpringBean> getSpringBeans(final String interfaceName) {
		Class<?> interfaze = getClass(interfaceName);
		if(interfaze == null) {
			throw new NullPointerException("Class or interface is not available on the classpath: " + interfaceName);
		}
		if(!interfaze.isInterface()) {
			throw new IllegalArgumentException("This exists on the classpath but is not an interface: " + interfaceName);
		}
		Set<SpringBean> unfiltered = InfoBuilderSource.getSpringBeans(interfaze);
		List<SpringBean> result = new ArrayList<SpringBean>();
		for(SpringBean b: unfiltered) {
			if(interfaze.isAssignableFrom(b.getClazz())) {
				result.add(b);
			}
		}
		return result;
	}

	public static Class<?> getClass(final String name) {
		return InfoBuilderSource.getClass(name);
	}

	public static boolean isAttributeGetterOrSetter(Method method) {
		return InfoBuilderSource.isGetterOrSetter(method);
	}

	public static boolean isConfigChildSetter(Method method) {
		return InfoBuilderSource.isConfigChildSetter(method);
	}
}
