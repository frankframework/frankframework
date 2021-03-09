package nl.nn.adapterframework.doc.model;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import lombok.Getter;

public class AttributeValuesList {
	private @Getter String fullName;
	private @Getter List<String> values;

	AttributeValuesList(Class<? extends Enum<?>> clazz) {
		this.fullName = clazz.getName();
		this.values = Arrays.asList(clazz.getEnumConstants()).stream()
				.map(c -> c.name())
				.collect(Collectors.toList());
	}
}
