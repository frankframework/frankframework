package nl.nn.adapterframework.frankdoc.model;

import java.util.List;

import lombok.Getter;

public class FrankDocGroup2 {
	private final @Getter String name;
	private final @Getter List<FrankElement> members;

	FrankDocGroup2(String name, List<FrankElement> members) {
		this.name = name;
		this.members = members;
	}
}
