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
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.jdbc.FixedQuerySender;
import nl.nn.adapterframework.jms.JmsRealmFactory;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.pipes.TimeoutGuardPipe;
import nl.nn.adapterframework.util.XmlBuilder;

import org.apache.commons.lang.StringUtils;

/**
 * Upload a configuration.
 * 
 * @author Peter Leeuwenburgh
 */

public class UploadConfig extends TimeoutGuardPipe {
	IbisContext ibisContext;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		ibisContext = ((Adapter) getAdapter()).getConfiguration()
				.getIbisManager().getIbisContext();
	}

	public String doPipeWithTimeoutGuarded(Object input,
			IPipeLineSession session) throws PipeRunException {
		String method = (String) session.get("method");
		if (method.equalsIgnoreCase("GET")) {
			return doGet(session);
		} else if (method.equalsIgnoreCase("POST")) {
			return doPost(session);
		} else {
			throw new PipeRunException(this, getLogPrefix(session)
					+ "Illegal value for method [" + method
					+ "], must be 'GET' or 'POST'");
		}
	}

	private String doGet(IPipeLineSession session) throws PipeRunException {
		return retrieveFormInput(session);
	}

	private String doPost(IPipeLineSession session) throws PipeRunException {
		String fileName = (String) session.get("fileName");
		String name = (String) session.get("name");
		String version = (String) session.get("version");
		String multipleConfigs = (String) session.get("multipleConfigs");

		if (!"on".equals(multipleConfigs)) {
			if (StringUtils.isEmpty(name) && StringUtils.isEmpty(version)) {
				String[] fnArray = splitFilename(fileName);
				if (fnArray[0] != null) {
					name = fnArray[0];
				}
				if (fnArray[1] != null) {
					version = fnArray[1];
				}
			}

			if (StringUtils.isEmpty(name)) {
				throw new PipeRunException(this, getLogPrefix(session)
						+ "Cannot determine configuration name");
			}

			if (StringUtils.isEmpty(version)) {
				throw new PipeRunException(this, getLogPrefix(session)
						+ "Cannot determine configuration version");
			}
		}

		Object file = session.get("file");
		if (file == null) {
			throw new PipeRunException(this, getLogPrefix(session)
					+ "Nothing to send or test");
		}

		String result = "";
		if ("on".equals(multipleConfigs)) {
			try {
				result = processZipFile(session, fileName);
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
			int i = fileName.lastIndexOf(".");
			if (i != -1) {
				name = fileName.substring(0, i);
				int j = name.lastIndexOf("-");
				if (j != -1) {
					name = name.substring(0, j);
					j = name.lastIndexOf("-");
					if (j != -1) {
						name = fileName.substring(0, j);
						version = fileName.substring(j + 1, i);
					}
				}
			}
		}
		return new String[] { name, version };
	}

	private String processZipFile(IPipeLineSession session, String fileName)
			throws PipeRunException, IOException {
		String result = "";
		Object form_file = session.get("file");
		if (form_file != null) {
			if (form_file instanceof InputStream) {
				InputStream inputStream = (InputStream) form_file;
				String form_fileEncoding = (String) session.get("fileEncoding");
				if (inputStream.available() > 0) {
					/*
					 * String fileEncoding; if
					 * (StringUtils.isNotEmpty(form_fileEncoding)) {
					 * fileEncoding = form_fileEncoding; } else { fileEncoding =
					 * Misc.DEFAULT_INPUT_STREAM_ENCODING; }
					 */ZipInputStream archive = new ZipInputStream(inputStream);
					int counter = 1;
					for (ZipEntry entry = archive.getNextEntry(); entry != null; entry = archive
							.getNextEntry()) {
						String entryName = entry.getName();
						int size = (int) entry.getSize();
						if (size > 0) {
							byte[] b = new byte[size];
							int rb = 0;
							int chunk = 0;
							while (((int) size - rb) > 0) {
								chunk = archive.read(b, rb, (int) size - rb);
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
							if (StringUtils.isNotEmpty(result)) {
								result += "\n";
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
							result += entryName
									+ ":"
									+ processJarFile(session, name, version,
											entryName, fileNameSessionKey);
							session.remove(fileNameSessionKey);
						}
						archive.closeEntry();
						counter++;
					}
					archive.close();
				}
			}
		}
		return result;
	}

	private String processJarFile(IPipeLineSession session, String name,
			String version, String fileName, String fileNameSessionKey)
			throws PipeRunException {
		String form_jmsRealm = (String) session.get("jmsRealm");

		String result = "";
		FixedQuerySender qs = (FixedQuerySender) ibisContext
				.createBeanAutowireByName(FixedQuerySender.class);
		try {
			qs.setName("QuerySender");
			qs.setJmsRealm(form_jmsRealm);
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
					"dummy", session);
			result = qs.sendMessage("dummy", "dummy", prc);
		} catch (Throwable t) {
			throw new PipeRunException(this, getLogPrefix(session)
					+ "Error occured on executing jdbc query", t);
		} finally {
			qs.close();
		}

		if (Integer.parseInt(result) > 0) {
			qs = (FixedQuerySender) ibisContext
					.createBeanAutowireByName(FixedQuerySender.class);
			try {
				qs.setName("QuerySender");
				qs.setJmsRealm(form_jmsRealm);
				qs.setQuery("DELETE FROM IBISCONFIG WHERE NAME=?");
				Parameter param = new Parameter();
				param.setName("name");
				param.setValue(name);
				qs.addParameter(param);
				qs.setScalar(true);
				qs.configure();
				qs.open();
				ParameterResolutionContext prc = new ParameterResolutionContext(
						"dummy", session);
				result = qs.sendMessage("dummy", "dummy", prc);
			} catch (Throwable t) {
				throw new PipeRunException(this, getLogPrefix(session)
						+ "Error occured on executing jdbc query", t);
			} finally {
				qs.close();
			}
		}

		/*
		 * Why is the following not working (remoteUser is always empty)? String
		 * remoteUser; try { remoteUser =
		 * RestListenerUtils.retrieveRequestRemoteUser(session); } catch
		 * (IOException e) { throw new PipeRunException(this,
		 * getLogPrefix(session) + "Error occured on retrieving remote user",
		 * e); }
		 */
		String remoteUser = (String) session.get("principal");

		qs = (FixedQuerySender) ibisContext
				.createBeanAutowireByName(FixedQuerySender.class);
		try {
			qs.setName("QuerySender");
			qs.setJmsRealm(form_jmsRealm);
			qs.setQueryType("insert");
			if (StringUtils.isEmpty(remoteUser)) {
				qs.setQuery("INSERT INTO IBISCONFIG (NAME, VERSION, FILENAME, CONFIG, CRE_TYDST) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)");
			} else {
				qs.setQuery("INSERT INTO IBISCONFIG (NAME, VERSION, FILENAME, CONFIG, CRE_TYDST, RUSER) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, ?)");
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
			qs.configure();
			qs.open();
			ParameterResolutionContext prc = new ParameterResolutionContext(
					"dummy", session);
			result = qs.sendMessage("dummy", "dummy", prc);
		} catch (Throwable t) {
			throw new PipeRunException(this, getLogPrefix(session)
					+ "Error occured on executing jdbc query", t);
		} finally {
			qs.close();
		}
		return result;
	}

	private String retrieveFormInput(IPipeLineSession session) {
		List<String> jmsRealms = JmsRealmFactory.getInstance()
				.getRegisteredRealmNamesAsList();
		if (jmsRealms.size() == 0)
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