package nl.nn.adapterframework.frankdoc.testtarget.reject.simple;

public class Parent implements ISuperseeded {
	@Override
	public void setRejectedAttribute(String value) {
	}

	@Override
	public void setRejectionOverruledAttribute(String value) {
	}

	public void setKeptInheritedAttribute(String value) {
	}
}
