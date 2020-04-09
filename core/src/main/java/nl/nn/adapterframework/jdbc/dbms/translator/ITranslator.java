package nl.nn.adapterframework.jdbc.dbms.translator;

import org.apache.commons.lang.StringUtils;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class ITranslator {
	Map<String, Pattern> finders;
	Map<String, String> values;
	ITranslator target;

	public ITranslator(ITranslator target) {
		this.target = target;
		populateMaps();
	}

	/**
	 * Translates the given query to the target language.
	 * Uses the translation rules set by this and the target translators.
	 *
	 * @param query Query to be translated.
	 * @return Translated query.
	 */
	public String translate(String query) {
		for (String key : finders.keySet()) {
			Matcher matcher = finders.get(key).matcher(query);
			String regex = target.getValueRegex(key);
			if (matcher.find() && StringUtils.isNotEmpty(key)) {
				query = matcher.replaceAll(key);
			}
		}
		return query;
	}

	private String getValueRegex(String key) {
		return values.get(key);
	}

	protected abstract void populateMaps();
}