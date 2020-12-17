package nl.nn.adapterframework.doc.model;

import java.util.HashMap;
import java.util.Map;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.doc.Utils;

public class ElementRole {
	private final @Getter ElementType elementType;
	private final @Getter String syntax1Name;
	private final int syntax1NameSeq;
	private @Getter @Setter ElementRole founder;

	private ElementRole(ElementType elementType, String syntax1Name, int syntax1NameSeq) {
		this.elementType = elementType;
		this.syntax1Name = syntax1Name;
		this.syntax1NameSeq = syntax1NameSeq;
	}

	public String createXsdElementName(String kindDifferentiatingWord) {
		return Utils.toUpperCamelCase(syntax1Name) + kindDifferentiatingWord + disambiguation();
	}

	private String disambiguation() {
		if(syntax1NameSeq == 1) {
			return "";
		} else {
			return "_" + syntax1NameSeq;
		}
	}

	public Key getKey() {
		return new Key(elementType.getFullName(), syntax1Name);
	}

	static class Factory {
		private final Map<String, Integer> numUsagePerSyntax1Name = new HashMap<>();

		ElementRole create(ElementType elementType, String syntax1Name) {
			return new ElementRole(elementType, syntax1Name, newSyntax1NameSeq(syntax1Name));
		}

		private int newSyntax1NameSeq(String syntax1Name) {
			int maxExistingSyntax1NameSeq = numUsagePerSyntax1Name.getOrDefault(syntax1Name, 0);
			int syntax1NameSeq = maxExistingSyntax1NameSeq + 1;
			numUsagePerSyntax1Name.put(syntax1Name, syntax1NameSeq);
			return syntax1NameSeq;
		}
	}

	@EqualsAndHashCode
	public static class Key {
		private @Getter String elementTypeName;
		private @Getter String syntax1Name;

		public Key(String elementTypeName, String syntax1Name) {
			this.elementTypeName = elementTypeName;
			this.syntax1Name = syntax1Name;
		}

		public Key(ConfigChild configChild) {
			this(configChild.getElementType().getFullName(), configChild.getSyntax1Name());
		}

		@Override
		public String toString() {
			return "(" + elementTypeName + ", " + syntax1Name + ")"; 
		}
	}
}
