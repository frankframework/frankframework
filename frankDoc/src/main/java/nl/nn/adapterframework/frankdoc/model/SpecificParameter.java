package nl.nn.adapterframework.frankdoc.model;

import lombok.Getter;

public class SpecificParameter {
	private final @Getter String name;
	private final @Getter String description;

	static SpecificParameter getInstance(String javaDocTagParameter) {
		int idx = javaDocTagParameter.indexOf(" ");
		if(idx < 0) {
			return new SpecificParameter(javaDocTagParameter, null);
		}
		String name = javaDocTagParameter.substring(0, idx).trim();
		String description = javaDocTagParameter.substring(idx).trim();
		if(description.isEmpty()) {
			return new SpecificParameter(name, null);
		}
		return new SpecificParameter(name, description);
	}

	private SpecificParameter(String name, String description) {
		this.name = name;
		this.description = description;
	}
}
