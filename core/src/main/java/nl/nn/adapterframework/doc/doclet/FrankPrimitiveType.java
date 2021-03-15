package nl.nn.adapterframework.doc.doclet;

public class FrankPrimitiveType implements FrankType {
	private final String name;

	FrankPrimitiveType(String name) {
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
	public boolean isPrimitive() {
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
}
