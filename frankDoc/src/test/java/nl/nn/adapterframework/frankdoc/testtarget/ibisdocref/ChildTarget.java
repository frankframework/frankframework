package nl.nn.adapterframework.frankdoc.testtarget.ibisdocref;

import nl.nn.adapterframework.doc.IbisDoc;

public class ChildTarget extends ParentTarget {
	@IbisDoc("Description of ibisDocRefClassNoOrderRefersIbisDocOrderDescriptionDefault")
	public void setIbisDocRefClassNoOrderRefersIbisDocOrderDescriptionDefault(String value) {
	}

	@IbisDoc("Description of otherMethod")
	public void otherMethod(String value) {
	}

	/**
	 * IbisDocRef'd JavaDoc of setAttributeWithIbisDocRefReferringJavadoc
	 * @param value
	 */
	public void setAttributeWithIbisDocRefReferringJavadoc(String value) {
	}

	/**
	 * This JavaDoc is considered old because we have @IbisDoc.
	 * @param value
	 */
	@IbisDoc({"100", "IbisDoc description of setAttributeWithIbisDocRefThatGivesPreferenceToIbisDocDescriptionOverJavadoc"})
	public void setAttributeWithIbisDocRefThatGivesPreferenceToIbisDocDescriptionOverJavadoc(String value) {
	}

	/**
	 * This Javadoc is the description, because the IbisDoc annotation lacks a description.
	 */
	@IbisDoc("200")
	public void setAttributeWithIbisDocRefReferringIbisDocWithoutDescriptionButWithJavadoc(String value) {
	}

	/**
	 * @ff.default setIbisDocRefRefersJavaDocDefault default value
	 */
	public void setIbisDocRefRefersJavaDocDefault(String value) {
	}
}
