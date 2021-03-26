package nl.nn.adapterframework.frankdoc.doclet;

abstract class FrankSimpleType implements FrankType {
	private final String name;

	FrankSimpleType(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean isPublic() {
		return true;
	}

	@Override
	public boolean isAnnotation() {
		return false;
	}

	@Override
	public FrankAnnotation[] getAnnotations() {
		return new FrankAnnotation[] {};
	}

	@Override
	public FrankAnnotation getAnnotation(String name) {
		return null;
	}

	@Override
	public boolean isEnum() {
		return false;
	}
}
