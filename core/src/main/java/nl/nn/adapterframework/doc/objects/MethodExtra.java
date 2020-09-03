package nl.nn.adapterframework.doc.objects;

import java.lang.reflect.Method;
import java.util.TreeSet;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MethodExtra {
	private Method method;
	private IbisMethod ibisMethod;
	private String childIbisBeanName;
	private TreeSet<IbisBean> childIbisBeans;
	private boolean isExistingIbisBean;
	private int maxOccurs;
}
