/*
   Copyright 2016, 2020 Nationale-Nederlanden

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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationUtils;
import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.jms.JmsRealm;
import nl.nn.adapterframework.jms.JmsRealmFactory;
import nl.nn.adapterframework.pipes.TimeoutGuardPipe;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.XmlBuilder;

/**
 * Upload a configuration.
 * 
 * @author Peter Leeuwenburgh
 */

public class UploadConfig extends TimeoutGuardPipe {
	private IbisContext ibisContext;
	private static final String AUTO_RELOAD = "autoReload";
	private static final String ACTIVE_CONFIG = "activeConfig";
	private static final AppConstants APP_CONSTANTS = AppConstants.getInstance();
	private static final boolean CONFIG_AUTO_DB_CLASSLOADER = APP_CONSTANTS.getBoolean("configurations.autoDatabaseClassLoader", false);

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		ibisContext = getAdapter().getConfiguration().getIbisManager().getIbisContext();
	}

	@Override
	public PipeRunResult doPipeWithTimeoutGuarded(Message input, PipeLineSession session) throws PipeRunException {
		String method = (String) session.get("method");
		if ("GET".equalsIgnoreCase(method)) {
			return new PipeRunResult(getSuccessForward(), doGet(session));
		} else if ("POST".equalsIgnoreCase(method)) {
			return new PipeRunResult(getSuccessForward(), doPost(session));
		} else {
			throw new PipeRunException(this, getLogPrefix(session) + "Illegal value for method [" + method + "], must be 'GET' or 'POST'");
		}
	}

	private String doGet(PipeLineSession session) throws PipeRunException {
		String dtapStage = APP_CONSTANTS.getResolvedProperty("dtap.stage");
		session.put(ACTIVE_CONFIG, "on");
		if ("DEV".equalsIgnoreCase(dtapStage) || "TEST".equalsIgnoreCase(dtapStage)) {
			session.put(AUTO_RELOAD, "on");
		} else {
			session.put(AUTO_RELOAD, "off");
		}
		return retrieveFormInput();
	}

	private String doPost(PipeLineSession session) throws PipeRunException {
		String multipleConfigs = (String) session.get("multipleConfigs");
		String fileName = (String) session.get("fileName");
		String result;
		Object file = session.get("file");
		if (file == null) {
			throw new PipeRunException(this,
					getLogPrefix(session) + "Nothing to send or test");
		}

		if ("on".equals(multipleConfigs)) {
			try {
				result = processZipFile(session);
			} catch (IOException e) {
				throw new PipeRunException(this, getLogPrefix(session)
						+ "Error while processing zip file", e);
			}
		} else {
			result = processJarFile(session, fileName, "file");
		}

		session.put("result", result);
		return "<dummy/>";
	}

	private String processZipFile(PipeLineSession session)
			throws PipeRunException, IOException {
		StringBuilder result = new StringBuilder();
		Object formFile = session.get("file");
		if (formFile instanceof InputStream) {
			InputStream inputStream = (InputStream) formFile;
			try {
				if (inputStream.available() > 0) {
					ZipInputStream archive = new ZipInputStream(inputStream);
					int counter = 1;
					for (ZipEntry entry = archive
							.getNextEntry(); entry != null; entry = archive
									.getNextEntry()) {
						String entryName = entry.getName();
						int size = (int) entry.getSize();
						if (size > 0) {
							byte[] b = new byte[size];
							int rb = 0;
							while ((size - rb) > 0) {
								int chunk = archive.read(b, rb, size - rb);
								if (chunk == -1) {
									break;
								}
								rb += chunk;
							}
							ByteArrayInputStream bais = new ByteArrayInputStream(b,
									0, rb);
							String fileNameSessionKey = "file_zipentry" + counter;
							session.put(fileNameSessionKey, bais);
							if (result.length()!=0){
								result.append("\n" + new String(new char[32]).replace("\0", "-") + "\n"); //Creates separator between config results
							}
							result.append(processJarFile(session,
									entryName, fileNameSessionKey));
							session.remove(fileNameSessionKey);
						}
						archive.closeEntry();
						counter++;
					}
					archive.close();
				} else {
					throw new PipeRunException(this, getLogPrefix(session)
							+ "Cannot read zip file");
				}
			} finally {
				if (inputStream != null) {
					inputStream.close();
				}
			}
		}
		return result.toString();
	}

	

	private String processJarFile(PipeLineSession session, String fileName, String fileSessionKey) throws PipeRunException {
		String formJmsRealm = (String) session.get("jmsRealm");
		String activeConfig = (String) session.get(ACTIVE_CONFIG);
		boolean isActiveConfig = "on".equals(activeConfig);
		String autoReload = (String) session.get(AUTO_RELOAD);
		boolean isAutoReload = "on".equals(autoReload);
		String remoteUser = (String) session.get("principal");
		InputStream inputStream = (InputStream) session.get(fileSessionKey);

		JmsRealm jmsRealm = JmsRealmFactory.getInstance().getJmsRealm(formJmsRealm);
		String datasource = jmsRealm.getDatasourceName();

		try {
			// convert inputStream to byteArray so it can be read twice
			byte[] bytes = IOUtils.toByteArray(inputStream);

			String[] buildInfo = ConfigurationUtils.retrieveBuildInfo(new ByteArrayInputStream(bytes));
			String buildInfoName = buildInfo[0];
			String buildInfoVersion = buildInfo[1];
			if (StringUtils.isEmpty(buildInfoName) || StringUtils.isEmpty(buildInfoVersion)) {
				throw new PipeRunException(this, getLogPrefix(session) + "Cannot retrieve BuildInfo name and version");
			}
			if (ConfigurationUtils.addConfigToDatabase(ibisContext, datasource, isActiveConfig, isAutoReload, buildInfoName, buildInfoVersion, fileName, new ByteArrayInputStream(bytes), remoteUser)) {
				if (CONFIG_AUTO_DB_CLASSLOADER && isAutoReload && ibisContext.getIbisManager().getConfiguration(buildInfoName) == null) {
					ibisContext.reload(buildInfoName);
				}
				return ("OK\n" + "Name: " + buildInfoName + "\nVersion: " + buildInfoVersion);
			}
		} catch (Exception e) {
			throw new PipeRunException(this, getLogPrefix(session) + "Error occured on adding config to database", e);
		}
		return "NOT_OK";
	}

	private String retrieveFormInput() {
		List<String> jmsRealms = JmsRealmFactory.getInstance()
				.getRegisteredRealmNamesAsList();
		if (jmsRealms.isEmpty())
			jmsRealms.add("no realms defined");
		XmlBuilder jmsRealmsXML = new XmlBuilder("jmsRealms");
		for (int i = 0; i < jmsRealms.size(); i++) {
			if (StringUtils.isNotEmpty(JmsRealmFactory.getInstance()
					.getJmsRealm(jmsRealms.get(i)).getDatasourceName())) {
				XmlBuilder jmsRealmXML = new XmlBuilder("jmsRealm");
				jmsRealmXML.setValue(jmsRealms.get(i));
				jmsRealmsXML.addSubElement(jmsRealmXML);
			}
		}
		return jmsRealmsXML.toXML();
	}
}
