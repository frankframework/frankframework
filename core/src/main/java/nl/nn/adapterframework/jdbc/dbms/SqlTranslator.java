package nl.nn.adapterframework.jdbc.dbms;

import com.opencsv.CSVReader;
import org.apache.commons.lang.StringUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sql syntax translator to translate queries
 * for different database management systems (e.g. Oracle to MsSql or PostgreSql to MySql)
 */
public class SqlTranslator {
	private static final String SOURCE_CSV = "src/main/resources/dbms/source.csv";
	private static final String TARGET_CSV = "src/main/resources/dbms/target.csv";
	public Map<String, String> target;
	public Map<String, Pattern> source;

	public SqlTranslator(String source, String target) throws Exception {
		if (StringUtils.isEmpty(source) || StringUtils.isEmpty(target))
			throw new Exception("Can not translate from [" + source + "] to [" + target + "]");
		if (source.equalsIgnoreCase(target))
			return;
		readSource(source);
		readTarget(target);
	}

	/**
	 * Translates the given query to the target language.
	 * Uses the translation rules set by this and the target translators.
	 *
	 * @param query Query to be translated.
	 * @return Translated query.
	 */
	public String translate(String query) throws NullPointerException {
		if (source == null || target == null || StringUtils.isEmpty(query))
			return query;
		for (String key : source.keySet()) {
			Matcher matcher = source.get(key).matcher(query);
			if (matcher.find()) {
				String regex = target.get(key);
				if (StringUtils.isNotEmpty(regex)) {
					query = matcher.replaceAll(regex);
				} else {
					query = matcher.replaceAll("");
				}
			}
		}
		return query;
	}

	/**
	 * Compiles a pattern with necessary flags.
	 * @param str String to be compiled.
	 * @return Output pattern.
	 */
	protected Pattern toPattern(String str) {
		// Make sure there are no greedy matchers.
		str = str.replaceAll("\\.\\*", ".*?");
		return Pattern.compile(str, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
	}

	/**
	 * Reads data from SOURCE_CSV file.
	 * Puts the data in memory to be used later.
	 * @param name Name of the target database
	 * @throws Exception If database name can not be found or file can not be read.
	 */
	private void readSource(String name) throws Exception {
		source = new HashMap<>();
		CSVReader reader = new CSVReader(new BufferedReader(new FileReader(SOURCE_CSV)));
		int index = getIndex(reader, name);
		String[] values;
		while (((values = reader.readNext()) != null) && StringUtils.isNotEmpty(values[index])) {
			source.put(values[0], toPattern(values[index]));
		}
	}

	/**
	 * Reads data from TARGET_CSV file.
	 * Puts the data in memory to be used later.
	 * @param name Name of the target database
	 * @throws Exception If database name can not be found or file can not be read.
	 */
	private void readTarget(String name) throws Exception {
		target = new HashMap<>();
		CSVReader reader = new CSVReader(new BufferedReader(new FileReader(TARGET_CSV)));
		int index = getIndex(reader, name);
		String[] values;
		while (((values = reader.readNext()) != null) && StringUtils.isNotEmpty(values[index])) {
				target.put(values[0], values[index]);
		}
	}

	/**
	 * Returns the index of the database in the csv file
	 * from the first line.
	 * @param reader Reader for the csv file.
	 * @param name Name of the database
	 * @return Index at which that database's regex values are stored.
	 * @throws Exception If database name is not found in file.
	 */
	private int getIndex(CSVReader reader, String name) throws Exception {
		String[] firstline = reader.readNext();
		for (int i = 0; i < firstline.length; i++) {
			if (firstline[i].equalsIgnoreCase(name))
				return i;
		}
		throw new Exception("Database name not found");
	}
}