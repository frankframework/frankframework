package nl.nn.adapterframework.doc.objects;

import java.util.Map;

import lombok.Getter;
import lombok.Setter;

public class IbisBeanExtra implements Comparable<IbisBeanExtra>{
	private @Getter @Setter IbisBean ibisBean;
	private @Getter @Setter MethodExtra[] sortedClassMethods;
	private @Getter @Setter Map<String, BeanProperty> properties;

	@Override
	public int compareTo(final IbisBeanExtra other) {
		return this.ibisBean.getName().compareTo(other.ibisBean.getName());
	}
}
