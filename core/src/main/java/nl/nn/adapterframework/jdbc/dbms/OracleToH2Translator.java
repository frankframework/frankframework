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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.jdbc.JdbcException;
import nl.nn.adapterframework.jdbc.QueryExecutionContext;
import nl.nn.adapterframework.util.LogUtil;

/**
 * Class to translate Oracle queries to H2.
 * <p>
 * It is assumed that in H2 the 'Oracle Compatibility Mode' is used
 * (<code>url=jdbc:h2:~/test;MODE=Oracle</code>). Missing compatibility is
 * covered in this class.
 * </p>
 * <p>
 * Note: The Oracle functions <code>INSERT EMPTY_CLOB()</code> and
 * <code>INSERT EMPTY_BLOB()</code> are replaced with INSERT '', however in H2
 * this will result in a <code>INSERT NULL</code>
 * </p>
 * 
 * @author Peter Leeuwenburgh
 */
public class OracleToH2Translator {
	private static Logger log = LogUtil.getLogger(OracleToH2Translator.class);

	private static final String SEQUENCE_MAX_VALUE_STRING = "999999999999999999";
	private static final BigInteger SEQUENCE_MAX_VALUE = new BigInteger(SEQUENCE_MAX_VALUE_STRING);

	public static String convertQuery(QueryExecutionContext queryExecutionContext, boolean canModifyQueryExecutionContext) throws JdbcException, SQLException {
		if (StringUtils.isEmpty(queryExecutionContext.getQuery()))
			return null;

		// query can start with comment (multiple lines) which should not be
		// converted
		StringBuilder queryComment = new StringBuilder();
		StringBuilder queryStatement = new StringBuilder();
		boolean comment = true;
		String[] lines = queryExecutionContext.getQuery().split("\\r?\\n");
		for (String line : lines) {
			// ignore empty lines
			if (line.trim().length() > 0) {
				if (comment && line.trim().startsWith("--")) {
					queryComment.append(line.trim() + System.lineSeparator());
				} else {
					comment = false;
					queryStatement.append(line.trim() + System.lineSeparator());
				}
			}
		}

		String originalQuery = queryStatement.toString().trim();
		// add spaces around following characters: ,;()=
		String orgQueryReadyForSplit = originalQuery.replaceAll("([,;\\(\\)=])", " $1 ").trim();
		boolean removedEOS = false;
		// remove last character if it is a semi-colon
		String orgQueryReadyForSplitEOS = StringUtils.removeEnd(orgQueryReadyForSplit, ";");
		if (!orgQueryReadyForSplit.equals(orgQueryReadyForSplitEOS)) {
			removedEOS = true;
		}
		// split on whitespaces excepts whitespaces between single quotes
		String[] split = orgQueryReadyForSplitEOS.split("\\s+(?=([^']*'[^']*')*[^']*$)");
		String[] newSplit = convertQuery(split, queryExecutionContext, canModifyQueryExecutionContext);
		if (newSplit == null) {
			log.debug("ignore oracle query [" + queryComment.toString() + originalQuery + "]");
			return null;
		}
		if (compareStringArrays(split, newSplit)) {
			log.debug("oracle query [" + queryComment.toString() + originalQuery + "] not converted");
			return queryExecutionContext.getQuery();
		} else {
			String convertedQuery = getConvertedQueryAsString(newSplit, removedEOS);
			log.debug("converted oracle query [" + queryComment.toString() + originalQuery + "] to [" + queryComment.toString() + convertedQuery + "]");
			return queryComment.toString() + convertedQuery;
		}
	}

