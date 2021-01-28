package nl.nn.adapterframework.doc;

import nl.nn.adapterframework.doc.model.ConfigChild;
import nl.nn.adapterframework.util.XmlBuilder;

abstract class Multiplicity {
	abstract void addElementRef(XmlBuilder context, String name);
	abstract void addGroupRef(XmlBuilder context, String name);

	static class ForConfigChild extends Multiplicity {
		private final ConfigChild configChild;

		ForConfigChild(ConfigChild configChild) {
			this.configChild = configChild;
		}

		@Override
		void addElementRef(XmlBuilder context, String name) {
			DocWriterNewXmlUtils.addElementRef(context, name, getMinOccurs(configChild), getMaxOccurs(configChild));
		}

		@Override
		void addGroupRef(XmlBuilder context, String name) {
			DocWriterNewXmlUtils.addGroupRef(context, name, getMinOccurs(configChild), getMaxOccurs(configChild));
		}

		private static String getMinOccurs(ConfigChild child) {
			if(child.isMandatory()) {
				return "1";
			} else {
				return "0";
			}
		}

		private static String getMaxOccurs(ConfigChild child) {
			if(child.isAllowMultiple()) {
				return "unbounded";
			} else {
				return "1";
			}
		}
	}

	static class Once extends Multiplicity {
		@Override
		void addElementRef(XmlBuilder context, String name) {
			DocWriterNewXmlUtils.addElementRef(context, name);
		}

		@Override
		void addGroupRef(XmlBuilder context, String name) {
			DocWriterNewXmlUtils.addGroupRef(context, name);
		}		
	}

	static class Multiple extends Multiplicity {
		@Override
		void addElementRef(XmlBuilder context, String name) {
			DocWriterNewXmlUtils.addElementRef(context, name, "0", "unbounded");
		}

		@Override
		void addGroupRef(XmlBuilder context, String name) {
			DocWriterNewXmlUtils.addGroupRef(context, name, "0", "unbounded");
		}		
	}
}
