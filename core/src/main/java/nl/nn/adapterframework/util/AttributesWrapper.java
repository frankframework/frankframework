package nl.nn.adapterframework.util;

import org.xml.sax.Attributes;

public class AttributesWrapper implements Attributes {
	private Attributes source;
	
	AttributesWrapper(Attributes source) {
		this.source=source;
	}

	@Override
	public int getIndex(String qName) {
		return source.getIndex(qName);
	}

	@Override
	public int getIndex(String uri, String localName) {
		return source.getIndex(uri, localName);
	}

	@Override
	public int getLength() {
		return source.getLength();
	}

	@Override
	public String getLocalName(int i) {
		return source.getLocalName(i);
	}

	@Override
	public String getQName(int i) {
		return source.getQName(i);
	}

	@Override
	public String getType(int i) {
		return source.getType(i);
	}

	@Override
	public String getType(String qName) {
		return source.getType(qName);
	}

	@Override
	public String getType(String uri, String localName) {
		return source.getType(uri,localName);
	}

	@Override
	public String getURI(int i) {
		return source.getURI(i);
	}

	@Override
	public String getValue(int i) {
		return source.getValue(i);
	}

	@Override
	public String getValue(String qName) {
		return source.getValue(qName);
	}

	@Override
	public String getValue(String uri, String localName) {
		return source.getValue(uri, localName);
	}

}
