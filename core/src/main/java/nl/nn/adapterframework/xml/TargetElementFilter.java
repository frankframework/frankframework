package nl.nn.adapterframework.xml;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

public class TargetElementFilter extends FullXmlFilter {

	private String targetElement;
	private boolean includeRoot=false;
	
	private int level;
	private int globalLevel;
	
	public TargetElementFilter(String targetElement) {
		super();
		this.targetElement=targetElement;
	}

	public TargetElementFilter(String targetElement, boolean includeRoot) {
		super();
		this.targetElement=targetElement;
		this.includeRoot=includeRoot;
	}

	public TargetElementFilter(String targetElement, XMLReader parent) {
		super(parent);
		this.targetElement=targetElement;
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
		if (level<=0) {
			if (localName.equals(targetElement)) {
				level=1;
			}
		} else {
			level++;
		}
		if (level>0 || (includeRoot && globalLevel==0)) {
			super.startElement(uri, localName, qName, atts);
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
