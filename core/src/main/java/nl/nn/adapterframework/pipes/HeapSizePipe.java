/*
   Copyright 2018 Nationale-Nederlanden

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
package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.util.ProcessMetrics;

/**
 * Pipe that returns metrics about current memory usage
 * 
 * 	Output example:
 * 
 * 	<processMetrics>
 * 		<properties>
 * 			<property name="freeMemory">201M</property>
 * 			<property name="totalMemory">245M</property>
 * 			<property name="heapSize">43M</property>
 * 			<property name="currentTime">2018-12-24 14:27:29.730</property>
 * 		</properties>
 * 	</processMetrics>

 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Pipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setAlgorithm(String) algorithm}</td><td>name of the Pipe</td><td>HmacSHA256</td></tr>
 * <tr><td>{@link #setEncoding(String) encoding}</td><td>name of the Pipe</td><td>ISO8859_1</td></tr>
 * <tr><td>{@link #setSecret(String) secret}</td><td>the secret to hash with</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setAuthAlias(String) authAlias}</td><td>AuthAlias to retrieve the secret from (password field).</td><td>&nbsp;</td></tr>
 * </table>
 * <p><b>NOTE:</b> You can also retrieve the secret or authAlias from a parameter.</p>
 * 
 * @author	Laurens Mäkel
 */

public class HeapSizePipe extends FixedForwardPipe {

	public PipeRunResult doPipe(Object input, IPipeLineSession session) throws PipeRunException {	
		return new PipeRunResult(getForward(), ProcessMetrics.toXml());
	}

}