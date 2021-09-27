package nl.nn.adapterframework.frankdoc.testtarget.reject.complex2;

public class Child implements INewExtended, ISuperseded {
	// Attribute because in INew
	@Override
	public void setSuperseded1(String value) {
	}

	// Attribute because in INewExtended
	@Override
	public void setSuperseded2(String value) {
	}

	// Not an attribute because in ISuperseded
	@Override
	public void setSuperseded3(String value) {
	}
}
