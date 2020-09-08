package nl.nn.adapterframework.doc.objects;

import java.lang.reflect.Method;

import lombok.Getter;
import lombok.Setter;

public class BeanProperty {
	private @Getter @Setter String name;
	private @Getter @Setter Method method;
	private @Getter @Setter boolean isExcluded;
	private @Getter @Setter boolean hasDocumentation;
	private @Getter @Setter String description;
	private @Getter @Setter String defaultValue;
	private @Getter @Setter int order;
}
