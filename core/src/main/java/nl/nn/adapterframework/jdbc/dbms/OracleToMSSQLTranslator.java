/*
   Copyright 2020 WeAreFrank!

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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.jdbc.JdbcException;
import nl.nn.adapterframework.jdbc.QueryExecutionContext;
import nl.nn.adapterframework.util.LogUtil;

/**
 * Class to translate Oracle queries to MS SQL Server.
 * 
 * @author Carlo Camiletti
 */
public class OracleToMSSQLTranslator {
	
	private static Logger log = LogUtil.getLogger(OracleToMSSQLTranslator.class);

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
		String[] newSplit = convertQuery(split);
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

	private static boolean isOracleKeyPresent(List<String> splitString, String key) {
		return splitString.stream()
    			.filter(s -> s.toUpperCase().contains(key)).findAny().isPresent();
	}
	
	private static String[] convertQuery(String[] split) {
		List<String> splitString = Arrays.asList(Arrays.copyOf(split, split.length));
		
		if (isOracleKeyPresent(splitString, OracleKeyWords.NEXTVAL.key())) {
			splitString = convertSequenceFunction(splitString);
		}
		if (isOracleKeyPresent(splitString, OracleKeyWords.CURRVAL.key())) {
			splitString = convertCurrvalSequenceQuery(splitString);
		}
		if (isOracleKeyPresent(splitString, OracleKeyWords.DUAL.key())) {
			splitString = convertQueryFromDual(splitString);
		}
		if (isOracleKeyPresent(splitString, OracleKeyWords.FOR_UPDATE.key())) { 
			splitString = convertQuerySelectOneWhereForUpdate(splitString);
		}
		if (isOracleKeyPresent(splitString, OracleKeyWords.EMPTY_BLOB.key())) {
			splitString = convertQueryClobBlobFuntion(splitString);
		}
		if (isOracleKeyPresent(splitString, OracleKeyWords.EMPTY_CLOB.key())) {
			splitString = convertQueryClobBlobFuntion(splitString);
		}
		if (isOracleKeyPresent(splitString, OracleKeyWords.SYSDATE.key())) {
			splitString = convert(splitString, OracleKeyWords.SYSDATE.key(), MsSqlServerDbmsSupport.GET_DATE);
		}
		if (isOracleKeyPresent(splitString, OracleKeyWords.SYSTIMESTAMP.key())) {
			splitString = convert(splitString, OracleKeyWords.SYSTIMESTAMP.key(), MsSqlServerDbmsSupport.CURRENT_TIMESTAMP);
		}
		return splitString.toArray(new String[splitString.size()]);
	}

	private static List<String> convert(List<String> splitString, String oracleFunction, String mssqlFunction) {
        List<String> remove = splitString.stream()
    			.filter(s -> s.contains(oracleFunction))
    			.map(s -> s.replaceAll("[^.^_^a-zA-Z0-9\\s+]", ""))
    			.collect(Collectors.toList());

        remove.forEach(rem -> {
        	int index = splitString.indexOf(rem);
        	splitString.set(index, mssqlFunction);
        });
        
        return splitString;
	}

	private static List<String> convertQuerySelectOneWhereForUpdate(List<String> splitString) {
		List<String> query = new LinkedList<String>(splitString);
		try {
			query.remove(splitString.size()-1);
			query.remove(splitString.size()-2);
			int index = splitString.indexOf("WHERE");
			query.add(index, MsSqlServerDbmsSupport.WITH_UPDLOCK_ROWLOCK);
		}catch(Exception e) {
			System.out.println(e);
		}
		return query;
	}
	
	private static List<String> convertQueryClobBlobFuntion(List<String> splitString) {
		String[] newarr = splitString.toArray(new String[splitString.size()]);
		List<String> newSplit = new ArrayList<>();
		for (int i = 0; i < newarr.length; i++) {
			if (("EMPTY_BLOB".equalsIgnoreCase(newarr[i]) || "EMPTY_CLOB".equalsIgnoreCase(newarr[i])) && (i + 2) < newarr.length && "(".equals(newarr[i + 1]) && ")".equals(newarr[i + 2])) {
				newSplit.add(MsSqlServerDbmsSupport.DEFAULT_BLOB_VALUE);
				i = i + 2;
			} else {
				newSplit.add(newarr[i]);
			}
		}
		return newSplit;
	}

	private static List<String> convertSequenceFunction(List<String> splitString) {
        List<String> remove = splitString.stream()
    			.filter(s -> s.contains(".NEXTVAL"))
    			.map(s -> s.replaceAll("[^.^_^a-zA-Z0-9\\s+]", ""))
    			.collect(Collectors.toList());

        remove.forEach(rem -> {
        	int index = splitString.indexOf(rem);
        	splitString.set(index, MsSqlServerDbmsSupport.NEXT_VALUE_FOR.concat(rem.split("\\.")[0]));
        });
        
        return splitString;
	}
	
	private static List<String> convertCurrvalSequenceQuery(List<String> splitString) {
        String sequenceName = splitString.stream()
    			.filter(s -> s.contains(".CURRVAL"))
    			.map(s -> s.replaceAll("[^.^_^a-zA-Z0-9\\s+]", "")).findFirst().get();
        
        String query = MsSqlServerDbmsSupport.SELECT_CURRENT_VALUE.concat("'" + sequenceName.split("\\.")[0] + "'");
        String queryForSplit = query.replaceAll("([,;\\(\\)=])", " $1 ").trim();
        String[] split = queryForSplit.split("\\s+(?=([^']*'[^']*')*[^']*$)");
        return Arrays.asList(split);
	}	

	private static List<String> convertQueryFromDual(List<String> splitString) {
		List<String> query = new LinkedList<String>(splitString);
		
		for (int i=0; i<query.size(); i++) {
			String item = query.get(i);
			if (item.equalsIgnoreCase("DUAL")) {
				String part = query.remove(i--); // remove DUAL
				String prevPart = query.remove(i--); // remove FROM, assuming that DUAL is the only table in the query.
				if (log.isDebugEnabled()) log.debug("removed from query ["+prevPart+" "+part+"]");
			}
		}
		return query;
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
