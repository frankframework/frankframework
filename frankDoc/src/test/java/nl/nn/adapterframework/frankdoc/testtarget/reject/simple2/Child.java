package nl.nn.adapterframework.frankdoc.testtarget.reject.simple2;

public class Child extends Parent implements IIgnored {
	@Override
	public void setAttributeIIgnored(String value) {
	}

	@Override
	public void setAttributeIKept(String value) {
	}

	@Override
	public void setAttributeParentKept(String value) {
	}

	@Override
	public void setAttributeIgnoreOverriddenByGrandParent() {
	}
}
