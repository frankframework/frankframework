package nl.nn.adapterframework.align;

import org.apache.xerces.impl.dv.XSSimpleType;
import org.apache.xerces.xs.XSConstants;

public enum ScalarType {
	UNKNOWN,NUMERIC,BOOLEAN,STRING;
	
	public static ScalarType findType(XSSimpleType simpleType) {
		if (simpleType==null) {
			return UNKNOWN;
		}
		if (simpleType.getNumeric()) {
			return NUMERIC;
		}
		if (simpleType.getBuiltInKind()==XSConstants.BOOLEAN_DT) {
			return BOOLEAN;
		}
		return STRING;
	}
}
