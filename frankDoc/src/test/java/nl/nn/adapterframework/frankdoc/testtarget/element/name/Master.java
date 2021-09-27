package nl.nn.adapterframework.frankdoc.testtarget.element.name;

public class Master {
	// Makes IFirst into a common interface, which means it has a corresponding ElementType.
	public void registerA(IFirst child) {
	}

	// The interface has subclasses Child1First, Child2Second and Child3Third. IThird and IFirst
	// are common interfaces, but not ISecond. Therefore the element names become
	// Child1B, Child2SecondB and Child3B.
	public void registerB(IThird child) {
	}
}
