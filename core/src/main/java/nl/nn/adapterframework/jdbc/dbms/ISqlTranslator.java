package nl.nn.adapterframework.jdbc.dbms;

public interface ISqlTranslator {

	public boolean canConvert(String from, String to);
	
	/**
	 * Translates the given query to the target language.
	 * Uses the translation rules set by this and the target translators.
	 *
	 * @param original Original query to be translated.
	 * @return Translated query.
	 */
	public String translate(String original);
	
}
