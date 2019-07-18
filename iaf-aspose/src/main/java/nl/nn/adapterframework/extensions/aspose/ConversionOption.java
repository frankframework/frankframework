package nl.nn.adapterframework.extensions.aspose;

public enum ConversionOption {
	SINGLEPDF(0), SEPERATEPDF(1);

	private final int value;

	private ConversionOption(int value) {
		this.value = value;
	}

	public int getValue() {
		return this.value;
	}
}
