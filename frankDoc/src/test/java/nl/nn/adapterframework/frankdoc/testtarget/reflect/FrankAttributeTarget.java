package nl.nn.adapterframework.frankdoc.testtarget.reflect;

import java.util.List;

import nl.nn.adapterframework.doc.IbisDoc;

public class FrankAttributeTarget extends FrankAttributeTargetParent {
	public void setAttributeSetterGetter(String value) {
	}

	public String getAttributeSetterGetter() {
		return null;
	}

	public void setAttributeSetterIs(boolean value) {
	}

	public boolean isAttributeSetterIs() {
		return true;
	}

	public void setAttributeOnlySetter(String value) {
	}

	public void setNonAttributeVararg(String ...value) {
	}

	public void setAttributeOnlySetterInt(int value) {
	}

	public void setAttributeOnlySetterIntBoxed(Integer value) {
	}

	public void setAttributeOnlySetterBoolBoxed(Boolean value) {
	}

	public void setAttributeOnlySetterLongBoxed(Long value) {
	}

	public void setAttributeOnlySetterByteBoxed(Byte value) {
	}

	public void setAttributeOnlySetterShortBoxed(Short value) {
	}

	public void setNoAttributeComplexType(List<String> value) {
	}

	public List<String> getNoAttributeComplexType() {
		return null;
	}

	public String prefix() {
		return null;
	}

	public String get() {
		return null;
	}

	public void setInvalidSetter(String s, int i) {
	}

	public void setInvalidSetterNoParams() {
	}

	@IbisDoc("Description of ibisDockedOnlyDescription")
	public void setIbisDockedOnlyDescription(String value) {
	}

	@IbisDoc({"3", "Description of ibisDockedOrderDescription"})
	public void setIbisDockedOrderDescription(String value) {
	}

	@IbisDoc({"Description of ibisDockedDescriptionDefault", "Default of ibisDockedDescriptionDefault"})
	public void setIbisDockedDescriptionDefault(String value) {
	}

	@IbisDoc({"5", "Description of ibisDockedOrderDescriptionDefault", "Default of ibisDockedOrderDescriptionDefault"})
	public void setIbisDockedOrderDescriptionDefault(String value) {
	}

	@Deprecated
	@IbisDoc("Description of ibisDockedDeprecated")
	public void setIbisDockedDeprecated(String value) {
	}
}
