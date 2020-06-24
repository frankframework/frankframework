package nl.nn.adapterframework.jdbc.dbms;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.Logger;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;

/**
 * Sql syntax translator to translate queries
 * for different database management systems (e.g. Oracle to MsSql or PostgreSql to MySql)
 */
public class SqlTranslator {
	private static final String SOURCE_CSV = "dbms/source.csv";
	private static final String TARGET_CSV = "dbms/target.csv";
	private static final Pattern spaceReplacer = Pattern.compile("\\s+(?=((\\\\[\\\\\"]|[^\\\\\"])*\"(\\\\[\\\\\"]|[^\\\\\"])*\")*(\\\\[\\\\\"]|[^\\\\\"])*$)");

	private final Logger logger = LogUtil.getLogger(this);
	private Map<String, String> target;
	private Map<String, Pattern> source;
	private boolean translate = AppConstants.getInstance().getBoolean("jdbc.translate", true);

	public SqlTranslator(String source, String target) throws IOException, CsvValidationException {
		if (StringUtils.isEmpty(source) || StringUtils.isEmpty(target))
			throw new IllegalArgumentException("Can not translate from [" + source + "] to [" + target + "]");
		if (source.equalsIgnoreCase(target)) {
			logger.warn("Same source and target for SqlTranslator. Skipping pattern generation.");
			return;
		}
		translate = AppConstants.getInstance().getBoolean("jdbc.translate." + source + "." + target, translate);
		if (!translate) {
			logger.warn("Query translation from [" + source + "] to [" + target + "] is disabled. Skipping pattern generation.");
			return;
		}
		readSource(source);
		readTarget(target);
	}

	/**
	 * Translates the given query to the target language.
	 * Uses the translation rules set by this and the target translators.
	 *
	 * @param original Original query to be translated.
	 * @return Translated query.
	 */
	public String translate(String original) throws NullPointerException {
		if (!translate || source == null || target == null || StringUtils.isEmpty(original)) {
			logger.info("Skipping SQL translation from [" + source + "] to [" + target + "]");
			return original;
		}
		String query = original;
		for (String key : source.keySet()) {
			Matcher matcher = source.get(key).matcher(query);
			if (matcher.find()) {
				logger.trace(String.format("Found a match for pattern [%s]", source.get(key).pattern()));
				String regex = target.get(key);
				if (StringUtils.isNotEmpty(regex)) {
					query = matcher.replaceAll(regex);
				} else {
					query = matcher.replaceAll("");
				}
			}
		}
		return cleanSpaces(query);
	}

	/**
	 * Compiles a pattern with necessary flags.
	 * @param str String to be compiled.
	 * @return Output pattern.
	 */
	protected Pattern toPattern(String str) {
		// Make sure there are no greedy matchers.
		String pattern = str.replaceAll("\\.\\*", ".*?");
		logger.trace("Compiling pattern [" + pattern + "]");
		return Pattern.compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
	}

	/**
	 * Reads data from SOURCE_CSV file.
	 * Puts the data in memory to be used later.
	 * @param name Name of the target database
	 * @throws IOException If database name can not be found or file can not be read.
	 * @throws CsvValidationException If database name can not be read.
	 */
	private void readSource(String name) throws IOException, CsvValidationException {
		source = new HashMap<>();
		URL resourceUrl = ClassUtils.getResourceURL(Thread.currentThread().getContextClassLoader(), SOURCE_CSV);
		CSVReader reader = new CSVReader(ClassUtils.urlToReader(resourceUrl, 1000));
		int index = getIndex(reader, name);
		logger.debug(String.format("Reading SqlTranslator source values for database [%s] from index [%d]", name, index));
		String[] values;
		while (((values = reader.readNext()) != null)) {
			if (StringUtils.isEmpty(values[index]))
				continue;
			source.put(values[0].trim(), toPattern(values[index].trim()));
		}
	}

	/**
	 * Reads data from TARGET_CSV file.
	 * Puts the data in memory to be used later.
	 * @param name Name of the target database
	 * @throws IOException If database name can not be found or file can not be read.
	 * @throws CsvValidationException If database name can not be read.
	 */
	private void readTarget(String name) throws IOException, CsvValidationException {
		target = new HashMap<>();
		URL resourceUrl = ClassUtils.getResourceURL(Thread.currentThread().getContextClassLoader(), TARGET_CSV);
		CSVReader reader = new CSVReader(ClassUtils.urlToReader(resourceUrl, 1000));
		int index = getIndex(reader, name);
		logger.debug(String.format("Reading SqlTranslator target values for database [%s] from index [%d]", name, index));
		String[] values;
		while (((values = reader.readNext()) != null)) {
			if (StringUtils.isEmpty(values[index]))
				continue;
				target.put(values[0].trim(), values[index].trim());
		}
	}

	/**
	 * Returns the index of the database in the csv file
	 * from the first line.
	 * @param reader Reader for the csv file.
	 * @param name Name of the database
	 * @return Index at which that database's regex values are stored.
	 */
	private int getIndex(CSVReader reader, String name) throws IOException, CsvValidationException, IllegalArgumentException {
		String[] firstline = reader.readNext();
		for (int i = 0; i < firstline.length; i++) {
			if (firstline[i].trim().equalsIgnoreCase(name))
				return i;
		}
		throw new IllegalArgumentException("Database name not found");
	}

	/**
	 * Replaces all multiple space characters with single space,
	 * as long as they are not enclosed within quotes.
	 * Then trims the ends from white spaces.
	 * @param str String to be cleaned.
	 * @return Cleaned string that does not contain multiple spaces, or spaces in either end.
	 */
	private String cleanSpaces(String str) {
		return spaceReplacer.matcher(str).replaceAll(" ").trim();
	}
}