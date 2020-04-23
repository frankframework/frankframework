package nl.nn.adapterframework.jdbc.dbms;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.FileUtils;
import nl.nn.adapterframework.util.LogUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sql syntax translator to translate queries
 * for different database management systems (e.g. Oracle to MsSql or PostgreSql to MySql)
 */
public class SqlTranslator {
	Logger logger = LogUtil.getLogger(this);
	private static final String SOURCE_CSV = "dbms/source.csv";
	private static final String TARGET_CSV = "dbms/target.csv";
	public Map<String, String> target;
	public Map<String, Pattern> source;
	private boolean translate = AppConstants.getInstance().getBoolean("jdbc.translate", true);
	private static Map<String, String> logs = new HashMap<>();

	public SqlTranslator(String source, String target) throws IOException, CsvValidationException {
		LogUtil.getRootLogger().setLevel(Level.ALL);
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
	 * @param query Query to be translated.
	 * @return Translated query.
	 */
	public String translate(String query) throws NullPointerException {
		if (!translate || source == null || target == null || StringUtils.isEmpty(query)) {
			logger.info("Skipping SQL translation from [" + source + "] to [" + target + "]");
			return query;
		}

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
		return query.trim();
	}

	/**
	 * Compiles a pattern with necessary flags.
	 * @param str String to be compiled.
	 * @return Output pattern.
	 */
	protected Pattern toPattern(String str) {
		// Make sure there are no greedy matchers.
		str = str.replaceAll("\\.\\*", ".*?");
		logger.trace("Compiling pattern [" + str + "]");
		return Pattern.compile(str, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
	}

	/**
	 * Reads data from SOURCE_CSV file.
	 * Puts the data in memory to be used later.
	 * @param name Name of the target database
	 * @throws Exception If database name can not be found or file can not be read.
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
	 * @throws Exception If database name can not be found or file can not be read.
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
	 * @throws Exception If database name is not found in file.
	 */
	private int getIndex(CSVReader reader, String name) throws IOException, CsvValidationException, IllegalArgumentException {
		String[] firstline = reader.readNext();
		for (int i = 0; i < firstline.length; i++) {
			if (firstline[i].trim().equalsIgnoreCase(name))
				return i;
		}
		throw new IllegalArgumentException("Database name not found");
	}
}