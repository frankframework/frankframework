package nl.nn.adapterframework.util;

import org.xml.sax.Attributes;

public class NamespaceRemovingAttributesWrapper extends AttributesWrapper {
	
	NamespaceRemovingAttributesWrapper(Attributes source) {
		super(source);
	}

	int findIndexByLocalName(String localName) {
		for(int i=0;i<getLength();i++) {
			if (localName.equals(getLocalName(i))) {
				return i;
			}
		}
		return -1;
	}

	@Override
	public int getIndex(String uri, String localName) {
		return findIndexByLocalName(localName);
	}


	@Override
	public String getType(String uri, String localName) {
		return getType(findIndexByLocalName(localName));
	}

	@Override
	public String getURI(int i) {
		return "";
	}

	@Override
	public String getValue(String uri, String localName) {
		return getValue(findIndexByLocalName(localName));
	}

}
