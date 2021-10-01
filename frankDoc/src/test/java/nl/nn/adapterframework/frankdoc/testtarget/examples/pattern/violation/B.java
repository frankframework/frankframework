package nl.nn.adapterframework.frankdoc.testtarget.examples.pattern.violation;

public class B {
	// Config child that satisfies patter */b/c.
	public void registerC(C child) {
	}

	// Violating config child, violates pattern */c/d.
	public void registerD(D child) {
	}

	// Text config child.
	public void registerI(String text) {
	}
}
