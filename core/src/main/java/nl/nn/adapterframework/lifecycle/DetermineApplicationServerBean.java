/*
   Copyright 2020 Nationale-Nederlanden

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
package nl.nn.adapterframework.lifecycle;

import java.io.File;

import javax.servlet.ServletContext;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.ServletContextAware;

import nl.nn.adapterframework.configuration.ApplicationWarnings;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.LogUtil;

@IbisInitializer
public class DetermineApplicationServerBean implements ServletContextAware {

	private ServletContext servletContext;
	private Logger log = LogUtil.getLogger(this);
	private ServletManager servletManager;

	@Override
	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;

		setUploadPathInServletContext();
		checkSecurityConstraintEnabled();
	}

	@Autowired
	public void setServletManager(ServletManager servletManager) {
		this.servletManager = servletManager;
	}

	private void checkSecurityConstraintEnabled() {
		AppConstants appConstants = AppConstants.getInstance();
		boolean isDtapStageLoc = "LOC".equalsIgnoreCase(appConstants.getProperty("dtap.stage"));
		if(appConstants.getBoolean("security.constraint.warning", !isDtapStageLoc) && !servletManager.isWebSecurityEnabled()) {
			ApplicationWarnings.add(log, "unsecure Frank! application, authentication has been disabled!");
		}
	}

	private void setUploadPathInServletContext() {
		try {
			// set the directory for struts upload, that is used for instance in 'test a pipeline'
			String path=AppConstants.getInstance().getResolvedProperty("upload.dir");
			// if the path is not found
			if (StringUtils.isEmpty(path)) {
				path="/tmp";
			}
			log.debug("setting path for Struts file-upload to ["+path+"]");
			File tempDirFile = new File(path);
			servletContext.setAttribute("javax.servlet.context.tempdir",tempDirFile);
		} catch (Exception e) {
			log.error("Could not set servlet context attribute 'javax.servlet.context.tempdir' to value of ${upload.dir}",e);
		}
	}
}
