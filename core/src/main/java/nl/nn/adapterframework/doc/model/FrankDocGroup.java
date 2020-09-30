package nl.nn.adapterframework.doc.model;

import java.util.Map;

import lombok.Getter;
import lombok.Setter;

public class FrankDocGroup {
	private @Getter String name;
	private @Getter @Setter String digesterRulesRef;
	private @Getter @Setter Map<String, FrankElement> elements;

	public FrankDocGroup(String name) {
		this.name = name;
	}
}
