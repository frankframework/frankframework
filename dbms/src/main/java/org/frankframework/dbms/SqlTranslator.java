/*
   Copyright 2020-2023 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package org.frankframework.dbms;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import lombok.extern.log4j.Log4j2;

import org.frankframework.util.ClassUtils;
import org.frankframework.util.StreamUtil;


/**
 * Sql syntax translator to translate queries
 * for different database management systems (e.g. Oracle to MsSql or PostgreSql to MySql)
 */
@Log4j2
public class SqlTranslator implements ISqlTranslator {
	private static final String PATTERN_FILE = "SqlTranslationPatterns.properties";
	private Map<String, Pattern> sources;
	private Map<String, String> targets;
	private String target;
	private boolean configured = false;

	public SqlTranslator(String source, String target) throws DbmsException {
		if (StringUtils.isEmpty(source) || StringUtils.isEmpty(target))
			throw new IllegalArgumentException("Can not translate from [" + source + "] to [" + target + "]");
		if (source.equalsIgnoreCase(target)) {
			log.warn("Same source and target for SqlTranslator. Skipping pattern generation.");
			return;
		}
		try {
			if (!readPatterns(source, target)) {
				return;
			}
		} catch (Exception e) {
			throw new DbmsException("cannot create SqlTranslator", e);
		}
		this.target = target;
		configured = true;
	}

	@Override
	public boolean canConvert(String from, String to) {
		return configured && to.equals(target);
	}

	/**
	 * Translates the given query to the target language.
	 * Uses the translation rules set by this and the target translators.
	 *
	 * @param original Original query to be translated.
	 * @return Translated query.
	 */
	@Override
	public String translate(String original) {
		String query = original;
		if (sources != null) {
			for (Map.Entry<String, Pattern> entry : sources.entrySet()) {
				String label = entry.getKey();
				Matcher matcher = entry.getValue().matcher(query);
				if (matcher.find()) {
					if (log.isTraceEnabled()) log.trace("Found a match for label [{}] pattern [{}]", label, sources.get(label));
					String replacement = targets.get(label);
					if (StringUtils.isNotEmpty(replacement)) {
						query = matcher.replaceAll(replacement);
					} else {
						query = matcher.replaceAll("");
					}
				}
			}
		}
		if (StringUtils.isEmpty(query)) {
			return null;
		}
		return query;
	}

	/**
	 * Compiles a pattern with necessary flags.
	 *
	 * @param str String to be compiled.
	 * @return Output pattern.
	 */
	protected Pattern toPattern(String str) {
		// Make sure there are no greedy matchers.
		String pattern = str.replaceAll("\\.\\*", ".*?");
		log.trace("Compiling pattern [{}]", pattern);
		return Pattern.compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
	}

	/**
	 * Reads data from PATTERN_FILE.
	 * Puts the data in memory to be used later.
	 *
	 * @throws IOException If database name can not be found or file can not be read.
	 */
	private boolean readPatterns(String sourceDialect, String targetDialect) throws IOException {
		sources = new LinkedHashMap<>();
		targets = new LinkedHashMap<>();

		String sourceMatch = (".source." + sourceDialect.replace(" ", "_")).toLowerCase();
		String targetMatch = (".target." + targetDialect.replace(" ", "_")).toLowerCase();

		URL resourceUrl = ClassUtils.getResourceURL(PATTERN_FILE);
		if (resourceUrl == null) {
			throw new IOException("unable to find SQL Pattern File");
		}
		Reader streamReader = StreamUtil.getCharsetDetectingInputStreamReader(resourceUrl.openStream());
		try (BufferedReader reader = new BufferedReader(streamReader)) {
			String line = reader.readLine();
			while (line != null) {
				int equalsPos = line.indexOf("=");
				if (!line.startsWith("#") && equalsPos >= 0) {
					String key = line.substring(0, equalsPos).trim().toLowerCase();
					String value = line.substring(equalsPos + 1).trim();
					if (log.isTraceEnabled()) log.trace("read key [{}] value [{}]", key, value);
					int sourceMatchPos = key.indexOf(sourceMatch);
					if (sourceMatchPos > 0) {
						String label = key.substring(0, sourceMatchPos);
						sources.put(label, toPattern(value));
					} else {
						int targetMatchPos = key.indexOf(targetMatch);
						if (targetMatchPos > 0) {
							String label = key.substring(0, targetMatchPos);
							targets.put(label, value);
						}
					}
				}
				line = reader.readLine();
			}
		}
		for (Iterator<String> it = sources.keySet().iterator(); it.hasNext(); ) {
			String label = it.next();
			String source = sources.get(label).toString();
			String target = targets.get(label);
			if (target == null || target.equals(source) || "$0".equals(target)) {
				it.remove();
			} else {
				log.debug("configured translation pattern label [{}] source [{}] target [{}]", label, source, target);
			}
		}
		return true;
	}

}
