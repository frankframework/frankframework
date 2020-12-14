package nl.nn.adapterframework.doc;

import java.util.ArrayDeque;
import java.util.Deque;

import nl.nn.adapterframework.util.XmlBuilder;

abstract class XsdWriteFilter {
	boolean isWritingOn;

	abstract void startSimpleElement();
	abstract void startComplexStuff();
	
	static class EnableSimpleElements extends XsdWriteFilter {
		@Override
		void startSimpleElement() {
			this.isWritingOn = true;
		}
		
		@Override
		void startComplexStuff() {
			isWritingOn = false;
		}
	}

	static class EnableComplexStuff extends XsdWriteFilter {
		@Override
		void startSimpleElement() {
			isWritingOn = false;
		}
		
		@Override
		void startComplexStuff() {
			isWritingOn = true;
		}
	}

	private enum ControlNode {
		SIMPLE_ELEMENT,
		COMPLEX_STUFF;
	}

	static class ControlStack {
		private final XsdWriteFilter slave;
		private final Deque<ControlNode> controlNodes = new ArrayDeque<>();

		ControlStack(XsdWriteFilter slave) {
			this.slave = slave;
		}

		void pushSimpleElement() {
			controlNodes.addLast(ControlNode.SIMPLE_ELEMENT);
			apply();
		}

		void pushComplexStuff() {
			controlNodes.addLast(ControlNode.COMPLEX_STUFF);
			apply();
		}

		void pop() {
			controlNodes.removeLast();
			apply();
		}

		private void apply() {
			switch(controlNodes.getLast()) {
			case SIMPLE_ELEMENT:
				slave.startSimpleElement();
				break;
			case COMPLEX_STUFF:
				slave.startComplexStuff();
				break;
			}
		}
	}

	void addElement(XmlBuilder context, String elementName, String elementType) {
		if(isWritingOn) {
			DocWriterNewXmlUtils.addElement(context, elementName, elementType);
		}
	}


	void addElementRef(
			XmlBuilder context,
			String elementName,
			String minOccurs,
			String maxOccurs) {
		if(isWritingOn) {
			DocWriterNewXmlUtils.addElementRef(context, elementName, minOccurs, maxOccurs);
		}
	}

	XmlBuilder addElementWithType(XmlBuilder context, String name) {
		if(isWritingOn) {
			return DocWriterNewXmlUtils.addElementWithType(context, name);
		} else {
			return null;
		}
	}

	XmlBuilder addComplexType(XmlBuilder schema) {
		if(isWritingOn) {
			return DocWriterNewXmlUtils.addComplexType(schema);
		} else {
			return null;
		}
	}

	XmlBuilder addComplexType(XmlBuilder schema, String name) {
		if(isWritingOn) {
			return DocWriterNewXmlUtils.addComplexType(schema, name);
		} else {
			return null;
		}
	}

	XmlBuilder addChoice(XmlBuilder context) {
		if(isWritingOn) {
			return DocWriterNewXmlUtils.addChoice(context);
		} else {
			return null;
		}
	}

	XmlBuilder addSequence(XmlBuilder context) {
		if(isWritingOn) {
			return DocWriterNewXmlUtils.addSequence(context);
		} else {
			return null;
		}
	}

	XmlBuilder addAttribute(
			XmlBuilder context,
			String name,
			DocWriterNewXmlUtils.AttributeValueStatus valueStatus,
			String value,
			DocWriterNewXmlUtils.AttributeUse attributeUse) {
		if(isWritingOn) {
			return DocWriterNewXmlUtils.addAttribute(context, name, valueStatus, value, attributeUse);
		} else {
			return null;
		}
	}

	XmlBuilder addAnyAttribute(XmlBuilder context) {
		if(isWritingOn) {
			return DocWriterNewXmlUtils.addAnyAttribute(context);
		} else {
			return null;
		}
	}

	void addDocumentation(XmlBuilder context, String description) {
		if(isWritingOn) {
			DocWriterNewXmlUtils.addDocumentation(context, description);
		}
	}

	XmlBuilder addGroup(XmlBuilder context, String name) {
		if(isWritingOn) {
			return DocWriterNewXmlUtils.addGroup(context, name);
		} else {
			return null;
		}
	}

	XmlBuilder addGroupRef(XmlBuilder context, String id) {
		if(isWritingOn) {
			return DocWriterNewXmlUtils.addGroupRef(context, id);
		} else {
			return null;
		}
	}

	XmlBuilder addGroupRef(XmlBuilder context, String id, String minOccurs, String maxOccurs) {
		if(isWritingOn) {
			return DocWriterNewXmlUtils.addGroupRef(context, id, minOccurs, maxOccurs);
		} else {
			return null;
		}
	}

	XmlBuilder addAttributeGroup(XmlBuilder context, String name) {
		if(isWritingOn) {
			return DocWriterNewXmlUtils.addAttributeGroup(context, name);
		} else {
			return null;
		}
	}

	XmlBuilder addAttributeGroupRef(XmlBuilder context, String name) {
		if(isWritingOn) {
			return DocWriterNewXmlUtils.addAttributeGroupRef(context, name);
		} else {
			return null;
		}
	}

	XmlBuilder addComplexContent(XmlBuilder context) {
		if(isWritingOn) {
			return DocWriterNewXmlUtils.addComplexContent(context);
		} else {
			return null;
		}
	}

	XmlBuilder addExtension(XmlBuilder context, String base) {
		if(isWritingOn) {
			return DocWriterNewXmlUtils.addExtension(context, base);
		} else {
			return null;
		}
	}
}
