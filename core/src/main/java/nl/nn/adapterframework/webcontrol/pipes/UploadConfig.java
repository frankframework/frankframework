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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.jdbc.FixedQuerySender;
import nl.nn.adapterframework.jms.JmsRealmFactory;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.pipes.TimeoutGuardPipe;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.XmlBuilder;

import org.apache.commons.lang.StringUtils;

/**
 * Upload a configuration.
 * 
 * @author Peter Leeuwenburgh
 */

public class UploadConfig extends TimeoutGuardPipe {
	private IbisContext ibisContext;
	private static final String DUMMY = "dummy";
	private static final String AUTO_RELOAD = "autoReload";
	private static final String ACTIVE_CONFIG = "activeConfig";
	

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		ibisContext = ((Adapter) getAdapter()).getConfiguration()
				.getIbisManager().getIbisContext();
	}
	
	@Override
	public String doPipeWithTimeoutGuarded(Object input,
			IPipeLineSession session) throws PipeRunException {
		String method = (String) session.get("method");
		if ("GET".equalsIgnoreCase(method)) {
			return doGet(session);
		} else if ("POST".equalsIgnoreCase(method)) {
			return doPost(session);
		} else {
			throw new PipeRunException(this, getLogPrefix(session)
					+ "Illegal value for method [" + method
					+ "], must be 'GET' or 'POST'");
		}
	}

	private String doGet(IPipeLineSession<String, String> session) throws PipeRunException {
		String otapStage = AppConstants.getInstance().getResolvedProperty("otap.stage");
		session.put(ACTIVE_CONFIG, "on");
		if ("DEV".equalsIgnoreCase(otapStage) || "TEST".equalsIgnoreCase(otapStage)) { 
			session.put(AUTO_RELOAD, "on"); 
		} else {
			session.put(AUTO_RELOAD, "off"); 	
		}
		return retrieveFormInput();
	}
	
	private String doPost(IPipeLineSession<String, String> session) throws PipeRunException {
		String multipleConfigs = session.get("multipleConfigs");
		String fileName = session.get("fileName");
		String name = session.get("name");
		String version = session.get("version");
		ConfigData configData = new ConfigData(multipleConfigs, fileName, name, version);
		String result;
		
		if (!"on".equals(multipleConfigs)) {
		processMultipleConfigs(configData, session);
		}
		
		Object file = session.get("file");
		if (file == null) {
			throw new PipeRunException(this, getLogPrefix(session)
					+ "Nothing to send or test");
		}
		
		if ("on".equals(multipleConfigs)) {
			try {
				result = processZipFile(session);
			} catch (IOException e) {
				throw new PipeRunException(this, getLogPrefix(session)
						+ "Error while processing zip file", e);
			}
		} else {
			result = processJarFile(session, name, version, fileName, "file");
		}
		
		session.put("result", result);
		return "<dummy/>";
	}

	private String[] splitFilename(String fileName) {
		String name = null;
		String version = null;
		if (StringUtils.isNotEmpty(fileName)) {
			int i = fileName.lastIndexOf('.');
			if (i != -1) {
				name = fileName.substring(0, i);
				int j = name.lastIndexOf('-');
				if (j != -1) {
					name = name.substring(0, j);
					j = name.lastIndexOf('-');
					if (j != -1) {
						name = fileName.substring(0, j);
						version = fileName.substring(j + 1, i);
					}
				}
			}
		}
		return new String[] { name, version };
	}
	
	private String processZipFile(IPipeLineSession session)
			throws PipeRunException, IOException {
		StringBuilder result = new StringBuilder();
		Object formFile = session.get("file");
		if (formFile != null && (formFile instanceof InputStream)) {
				InputStream inputStream = (InputStream) formFile;
				if (inputStream.available() > 0) {
					ZipInputStream archive = new ZipInputStream(inputStream);
					int counter = 1;
					for (ZipEntry entry = archive.getNextEntry(); entry != null; entry = archive
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
							ByteArrayInputStream bais = new ByteArrayInputStream(
									b, 0, rb);
							String fileNameSessionKey = "file_zipentry"
									+ counter;
							session.put(fileNameSessionKey, bais);
							if (result.toString().isEmpty()) {
								result.append("\n");
							}
							String name = "";
							String version = "";
							String[] fnArray = splitFilename(entryName);
							if (fnArray[0] != null) {
								name = fnArray[0];
							}
							if (fnArray[1] != null) {
								version = fnArray[1];
							}
							result.append(entryName);
							result.append(":");
							result.append(processJarFile(session, name, version,
											entryName, fileNameSessionKey));
							session.remove(fileNameSessionKey);
						}
						archive.closeEntry();
						counter++;
					}
					archive.close();
				}	
		}
		return result.toString();
	}
	
	private String selectConfigQuery(IPipeLineSession<?, ?> session, String name) throws PipeRunException {
		String formJmsRealm = (String) session.get("jmsRealm");
		String result = "";
		FixedQuerySender qs = (FixedQuerySender) ibisContext
				.createBeanAutowireByName(FixedQuerySender.class);
		try {
			qs.setName("QuerySender");
			qs.setJmsRealm(formJmsRealm);
			qs.setQueryType("select");
			qs.setQuery("SELECT COUNT(*) FROM IBISCONFIG WHERE NAME=?");
			Parameter param = new Parameter();
			param.setName("name");
			param.setValue(name);
			qs.addParameter(param);
			qs.setScalar(true);
			qs.configure();
			qs.open();
			ParameterResolutionContext prc = new ParameterResolutionContext(
					DUMMY, session);
			result = qs.sendMessage(DUMMY, DUMMY, prc);
		} catch (Exception t) {
			throw new PipeRunException(this, getLogPrefix(session)
					+ "Error occured on executing jdbc query", t);
		} finally {
			qs.close();
		}
		return result;
	}
	
	private String deleteConfigQuery(IPipeLineSession<?, ?> session, String name, String formJmsRealm, String version) throws PipeRunException {
		FixedQuerySender qs = (FixedQuerySender) ibisContext
				.createBeanAutowireByName(FixedQuerySender.class);
		String result = "";
		try {
			qs.setName("QuerySender");
			qs.setJmsRealm(formJmsRealm);
			qs.setQuery("DELETE FROM IBISCONFIG WHERE NAME = ? AND VERSION = ?");
			Parameter param = new Parameter();
			param.setName("name");
			param.setValue(name);
			qs.addParameter(param);
			param = new Parameter();
			param.setName("version");
			param.setValue(version);
			qs.addParameter(param);
			qs.setScalar(true);
			qs.configure();
			qs.open();
			ParameterResolutionContext prc = new ParameterResolutionContext(
					DUMMY, session);
			result = qs.sendMessage(DUMMY, DUMMY, prc);
		} catch (Exception t) {
			throw new PipeRunException(this, getLogPrefix(session)
					+ "Error occured on executing jdbc query", t);
		} finally {
			qs.close();
		}
		return result;
	}
	
	private String activeConfigQuery(IPipeLineSession<?, ?> session, String name, String formJmsRealm, String result) throws PipeRunException {
		FixedQuerySender qs = (FixedQuerySender) ibisContext
				.createBeanAutowireByName(FixedQuerySender.class);
		try {
			qs.setName("QuerySender");
			qs.setJmsRealm(formJmsRealm);
			qs.setQueryType("update");
			qs.setQuery("UPDATE IBISCONFIG SET ACTIVECONFIG = 'FALSE' WHERE NAME=?");
			Parameter param = new Parameter();
			param.setName("name");
			param.setValue(name);
			qs.addParameter(param);
			qs.setScalar(true);
			qs.configure();
			qs.open();
			ParameterResolutionContext prc = new ParameterResolutionContext(
					DUMMY, session);
			result = qs.sendMessage(DUMMY, DUMMY, prc);
		} catch (Exception t) {
			throw new PipeRunException(this, getLogPrefix(session)
					+ "Error occured on executing jdbc query", t);
		} finally {
			qs.close();
		}
		return result;
	}
	
	private ConfigData processMultipleConfigs(ConfigData configData, IPipeLineSession<String, String> session) throws PipeRunException {
			if (StringUtils.isEmpty(configData.getName()) && StringUtils.isEmpty(configData.getVersion())) {
				String[] fnArray = splitFilename(configData.getFileName());
				if (fnArray[0] != null) {
					configData.setName(fnArray[0]);
				}
				if (fnArray[1] != null) {
					configData.setVersion(fnArray[1]);
				}
			}

			if (StringUtils.isEmpty(configData.getName())) {
				throw new PipeRunException(this, getLogPrefix(session)
						+ "Cannot determine configuration name");
			}

			if (StringUtils.isEmpty(configData.getVersion())) {
				throw new PipeRunException(this, getLogPrefix(session)
						+ "Cannot determine configuration version");
			}
		return configData;
	}
	
	private FixedQuerySender setAutoReload(String autoReload, FixedQuerySender qs) {
		Parameter param = new Parameter();
		param.setName(AUTO_RELOAD);
		if ("on".equals(autoReload)) {
			param.setValue("true");	
		} else {
			param.setValue("false");
		}
		qs.addParameter(param);
		return qs;
	}
	
	private FixedQuerySender setActiveConfig(String activeConfig, FixedQuerySender qs) {
		Parameter param = new Parameter();
		param.setName(ACTIVE_CONFIG);
		if ("on".equals(activeConfig)) {
			param.setValue("true");	
		} else {
			param.setValue("false");
		}
		qs.addParameter(param);
		return qs;
	}
	
	private String processJarFile(IPipeLineSession<?, ?> session, String name,
			String version, String fileName, String fileNameSessionKey)
			throws PipeRunException {
		String formJmsRealm = (String) session.get("jmsRealm");
		String activeConfig = (String) session.get(ACTIVE_CONFIG);
		String autoReload = (String) session.get(AUTO_RELOAD);
		String result = selectConfigQuery(session, name);
		
		if ("on".equals(activeConfig)) {
			activeConfigQuery(session, name, formJmsRealm, result);
		}
		if (Integer.parseInt(result) > 0) {
			deleteConfigQuery(session, name, formJmsRealm, version);
		}

		String remoteUser = (String) session.get("principal");

		FixedQuerySender qs = (FixedQuerySender) ibisContext
				.createBeanAutowireByName(FixedQuerySender.class);
		try {
			qs.setName("QuerySender");
			qs.setJmsRealm(formJmsRealm);
			qs.setQueryType("insert");
			if (StringUtils.isEmpty(remoteUser)) {
				qs.setQuery("INSERT INTO IBISCONFIG (NAME, VERSION, FILENAME, CONFIG, CRE_TYDST, ACTIVECONFIG, AUTORELOAD) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, ?, ?)");
			} else {
				qs.setQuery("INSERT INTO IBISCONFIG (NAME, VERSION, FILENAME, CONFIG, CRE_TYDST, RUSER, ACTIVECONFIG, AUTORELOAD) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, ?, ?, ?)");
			}
			Parameter param = new Parameter();
			param.setName("name");
			param.setValue(name);
			qs.addParameter(param);
			param = new Parameter();
			param.setName("version");
			param.setValue(version);
			qs.addParameter(param);
			param = new Parameter();
			param.setName("fileName");
			param.setValue(fileName);
			qs.addParameter(param);
			param = new Parameter();
			param.setName("config");
			param.setSessionKey(fileNameSessionKey);
			param.setType(Parameter.TYPE_INPUTSTREAM);
			qs.addParameter(param);
			if (StringUtils.isNotEmpty(remoteUser)) {
				param = new Parameter();
				param.setName("ruser");
				param.setValue(remoteUser);
				qs.addParameter(param);
			}
			setActiveConfig(activeConfig, qs);
			setAutoReload(autoReload, qs);
			
			qs.configure();
			qs.open();
			ParameterResolutionContext prc = new ParameterResolutionContext(
					DUMMY, session);
			result = qs.sendMessage(DUMMY, DUMMY, prc);
		} catch (Exception t) {
			throw new PipeRunException(this, getLogPrefix(session)
					+ "Error occured on executing jdbc query", t);
		} finally {
			qs.close();
		}
		return result;
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


class ConfigData {
	private String multipleConfigs;
	private String fileName;
	private String name;
	private String version;
	
	public ConfigData(String multipleConfigs, String fileName, String name, String version){
		this.multipleConfigs = multipleConfigs;
		this.fileName = fileName;
		this.name = name;
		this.version = version;
	}

	public String getMultipleConfigs() {
		return multipleConfigs;
	}

	public String getFileName() {
		return fileName;
	}

	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}

	public String getVersion() {
		return version;
	}
	
	public void setVersion(String version) {
		this.version = version;
	}
}