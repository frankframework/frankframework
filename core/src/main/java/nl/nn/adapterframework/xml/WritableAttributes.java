package nl.nn.adapterframework.xml;

import java.util.Objects;

import org.xml.sax.Attributes;

public class WritableAttributes extends AttributesWrapper {

	public WritableAttributes(Attributes source) {
		super(source);
	}

	public void setValue(String qName, String value) {
		Attribute attribute = getAttributes().get(getIndex(qName));
		Objects.requireNonNull(attribute);
		attribute.value = value;
	}
}
