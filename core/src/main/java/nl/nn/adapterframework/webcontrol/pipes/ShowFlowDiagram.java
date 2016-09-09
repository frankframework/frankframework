/*
   Copyright 2016 Nationale-Nederlanden

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
package nl.nn.adapterframework.webcontrol.pipes;

import java.io.File;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.pipes.TimeoutGuardPipe;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.FileUtils;

import org.apache.commons.lang.StringUtils;

/**
 * ShowFlowDiagram.
 * 
 * @author Peter Leeuwenburgh
 * @version $Id$
 */

public class ShowFlowDiagram extends TimeoutGuardPipe {
	private File adapterFlowDir = new File(AppConstants.getInstance()
			.getResolvedProperty("flow.adapter.dir"));
	private File configFlowDir = new File(AppConstants.getInstance()
			.getResolvedProperty("flow.config.dir"));

	public String doPipeWithTimeoutGuarded(Object input,
			IPipeLineSession session) throws PipeRunException {
		String method = (String) session.get("method");
		if (method.equalsIgnoreCase("GET")) {
			return doGet(session);
		} else {
			throw new PipeRunException(this, getLogPrefix(session)
					+ "illegal value for method [" + method
					+ "], must be 'GET'");
		}
	}

	private String doGet(IPipeLineSession session) throws PipeRunException {
		String adapterName = null;
		String uri = (String) session.get("uri");
		if (StringUtils.isNotEmpty(uri)) {
			String[] split = uri.split("/");
			if (split.length > 2) {
				adapterName = split[2];
			}
		}
		File flowFile;
		if (StringUtils.isNotEmpty(adapterName)) {
			String adapterFileName = FileUtils
					.encodeFileName(java.net.URLDecoder.decode(adapterName))
					+ ".svg";
			flowFile = new File(adapterFlowDir, adapterFileName);
		} else {
			String configurationName = (String) session.get("configuration");
			if (StringUtils.isEmpty(configurationName)
					|| configurationName.equalsIgnoreCase("*ALL*")) {
				String configFileName = "_ALL_.svg";
				flowFile = new File(configFlowDir, configFileName);
			} else {
				String configFileName = FileUtils
						.encodeFileName(java.net.URLDecoder
								.decode(configurationName))
						+ ".svg";
				flowFile = new File(configFlowDir, configFileName);
			}
		}
		if (flowFile.exists()) {
			return flowFile.getPath();
		} else {
			return null;
		}
	}
}
