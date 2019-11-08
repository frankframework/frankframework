package nl.nn.adapterframework.xml;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

public class ContainerElementFilter extends FullXmlFilter {

	private String containerElement;
	private boolean includeRoot=false;
	
	private int level;
	private int globalLevel;
	
	public ContainerElementFilter(String containerElement) {
		super();
		this.containerElement=containerElement;
	}

	public ContainerElementFilter(String containerElement, boolean includeRoot) {
		super();
		this.containerElement=containerElement;
		this.includeRoot=includeRoot;
	}

	public ContainerElementFilter(String containerElement, XMLReader parent) {
		super(parent);
		this.containerElement=containerElement;
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
		if (level>0 || (includeRoot && globalLevel==0)) {
			super.startElement(uri, localName, qName, atts);
			level++;
		} else {
			if (localName.equals(containerElement)) {
				level=1;
			}
		}
		globalLevel++;
	}


	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		globalLevel--;
		if (level-->0 || (includeRoot && globalLevel==0)) {
			super.endElement(uri, localName, qName);
		}
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		if (level>0) {
			super.characters(ch, start, length);
		}
	}


	@Override
	public void comment(char[] ch, int start, int length) throws SAXException {
		if (level>0) {
			super.comment(ch, start, length);
		}
	}

	@Override
	public void startCDATA() throws SAXException {
		if (level>0) {
			super.startCDATA();
		}
	}

	@Override
	public void endCDATA() throws SAXException {
		if (level>0) {
			super.endCDATA();
		}
	}

	@Override
	public void startEntity(String name) throws SAXException {
		if (level>0) {
			super.startEntity(name);
		}
	}

	@Override
	public void endEntity(String name) throws SAXException {
		if (level>0) {
			super.endEntity(name);
		}
	}

	@Override
	public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
		if (level>0) {
			super.ignorableWhitespace(ch, start, length);
		}
	}


}
