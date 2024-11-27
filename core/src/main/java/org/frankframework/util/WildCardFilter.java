/*
   Copyright 2013 Nationale-Nederlanden, 2021 WeAreFrank!

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
package org.frankframework.util;

import java.io.File;
import java.io.FilenameFilter;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * Implementation of a FilenameFilter to support wildcards. <br/>
 * <b>use:</b><br/>
 * <pre>{@code
 * WildCardFilter filter = new WildCardFilter("*.java");
 * File currDir = new File(".");
 * String files[] = currDir.list(filter);
 * }</pre>
 * <br/>
 * Examples:
 * <ul>
 * <li>*.java</li>
 * <li>in*.log</li>
 * <li>data*.*</li>
 * </ul>
 *
 * @author Johan Verrips IOS
 **/
public class WildCardFilter implements FilenameFilter {

	String wildPattern = null;
	Vector<String> pattern = new Vector<>();

	private static final String FIND = "find";
	private static final String EXPECT = "expect";
	private static final String ANYTHING = "anything";
	private static final String NOTHING = "nothing";

	public WildCardFilter(String wildString) {
		wildPattern = wildString;

		// ensure wildString is lowercase for all testing
		wildString = wildString.toLowerCase();

		// remove duplicate asterisks
		int i = wildString.indexOf("**");
		while(i >= 0) {
			wildString = wildString.substring(0, i + 1) + wildString.substring(i + 2);
			i = wildString.indexOf("**");
		}

		// parse the input string
		StringTokenizer tokens = new StringTokenizer(wildString, "*", true);
		String token = null;
		while(tokens.hasMoreTokens()) {
			token = tokens.nextToken();

			if("*".equals(token)) {
				pattern.addElement(FIND);
				if(tokens.hasMoreTokens()) {
					token = tokens.nextToken();
					pattern.addElement(token);
				} else {
					pattern.addElement(ANYTHING);
				}
			} else {
				pattern.addElement(EXPECT);
				pattern.addElement(token);
			}
		}

		if(!"*".equals(token)) {
			pattern.addElement(EXPECT);
			pattern.addElement(NOTHING);
		}

	}

	@Override
	public boolean accept(File dir, String name) {
		// allow directories to match all patterns
		// not sure if this is the best idea, but
		// suits my needs for now
		if(dir != null) {
			String path = dir.getPath();
			if(!path.endsWith("/") && !path.endsWith("\\")) {
				path += File.separator;
			}
			File tempFile = new File(path, name);

			if(tempFile.isDirectory()) {
				return true;
			}
		}

		// ensure name is lowercase for all testing

		name = name.toLowerCase();

		// start processing the pattern vector

		boolean acceptName = true;

		String command = null;
		String param = null;

		int currPos = 0;
		int cmdPos = 0;

		while(cmdPos < pattern.size()) {
			command = pattern.elementAt(cmdPos);
			param = pattern.elementAt(cmdPos + 1);

			if(command.equals(FIND)) {
				// if we are to find 'anything'
				// then we are done

				if(param.equals(ANYTHING)) {
					break;
				}

				// otherwise search for the param
				// from the curr pos

				int nextPos = name.indexOf(param, currPos);
				if(nextPos >= 0) {
					// found it
					currPos = nextPos + param.length();
				} else {
					acceptName = false;
					break;
				}
			} else {
				if(command.equals(EXPECT)) {
					// if we are to expect 'nothing'
					// then we MUST be at the end of the string

					if(param.equals(NOTHING)) {
						if(currPos != name.length()) {
							acceptName = false;
						}

						// since we expect nothing else,
						// we must finish here

						break;
					} else {
						// otherwise, check if the expected string
						// is at our current position

						int nextPos = name.indexOf(param, currPos);
						if(nextPos != currPos) {
							acceptName = false;
							break;
						}

						// if we've made it this far, then we've
						// found what we're looking for

						currPos += param.length();
					}
				}
			}

			cmdPos += 2;
		}

		return acceptName;
	}

	public String toPattern() {
		StringBuilder out = new StringBuilder();

		int i = 0;
		while(i < pattern.size()) {
			out.append("(");
			out.append(pattern.elementAt(i));
			out.append(" ");
			out.append(pattern.elementAt(i + 1));
			out.append(") ");

			i += 2;
		}

		return out.toString();
	}

	@Override
	public String toString() {
		return wildPattern;
	}
}
