/*
   Copyright 2019 Nationale-Nederlanden

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
package nl.nn.adapterframework.jdbc.dbms;

import java.math.BigInteger;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import nl.nn.adapterframework.jdbc.JdbcException;
import nl.nn.adapterframework.util.LogUtil;

/**
 * Class to translate Oracle queries to H2.
 * <p>
 * It is assumed that in H2 the 'Oracle Compatibility Mode' is used
 * (<code>url=jdbc:h2:~/test;MODE=Oracle</code>). Missing compatibility is
 * covered in this class.
 * </p>
 * 
 * @author Peter Leeuwenburgh
 */
public class OracleToH2Translator {
	private static Logger log = LogUtil.getLogger(OracleToH2Translator.class);

	private static final String SEQUENCE_MAX_VALUE_STRING = "999999999999999999";
	private static final BigInteger SEQUENCE_MAX_VALUE = new BigInteger(SEQUENCE_MAX_VALUE_STRING);

	public static String convertQuery(Connection connection, String query) throws JdbcException, SQLException {
		if (query == null)
			return null;
		String originalQuery = query.trim();
		// add spaces around following characters: ,;()
		String trimmedQuery = originalQuery.replaceAll("([,;\\(\\)])", " $1 ").trim();
		boolean removedEOS = false;
		// remove last character if it is a semi-colon
		if (trimmedQuery != null && trimmedQuery.length() > 0 && trimmedQuery.charAt(trimmedQuery.length() - 1) == ';') {
			trimmedQuery = trimmedQuery.substring(0, trimmedQuery.length() - 1);
			removedEOS = true;
		}
		// split on whitespaces excepts whitespaces between single quotes
		String[] split = trimmedQuery.split("\\s+(?=([^']*'[^']*')*[^']*$)");
		String[] newSplit = convertQuery(split);
		if (newSplit == null) {
			log.debug("ignore oracle query [" + originalQuery + "]");
			return null;
		}
		if (compareStringArrays(split, newSplit)) {
			log.debug("oracle query [" + originalQuery + "] not converted");
			return query;
		} else {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < newSplit.length; i++) {
				if (i > 0 && !"(".equals(newSplit[i]) && !",".equals(newSplit[i]) && !")".equals(newSplit[i]) && !"(".equals(newSplit[i - 1])) {
					sb.append(" ");
				}
				sb.append(newSplit[i]);
			}
			String convertedQuery = sb.toString() + (removedEOS ? ";" : "");
			log.debug("converted oracle query [" + originalQuery + "] to [" + convertedQuery + "]");
			return convertedQuery;
		}
	}

	private static String[] convertQuery(String[] split) {
		String[] newSplit;
		if (isSelectQuery(split)) {
			newSplit = convertQuerySelect(split);
		} else if (isSetDefineOffQuery(split)) {
			newSplit = null;
		} else if (isCreateOrReplaceTriggerQuery(split)) {
			newSplit = null;
		} else if (isAlterTriggerQuery(split)) {
			newSplit = null;
		} else if (isCreateSequenceQuery(split)) {
			newSplit = convertQueryCreateSequence(split);
		} else if (isCreateTableIbisStoreQuery(split)) {
			newSplit = convertQueryCreateTableIbisStore(split);
		} else if (isCreateTableQuery(split)) {
			newSplit = convertQueryCreateTable(split);
		} else if (isDropSequenceOrTableQuery(split)) {
			newSplit = convertQueryDropSequence(split);
		} else if (isCreateIndexQuery(split)) {
			newSplit = convertQueryCreateIndex(split);
		} else if (isAlterTableQuery(split)) {
			newSplit = convertQueryAlterTable(split);
		} else {
			return split;
		}
		return newSplit;
	}

	private static boolean isSelectQuery(String[] split) {
		return split.length > 14 && "SELECT".equalsIgnoreCase(split[0]);
	}

	private static boolean isSetDefineOffQuery(String[] split) {
		return split.length == 3 && "SET".equalsIgnoreCase(split[0]) && "DEFINE".equalsIgnoreCase(split[1]) && "OFF".equalsIgnoreCase(split[2]);
	}

	private static boolean isCreateOrReplaceTriggerQuery(String[] split) {
		return split.length > 4 && "CREATE".equalsIgnoreCase(split[0]) && "OR".equalsIgnoreCase(split[1]) && "REPLACE".equalsIgnoreCase(split[2]) && "TRIGGER".equalsIgnoreCase(split[3]);
	}

	private static boolean isAlterTriggerQuery(String[] split) {
		return split.length > 2 && "ALTER".equalsIgnoreCase(split[0]) && "TRIGGER".equalsIgnoreCase(split[1]);
	}

	private static boolean isCreateSequenceQuery(String[] split) {
		return split.length > 3 && "CREATE".equalsIgnoreCase(split[0]) && "SEQUENCE".equalsIgnoreCase(split[1]);
	}

	private static boolean isCreateTableIbisStoreQuery(String[] split) {
		return split.length > 4 && "CREATE".equalsIgnoreCase(split[0]) && "TABLE".equalsIgnoreCase(split[1]) && "IBISSTORE".equalsIgnoreCase(split[2]);
	}

	private static boolean isCreateTableQuery(String[] split) {
		return split.length > 3 && "CREATE".equalsIgnoreCase(split[0]) && "TABLE".equalsIgnoreCase(split[1]);
	}

	private static boolean isDropSequenceOrTableQuery(String[] split) {
		return split.length >= 3 && "DROP".equalsIgnoreCase(split[0]) && ("SEQUENCE".equalsIgnoreCase(split[1]) || "TABLE".equalsIgnoreCase(split[1]));
	}

	private static boolean isCreateIndexQuery(String[] split) {
		return split.length > 3 && "CREATE".equalsIgnoreCase(split[0]) && "INDEX".equalsIgnoreCase(split[1]);
	}

	private static boolean isAlterTableQuery(String[] split) {
		return split.length > 3 && "ALTER".equalsIgnoreCase(split[0]) && "TABLE".equalsIgnoreCase(split[1]);
	}

	private static String[] convertQuerySelect(String[] split) {
		List<String> newSplit = new ArrayList<>();
		for (int i = 0; i < split.length; i++) {
			if ("LISTAGG".equalsIgnoreCase(split[i]) && (i + 12) < split.length && "(".equals(split[i + 1]) && ",".equals(split[i + 3]) && ")".equals(split[i + 5]) && "WITHIN".equalsIgnoreCase(split[i + 6]) && "GROUP".equalsIgnoreCase(split[i + 7]) && "(".equals(split[i + 8]) && "ORDER".equalsIgnoreCase(split[i + 9]) && "BY".equalsIgnoreCase(split[i + 10]) && ")".equals(split[i + 12])) {
				newSplit.add("group_concat");
				newSplit.add("(");
				newSplit.add(split[i + 2]);
				newSplit.add(split[i + 9]);
				newSplit.add(split[i + 10]);
				newSplit.add(split[i + 11]);
				newSplit.add("SEPARATOR");
				newSplit.add(split[i + 4]);
				newSplit.add(")");
				i = i + 12;
			} else {
				newSplit.add(split[i]);
			}
		}
		return newSplit.toArray(new String[0]);
	}

	private static String[] convertQueryCreateSequence(String[] split) {
		List<String> newSplit = new ArrayList<>();
		for (int i = 0; i < split.length; i++) {
			newSplit.add(split[i]);
			if ("MAXVALUE".equalsIgnoreCase(split[i]) && (i + 1) < split.length && StringUtils.isNumeric(split[i + 1])) {
				BigInteger maxValue = new BigInteger(split[i + 1]);
				if (maxValue.compareTo(SEQUENCE_MAX_VALUE) > 0) {
					newSplit.add(SEQUENCE_MAX_VALUE_STRING);
					i++;
				}
			}
		}
		return newSplit.toArray(new String[0]);
	}

	private static String[] convertQueryCreateTableIbisStore(String[] split) {
		List<String> newSplit = new ArrayList<>();
		for (int i = 0; i < split.length; i++) {
			newSplit.add(split[i]);
			if ("MESSAGEKEY".equalsIgnoreCase(split[i]) && (i + 4) < split.length && "NUMBER".equals(split[i + 1]) && "(".equals(split[i + 2]) && ")".equals(split[i + 4])) {
				newSplit.add("INT");
				newSplit.add("IDENTITY");
				i = i + 4;
			}
		}
		return newSplit.toArray(new String[0]);
	}

	private static String[] convertQueryCreateTable(String[] split) {
		List<String> newSplit = new ArrayList<>();
		for (int i = 0; i < split.length; i++) {
			newSplit.add(split[i]);
			if ("NUMBER".equalsIgnoreCase(split[i]) && (i + 5) < split.length && "(".equals(split[i + 1]) && "*".equals(split[i + 2]) && ",".equals(split[i + 3]) && ")".equals(split[i + 5])) {
				newSplit.add(split[i + 1]);
				newSplit.add("38");
				newSplit.add(split[i + 3]);
				newSplit.add(split[i + 4]);
				newSplit.add(split[i + 5]);
				i = i + 5;
			}
		}
		return newSplit.toArray(new String[0]);
	}

	private static String[] convertQueryDropSequence(String[] split) {
		List<String> newSplit = new ArrayList<>();
		newSplit.add(split[0]);
		newSplit.add(split[1]);
		newSplit.add("IF");
		newSplit.add("EXISTS");
		newSplit.add(split[2]);
		for (int i = 3; i < split.length; i++) {
			newSplit.add(split[i]);
		}
		return newSplit.toArray(new String[0]);
	}

	private static String[] convertQueryCreateIndex(String[] split) {
		List<String> newSplit = new ArrayList<>();
		for (int i = 0; i < split.length; i++) {
			if ("LOWER".equalsIgnoreCase(split[i]) && (i + 3) < split.length && "(".equals(split[i + 1]) && ")".equals(split[i + 3])) {
				newSplit.add(split[i + 2]);
				i = i + 3;
			} else {
				newSplit.add(split[i]);
			}
		}
		return newSplit.toArray(new String[0]);
	}

	private static String[] convertQueryAlterTable(String[] split) {
		List<String> newSplit = new ArrayList<>();
		for (int i = 0; i < split.length; i++) {
			if ("NOT".equalsIgnoreCase(split[i]) && (i + 4) < split.length && "DEFERRABLE".equalsIgnoreCase(split[i + 1]) && "INITIALLY".equalsIgnoreCase(split[i + 2]) && "IMMEDIATE".equalsIgnoreCase(split[i + 3]) && "VALIDATE".equalsIgnoreCase(split[i + 4])) {
				// ignore
				i = i + 4;
			} else {
				newSplit.add(split[i]);
			}
		}
		return newSplit.toArray(new String[0]);
	}

	private static boolean compareStringArrays(String[] array1, String[] array2) {
		if (array1.length != array2.length)
			return false;
		for (int i = 0; i < array1.length; i++) {
			if (!array1[i].equalsIgnoreCase(array2[i]))
				return false;
		}
		return true;
	}
}
