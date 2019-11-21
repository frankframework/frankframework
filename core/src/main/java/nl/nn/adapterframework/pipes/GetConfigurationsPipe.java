/*
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

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.doc.IbisDoc;

/**
 * Gets all configurations.
 *
 * @author  Tom van der Heijden
 * @since   7.5
 */
public class GetConfigurationsPipe extends FixedForwardPipe {
	
	private String configurationState = "loaded";
	
	@Override
	public PipeRunResult doPipe(Object input, IPipeLineSession session) {
		StringBuilder result = new StringBuilder();
		for (Configuration configuration : this.getAdapter().getConfiguration().getIbisManager().getConfigurations()) {
			if ("original".equals(configurationState)) {
				result.append(configuration.getOriginalConfiguration());
			} else {
				result.append(configuration.getLoadedConfiguration());
			}
		}
		return new PipeRunResult(getForward(), result.toString());
	}
	
	@IbisDoc({"You can choose if you want the loaded configurations or the original configurations by using either: 'loaded' or 'original'.","loaded"})
	public void setConfigurationState(String configurationState) {
		this.configurationState = configurationState;
	}
	
	public String getConfigurationState() {
		return configurationState;
	}

}
