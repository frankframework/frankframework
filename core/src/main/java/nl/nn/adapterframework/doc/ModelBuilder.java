package nl.nn.adapterframework.doc;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import lombok.Getter;
import nl.nn.adapterframework.doc.model.FrankDocGroup;
import nl.nn.adapterframework.doc.model.FrankDocModel;
import nl.nn.adapterframework.doc.objects.SpringBean;

public class ModelBuilder {
	private @Getter FrankDocModel model;

	public ModelBuilder() {
		model = new FrankDocModel();
	}

	/**
	 * @param interfaceName The interface for which we want SpringBean objects.
	 * @return All classes implementing interfaceName, ordered by their full class name.
	 */
	static List<SpringBean> getSpringBeans(final String interfaceName) {
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

	void addElementsToGroup(Class<?> clazz, FrankDocGroup group) {
		group.getElements().put(clazz.getName(), model.findOrCreateFrankElement(clazz));
	}

	public static Class<?> getClass(final String name) {
		return InfoBuilderSource.getClass(name);
	}
}
