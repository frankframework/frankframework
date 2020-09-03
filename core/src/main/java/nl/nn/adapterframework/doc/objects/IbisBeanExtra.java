package nl.nn.adapterframework.doc.objects;

import java.lang.reflect.Method;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IbisBeanExtra implements Comparable<IbisBeanExtra>{
	private IbisBean ibisBean;
	private Method[] sortedClassMethods;

	@Override
	public int compareTo(final IbisBeanExtra other) {
		return this.ibisBean.getName().compareTo(other.ibisBean.getName());
	}
}
