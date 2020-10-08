package nl.nn.adapterframework.doc.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

public class FrankDocModel implements FrankElement.FrankElementStore {
	private @Getter @Setter List<FrankDocGroup> groups;
	private @Getter Map<String, FrankElement> allElements = new HashMap<>();

	public FrankElement frankElement(Class<?> clazz) {
		return FrankElement.frankElement(clazz, this);
	}

	@Override
	public boolean hasFrankElement(String name) {
		return allElements.containsKey(name);
	}

	@Override
	public FrankElement getFrankElement(String name) {
		return allElements.get(name);
	}

	@Override
	public void addFrankElement(FrankElement frankElement) {
		allElements.put(frankElement.getFullName(), frankElement);
	}

	@Override
	public int numFrankElements() {
		return allElements.size();
	}
}
