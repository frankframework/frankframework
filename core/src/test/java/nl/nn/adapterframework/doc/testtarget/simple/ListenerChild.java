package nl.nn.adapterframework.doc.testtarget.simple;

public class ListenerChild extends ListenerParent {
	public void setChildAttribute(String value) {
	}

	public String getChildAttribute() {
		return null;
	}

	public void setListener(IListener listener) {
	}

	public void invalidConfigChildSetterTwoArgs(IListener first, IListener second) {
	}

	public int invalidConfigChildSetterReturnsInt(IListener listener) {
		return 0;
	}

	public String invalidConfigChildSetterReturnsString(IListener listener) {
		return null;
	}
}
