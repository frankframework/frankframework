package nl.nn.adapterframework.frankdoc.testtarget.technical.override;

public class ChildTechnicalOverride extends ParentTechnicalOverride {
	@Override
	public void setChild(Master child) {
	}

	/**
	 * Non-overridden config child
	 */
	public void setChild(ChildTechnicalOverride child) {
	}
}
