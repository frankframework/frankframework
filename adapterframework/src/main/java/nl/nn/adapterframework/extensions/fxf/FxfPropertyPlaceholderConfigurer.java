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
package nl.nn.adapterframework.extensions.fxf;

import java.io.File;
import java.util.Properties;

import nl.nn.adapterframework.configuration.AppConstantsPropertyPlaceholderConfigurer;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.log4j.Logger;

/**
 * Initialise the fxf.dir property when not already available from the
 * AppConstants and make it available to the Ibis configuration and the Spring
 * configuration.
 * 
 * @author Jaco de Groot
 */
public class FxfPropertyPlaceholderConfigurer
		extends AppConstantsPropertyPlaceholderConfigurer {
	protected Logger log = LogUtil.getLogger(this);

	protected void convertProperties(Properties props) {
		String fxfDir = appConstants.getResolvedProperty("fxf.dir");
		if (fxfDir == null) {
			// Use default location, see was.policy too
			fxfDir = System.getProperty("APPSERVER_ROOT_DIR");
			if (fxfDir != null) {
				fxfDir = fxfDir + File.separator + "fxf-work";
			}
		}
		if (fxfDir != null) {
			if (!new File(fxfDir).isDirectory()) {
				log.debug("Could not find FxF directory: " + fxfDir);
				fxfDir = null;
			} else {
				log.debug("FxF directory: " + fxfDir);
				appConstants.setProperty("fxf.dir", fxfDir);
				props.put("fxf.dir", fxfDir);
			}
		}
	}

}
