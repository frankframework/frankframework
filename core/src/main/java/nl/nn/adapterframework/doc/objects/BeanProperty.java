package nl.nn.adapterframework.doc.objects;

import java.lang.reflect.Method;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BeanProperty {
	String name;
	Method method;
	boolean isExcluded;
	boolean hasDocumentation;
	String description;
	String defaultValue;
	int order;
}
