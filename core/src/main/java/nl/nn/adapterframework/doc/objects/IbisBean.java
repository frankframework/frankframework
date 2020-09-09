package nl.nn.adapterframework.doc.objects;

import java.util.Map;

import lombok.Getter;
import lombok.Setter;

public class IbisBean implements Comparable<IbisBean>{
    private @Getter String name;
    private @Getter Class<?> clazz;
	private @Getter @Setter MethodExtra[] sortedClassMethods;
	private @Getter @Setter Map<String, BeanProperty> properties;

    public IbisBean(String name, Class<?> clazz) {
        this.name = name;
        this.clazz = clazz;
    }

	@Override
	public int compareTo(final IbisBean other) {
		return this.getName().compareTo(other.getName());
	}
}
