package nl.nn.adapterframework.jdbc.dbms.translator;

import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class ITranslator {
	Map<String, String> replacements;
	Map<String, Pattern> regexes;
	ITranslator target;

	public ITranslator(ITranslator target) {
		this();
		setTarget(target);
	}

	public ITranslator() {
		replacements = new HashMap<>();
		regexes = new HashMap<>();
		populateMaps();
	}

	/**
	 * Translates the given query to the target language.
	 * Uses the translation rules set by this and the target translators.
	 *
	 * @param query Query to be translated.
	 * @return Translated query.
	 */
	public String translate(String query) throws NullPointerException {
		for (String key : regexes.keySet()) {
			Matcher matcher = regexes.get(key).matcher(query);
			if (matcher.find()) {
				String regex = target.replacements.get(key);
				if (StringUtils.isNotEmpty(regex)) {
					query = matcher.replaceAll(regex);
				} else {
					query = matcher.replaceAll("");
				}
			}
		}
		return query;
	}

	public void setTarget(ITranslator target) {
		this.target = target;
	}

	protected Pattern toPattern(String str) {
		// Make sure there are no greedy matchers.
		str = str.replaceAll("\\.\\*", ".*?");
		return Pattern.compile(str, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
	}

	protected abstract void populateMaps();
}