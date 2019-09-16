/**
 * 
 */
package nl.nn.adapterframework.extensions.aspose.services.util;

/**
 * @author <a href="mailto:gerard_van_der_hoorn@deltalloyd.nl">Gerard van der
 *         Hoorn</a> (d937275)
 *
 */
public class FileConstants {

	/**
	 * The specific allowed characters in a filename (with the exception of digits
	 * and letters).
	 */
	public static final String ALLOWED_SPECIFIC_CHARACTERS_IN_NAME = "!#$%&()=@^_";

	/**
	 * The specific allowed characters in filename of digits and letters.
	 */
	public static final String ALLOWED_CHARACTERS_IN_NAME = "0-9a-zA-Z ";

	/**
	 * The specific allowed characters in a filename of digits and letters and
	 * special characters.
	 */
	public static final String ALLOWED_CHARACTERS_IN_NAME_REGEX = "[" + ALLOWED_CHARACTERS_IN_NAME
			+ ALLOWED_SPECIFIC_CHARACTERS_IN_NAME + "]+";

	/**
	 * The inverse of ALLOWED_CHARACTERS_IN_NAME_REGEX.
	 */
	public static final String REPLACE_CHARACTERS_IN_NAME_REGEX = "[^" + ALLOWED_CHARACTERS_IN_NAME
			+ ALLOWED_SPECIFIC_CHARACTERS_IN_NAME + "]+";

	/**
	 * The replace character.
	 */
	public static final String REPLACE_CHARACTER = "_";
	
	/**
	 * Private constructor to prevent constructing this class.
	 */
	private FileConstants() {
	}

}
