package nl.nn.adapterframework.configuration.digester;

import org.apache.commons.lang3.builder.ToStringBuilder;

import lombok.Getter;
import lombok.Setter;

/**
 * Java representation of a digester rule specified in the digester-rules.xml file.
 *
 */
public class DigesterRule {

	/**
	 * The digester rule's pattern.
	 */
	private @Getter @Setter String pattern;

	/**
	 * The 'object-create-rule' attribute.
	 */
	private @Getter @Setter String object;

	/**
	 * The 'factory-create-rule' attribute.
	 * When non specified it uses the GenericFactory. When specified as 
	 * NULL-String it does not use a factory.
	 */
	private @Getter @Setter String factory;

	/**
	 * The 'set-next-rule' attribute.
	 */
	private @Getter @Setter String registerMethod;

	/**
	 * The 'set-top-rule' attribute.
	 */
	private @Getter @Setter String selfRegisterMethod;

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}
}