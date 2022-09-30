/*
   Copyright 2013 Nationale-Nederlanden

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
package nl.nn.adapterframework.statistics.parser;

/**
 * Helperclass used to parse Statistics-files.
 * 
 * @author  Gerrit van Brakel
 * @since   4.9.9
 */
public class Item {

	private String name;
	private String value;


	public void setName(String string) {
		name = string;
	}
	public String getName() {
		return name;
	}

	public void setValue(String string) {
		value = string;
	}
	public String getValue() {
		return value;
	}

}
