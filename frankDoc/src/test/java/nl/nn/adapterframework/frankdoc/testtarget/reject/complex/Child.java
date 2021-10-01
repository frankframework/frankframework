package nl.nn.adapterframework.frankdoc.testtarget.reject.complex;

public class Child implements ISuperseded, IJoin, INew2 {
	// Produces attribute because it is in INew1
	@Override
	public void setSuperseded1(String value) {
	}

	// Produces attribute because it is in INew1
	@Override
	public void setSuperseded2(String value) {
	}

	// Does not produce attribute because it is in ISuperseded. It
	// is also in IJoin, but that does not count because IJoin extends
	// ISuperseded.
	@Override
	public void setSuperseded3(String value) {
	}

	// Produces attribute because it is in INew2
	@Override
	public void setSuperseded4(String value) {
	}
}
