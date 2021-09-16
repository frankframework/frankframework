package nl.nn.adapterframework.frankdoc.testtarget.examples.pattern.violation;

public class D {
	// Violating config child. Satisfies */d/e, but d does not count because it violates.
	public void registerE(E child) {
	}
}
