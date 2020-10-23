package nl.nn.adapterframework.doc.model;

import org.xml.sax.SAXException;

import lombok.Getter;

/**
 * This class is similar to {@link ConfigChild}, but it is not the same. As an example,
 * consider a digester rule that links setter {@code setAbc()} to a syntax 1 name {@code abc}.
 * This rule is represented by an instance of this class, {@code ConfigChildSetterDescriptor}. If there are
 * two classes {@code X} and {@code Y} with method {@code setAbc()}, then two
 * different instances of {@link ConfigChild} are needed. The reason is that
 * {@code X.setAbc()} and {@code Y.setAbc()} can have a different {@code sequenceInConfig}.
 * That field is obtained from an {@code IbisDoc} annotation.
 */
public class ConfigChildSetterDescriptor {
	private @Getter String methodName;
	private @Getter boolean mandatory;
	private @Getter boolean allowMultiple;
	private @Getter String syntax1Name;

	public ConfigChildSetterDescriptor(String methodName, String syntax1Name) throws SAXException {
		this.methodName = methodName;
		this.syntax1Name = syntax1Name;
		mandatory = false;
		if(methodName.startsWith("set")) {
			allowMultiple = false;
		} else if((methodName.startsWith("add")) || methodName.startsWith("register")) {
			allowMultiple = true;
		} else {
			throw new SAXException(
					String.format("Do not know how many elements go in method [%s]", methodName));
		}
	}
}
