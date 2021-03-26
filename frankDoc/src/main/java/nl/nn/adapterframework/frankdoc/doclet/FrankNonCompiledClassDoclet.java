package nl.nn.adapterframework.frankdoc.doclet;

class FrankNonCompiledClassDoclet extends FrankSimpleType {
	FrankNonCompiledClassDoclet(String name) {
		super(name);
	}

	@Override
	public boolean isPrimitive() {
		return false;
	}
}
