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
package nl.nn.adapterframework.statistics;

import java.util.Date;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.SenderException;

/**
 * Allows operations on iterations over all statistics keepers.
 * 
 * @author  Gerrit van Brakel
 * @since  
 */
public interface StatisticsKeeperIterationHandler<D> {

	public void configure() throws ConfigurationException;
	public D start(Date now, Date mainMark, Date detailMark) throws SenderException;
	public void end(D data) throws SenderException;
	public void handleStatisticsKeeper(D data, StatisticsKeeper sk) throws SenderException;
	public void handleScalar(D data, String name, ScalarMetricBase<?> meter) throws SenderException;
	public void handleScalar(D data, String scalarName, long value) throws SenderException;
	public void handleScalar(D data, String scalarName, Date value) throws SenderException;
	public D openGroup(D parentData, String name, String type) throws SenderException;
	public void  closeGroup(D data) throws SenderException;
}
