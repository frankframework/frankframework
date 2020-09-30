package nl.nn.adapterframework.doc;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.Getter;
import nl.nn.adapterframework.doc.model.FrankDocModel;
import nl.nn.adapterframework.doc.objects.SpringBean;

public class ModelBuilder {
	private @Getter FrankDocModel model;

	static class AttributeSeed {
		private @Getter String name;

		AttributeSeed(Method reflectMethod) {
			name = reflectMethod.getName();
		}
	}

	static class ElementSeed {
		private @Getter Map<String, AttributeSeed> methods;
		private @Getter Map<String, AttributeSeed> methodsWithInherited;

		ElementSeed(Class<?> clazz) {
			methods = new HashMap<>();
			for(Method reflect: clazz.getDeclaredMethods()) {
				methods.put(reflect.getName(), new AttributeSeed(reflect));
			}
			methodsWithInherited = new HashMap<>();
			methodsWithInherited.putAll(methods);
			for(Method reflect: clazz.getMethods()) {
				methodsWithInherited.putIfAbsent(reflect.getName(), new AttributeSeed(reflect));
			}
		}
	}

	public ModelBuilder() {
		model = new FrankDocModel();
		model.setGroups(new ArrayList<>());
		model.setAllElements(new HashMap<>());
	}

	/**
	 * @param interfaceName The interface for which we want SpringBean objects.
	 * @return All classes implementing interfaceName, ordered by their full class name.
	 */
	static List<SpringBean> getSpringBeans(final String interfaceName) {
		Class<?> interfaze = InfoBuilderSource.getClass(interfaceName);
		if(interfaze == null) {
			throw new NullPointerException();
		}
		if(!interfaze.isInterface()) {
			throw new IllegalArgumentException("Only retrieve Spring beans from an interface");
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
}
