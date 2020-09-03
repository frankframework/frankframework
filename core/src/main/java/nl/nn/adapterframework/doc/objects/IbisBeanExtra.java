package nl.nn.adapterframework.doc.objects;

import java.util.Map;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IbisBeanExtra implements Comparable<IbisBeanExtra>{
	private IbisBean ibisBean;
	private MethodExtra[] sortedClassMethods;
	private Map<String, BeanProperty> properties;

	@Override
	public int compareTo(final IbisBeanExtra other) {
		return this.ibisBean.getName().compareTo(other.ibisBean.getName());
	}
}
