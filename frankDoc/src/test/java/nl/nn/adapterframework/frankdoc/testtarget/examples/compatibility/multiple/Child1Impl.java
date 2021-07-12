package nl.nn.adapterframework.frankdoc.testtarget.examples.compatibility.multiple;

public class Child1Impl implements IChild1 {
	public void registerB(IGrandChild1 child) {
	}
	
	public void registerB(IGrandChild2 child) {
	}

	public void registerD(GrandChild3 child) {
	}

	public void setMyThirdAttribute(int value) {
	}

	public void setMyFourthAttribute(boolean value) {
	}
}
