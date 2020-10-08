package nl.nn.adapterframework.doc.model;

import java.util.HashMap;
import java.util.Map;

import lombok.Getter;

public class FrankDocGroup {
	private @Getter String name;
	private @Getter Map<String, FrankElement> elements;

	public FrankDocGroup(String name) {
		this.name = name;
		this.elements = new HashMap<>();
	}
}