	private static String getConvertedQueryAsString(String[] newSplit, boolean removedEOS) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < newSplit.length; i++) {
			if (i > 0 && !"(".equals(newSplit[i]) && !",".equals(newSplit[i]) && !")".equals(newSplit[i]) && !"=".equals(newSplit[i]) && !"(".equals(newSplit[i - 1]) && !"=".equals(newSplit[i - 1])) {
				sb.append(" ");
			}
			sb.append(newSplit[i]);
		}
		return sb.toString() + (removedEOS ? ";" : "");
	}

	private static String[] convertQuery(String[] split, QueryExecutionContext queryExecutionContext, boolean canModifyQueryExecutionContext) {
		String[] newSplit;
		if (isSelectOneWhereForUpdateQuery(split)) {
			newSplit = convertQuerySelectOneWhereForUpdate(split);
		} else if (isSelectQuery(split)) {
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
		} else if (isAlterTableIbisStoreQuery(split)) {
			// for H2 primary key is set via identity in create table
			newSplit = null;
		} else if (isAlterTableQuery(split)) {
			newSplit = convertQueryAlterTable(split);
		} else if (isInsertIntoQuery(split)) {
			newSplit = convertQueryInsertInto(split);
		} else if (isUpdateSetQuery(split)) {
			newSplit = convertQueryUpdateSet(split);
		} else if (isExitQuery(split)) {
			newSplit = null;
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
		return (split.length > 3 && "CREATE".equalsIgnoreCase(split[0]) && "INDEX".equalsIgnoreCase(split[1])) || (split.length > 4 && "CREATE".equalsIgnoreCase(split[0]) && "UNIQUE".equalsIgnoreCase(split[1]) && "INDEX".equalsIgnoreCase(split[2]));
	}

	private static boolean isAlterTableQuery(String[] split) {
		return split.length > 3 && "ALTER".equalsIgnoreCase(split[0]) && "TABLE".equalsIgnoreCase(split[1]);
	}

	private static boolean isAlterTableIbisStoreQuery(String[] split) {
		return split.length > 4 && "ALTER".equalsIgnoreCase(split[0]) && "TABLE".equalsIgnoreCase(split[1]) && "IBISSTORE".equalsIgnoreCase(split[2]);
	}

	private static boolean isExitQuery(String[] split) {
		return split.length == 1 && "EXIT".equalsIgnoreCase(split[0]);
	}

	private static boolean isInsertIntoQuery(String[] split) {
		return split.length > 3 && "INSERT".equalsIgnoreCase(split[0]) && "INTO".equalsIgnoreCase(split[1]);
	}

	private static boolean isSelectOneWhereForUpdateQuery(String[] split) {
		// H2 requires primary key in the select clause. When the select clause contains only one field and the where clause is used, the field from the where clause is added to the select clause
		return split.length > 7 && "SELECT".equalsIgnoreCase(split[0]) && "FROM".equalsIgnoreCase(split[2]) && "WHERE".equalsIgnoreCase(split[4]) && "FOR".equalsIgnoreCase(split[split.length - 2]) && "UPDATE".equalsIgnoreCase(split[split.length - 1]);
	}

	private static boolean isUpdateSetQuery(String[] split) {
		return split.length > 3 && "UPDATE".equalsIgnoreCase(split[0]) && "SET".equalsIgnoreCase(split[2]);
	}

	private static String[] convertQuerySelectOneWhereForUpdate(String[] split) {
		List<String> newSplit = new ArrayList<>();
		for (int i = 0; i < split.length; i++) {
			if (i == 2) {
				newSplit.add(",");
				newSplit.add(split[5]);
			}
			newSplit.add(split[i]);
		}
		return newSplit.toArray(new String[0]);
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
			if (("ORDER".equalsIgnoreCase(split[i]) || "NOORDER".equalsIgnoreCase(split[i])) && !containsBracket(split, i + 1)) {
				// ignore
				i = i + 1;
			} else {
				newSplit.add(split[i]);
				if ("MAXVALUE".equalsIgnoreCase(split[i]) && (i + 1) < split.length && StringUtils.isNumeric(split[i + 1])) {
					BigInteger maxValue = new BigInteger(split[i + 1]);
					if (maxValue.compareTo(SEQUENCE_MAX_VALUE) > 0) {
						newSplit.add(SEQUENCE_MAX_VALUE_STRING);
						i++;
					}
				}
			}
		}
		return newSplit.toArray(new String[0]);
	}

	private static String[] convertQueryCreateTableIbisStore(String[] split) {
		List<String> newSplit = new ArrayList<>();
		for (int i = 0; i < split.length; i++) {
			if (isCreateTableOrIndexClause(split[i]) && !containsBracket(split, i + 1)) {
				// ignore
				i = i + 1;
			} else {
				newSplit.add(split[i]);
				if ("MESSAGEKEY".equalsIgnoreCase(split[i]) && (i + 4) < split.length && "NUMBER".equals(split[i + 1]) && "(".equals(split[i + 2]) && ")".equals(split[i + 4])) {
					newSplit.add("INT");
					newSplit.add("IDENTITY");
					i = i + 4;
				}
			}
		}
		return newSplit.toArray(new String[0]);
	}

	private static String[] convertQueryCreateTable(String[] split) {
		List<String> newSplit = new ArrayList<>();
		for (int i = 0; i < split.length; i++) {
			if (isCreateTableOrIndexClause(split[i]) && !containsBracket(split, i + 1)) {
				// ignore
				i = i + 1;
			} else {
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
		}
		return newSplit.toArray(new String[0]);
	}

	private static boolean isCreateTableOrIndexClause(String string) {
		return "LOGGING".equalsIgnoreCase(string) || "NOLOGGING".equalsIgnoreCase(string) || "COMPRESS".equalsIgnoreCase(string) || "NOCOMPRESS".equalsIgnoreCase(string) || "CACHE".equalsIgnoreCase(string) || "NOCACHE".equalsIgnoreCase(string) || "PARALLEL".equalsIgnoreCase(string) || "NOPARALLEL".equalsIgnoreCase(string) || "MONITORING".equalsIgnoreCase(string) || "NOMONITORING".equalsIgnoreCase(string);
	}

	private static boolean containsBracket(String[] split, int startPos) {
		for (int i = startPos; i < split.length; i++) {
			if ("(".equals(split[i]) || ")".equals(split[i]))
				return true;
		}
		return false;
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
			if (isCreateTableOrIndexClause(split[i]) && !containsBracket(split, i + 1)) {
				// ignore
				i = i + 1;
			} else if ("LOWER".equalsIgnoreCase(split[i]) && (i + 3) < split.length && "(".equals(split[i + 1]) && ")".equals(split[i + 3])) {
				newSplit.add(split[i + 2]);
				i = i + 3;
			} else {
				newSplit.add(split[i]);
			}
		}
		return newSplit.toArray(new String[0]);
	}

	private static String[] convertQueryInsertInto(String[] split) {
		List<String> newSplit = new ArrayList<>();
		for (int i = 0; i < split.length; i++) {
			if (("EMPTY_BLOB".equalsIgnoreCase(split[i]) || "EMPTY_CLOB".equalsIgnoreCase(split[i])) && (i + 2) < split.length && "(".equals(split[i + 1]) && ")".equals(split[i + 2])) {
				newSplit.add("''");
				i = i + 2;
			} else {
				newSplit.add(split[i]);
			}
		}
		return newSplit.toArray(new String[0]);
	}

	private static String[] convertQueryUpdateSet(String[] split) {
		List<String> newSplit = new ArrayList<>();
		for (int i = 0; i < split.length; i++) {
			if (("EMPTY_BLOB".equalsIgnoreCase(split[i]) || "EMPTY_CLOB".equalsIgnoreCase(split[i])) && (i + 2) < split.length && "(".equals(split[i + 1]) && ")".equals(split[i + 2])) {
				newSplit.add("''");
				i = i + 2;
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
