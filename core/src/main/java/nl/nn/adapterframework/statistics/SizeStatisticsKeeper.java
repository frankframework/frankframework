/*
   Copyright 2013 Nationale-Nederlanden, 2022 WeAreFrank!

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
package nl.nn.adapterframework.statistics;

/**
 * @author  Gerrit van Brakel
 * @since  
 */
public class SizeStatisticsKeeper extends StatisticsKeeper {

	private static final String statConfigKey="Statistics.size.boundaries";
    public static final String DEFAULT_BOUNDARY_LIST="10000,100000,1000000";

    public SizeStatisticsKeeper(String name) {
		super(name,BigBasics.class, statConfigKey, DEFAULT_BOUNDARY_LIST);
	}

	@Override
	public String getQuantity() {
		return "size";
	}
	@Override
	public String getUnits() {
		return "B";
	}

}
