package nl.nn.adapterframework.frankdoc.testtarget.textconfig.plural;

public class Start {
	/*
	 * This is a plural config child. We want to cover the DocWriterNew code
	 * for plural config children.
	 */
	public void registerA(IChild1 child) {
	}

	public void registerA(IChild2 child) {
	}

	// This TextConfigChild is parsed using the DocWriterNew code for plural config children.
	public void addP(String value) {
	}
}
