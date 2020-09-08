package nl.nn.adapterframework.doc.objects;

import java.lang.reflect.Method;
import java.util.TreeSet;

import lombok.Getter;
import lombok.Setter;

public class MethodExtra {
	private @Getter @Setter Method method;
	private @Getter @Setter String childIbisBeanName;
	private @Getter @Setter TreeSet<IbisBean> childIbisBeans;
	private @Getter @Setter boolean isExistingIbisBean;
	private @Getter @Setter int maxOccurs;
}
