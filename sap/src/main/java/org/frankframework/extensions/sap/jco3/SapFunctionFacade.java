/*
   Copyright 2013 Nationale-Nederlanden, 2020, 2022 WeAreFrank!

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
package org.frankframework.extensions.sap.jco3;

import java.util.ArrayList;
import java.util.List;

import com.sap.conn.jco.JCoFunction;
import com.sap.conn.jco.JCoFunctionTemplate;
import com.sap.conn.jco.JCoParameterList;
import com.sap.conn.jco.JCoStructure;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.extensions.sap.ISapFunctionFacade;
import org.frankframework.extensions.sap.SapException;
import org.frankframework.extensions.sap.jco3.handlers.Handler;
import org.frankframework.parameters.ParameterValue;
import org.frankframework.parameters.ParameterValueList;
import org.frankframework.stream.Message;
import org.frankframework.util.LogUtil;
import org.frankframework.util.XmlUtils;
import org.springframework.context.ApplicationContext;
/**
 * Wrapper round SAP-functions, either SAP calling Ibis, or Ibis calling SAP.
 *
 * N.B. If no requestFieldIndex or requestFieldName is specified, input is converted from/to xml;
 * If no replyFieldIndex or replyFieldName is specified, output is converted from/to xml.
 * </p>
 * @author  Gerrit van Brakel
 * @author  Jaco de Groot
 * @since   5.0
 */
public abstract class SapFunctionFacade implements ISapFunctionFacade {
	private final @Getter String domain = "SAP";
	protected static Logger log = LogUtil.getLogger(SapFunctionFacade.class);
	private final @Getter ClassLoader configurationClassLoader = Thread.currentThread().getContextClassLoader();
	private @Getter @Setter ApplicationContext applicationContext;

	private @Getter String name;
	private @Getter String sapSystemName;

	private @Getter int correlationIdFieldIndex=0;
	private @Getter String correlationIdFieldName;
	private @Getter int requestFieldIndex=0;
	private @Getter String requestFieldName;
	private @Getter int replyFieldIndex=0;
	private @Getter String replyFieldName;

	private JCoFunctionTemplate ftemplate;
	private SapSystemImpl sapSystem;

	protected String getLogPrefix() {
		return this.getClass().getName()+" ["+getName()+"] ";
	}

	@Override
	public void configure() throws ConfigurationException {
//		if (StringUtils.isEmpty(getSapSystemName())) {
//			throw new ConfigurationException("attribute sapSystemName must be specified");
//		}
		if (StringUtils.isNotEmpty(getSapSystemName())) {
			sapSystem=SapSystemImpl.getSystem(getSapSystemName());
			if (sapSystem==null) {
				throw new ConfigurationException(getLogPrefix()+"cannot find SapSystem ["+getSapSystemName()+"]");
			}
		} else {
			SapSystemImpl.configureAll();
		}
	}

	public void openFacade() throws SapException {
		if (sapSystem!=null) {
			sapSystem.openSystem();
			log.info("open SapSystem [{}]", sapSystem.toString());

			//Something has changed, so remove the cached templates
			SapSystemDataProvider.getInstance().updateSystem(sapSystem);

			if (StringUtils.isNotEmpty(getFunctionName())) { //Listeners and IdocSenders don't use a functionName
				ftemplate = getFunctionTemplate(sapSystem, getFunctionName());
				log.debug("found JCoFunctionTemplate [{}]", ftemplate.toString());
				try {
					calculateStaticFieldIndices(ftemplate);
				} catch (Exception e) {
					throw new SapException(getLogPrefix()+"Exception calculation field-indices ["+getFunctionName()+"]", e);
				}
			}
		} else {
			log.info("open ALL SapSystems");
			SapSystemImpl.openSystems();
		}
	}

	public void closeFacade() {
		log.info("trying to close all SapSystem resources");

		if (sapSystem != null) {
			sapSystem.closeSystem();
			log.debug("closed local defined sapSystem");
		} else {
			SapSystemImpl.closeSystems();
			log.debug("closed all sapSystems");
		}
		ftemplate = null;
	}

	@Override
	public String getPhysicalDestinationName() {
		String result;
		if (sapSystem==null) {
			return "dynamical determined"; // to avoid NPE
		}
		result = "mandant ["+sapSystem.getMandant()+"] on gwhost ["+sapSystem.getGwhost()+"] system ["+sapSystem.getSystemnr()+"]";
		return result;
	}

	protected static void setParameters(JCoParameterList inputOrOutputParameterList, JCoParameterList tableParameterList, String message, int fieldIndex) throws SapException {
		if (StringUtils.isNotEmpty(message)) {
			if (fieldIndex>0) {
				if (inputOrOutputParameterList != null) {
					inputOrOutputParameterList.setValue(fieldIndex-1,message);
				}
			} else {
				List<JCoParameterList> parameterLists = new ArrayList<>();
				if (inputOrOutputParameterList != null) {
					parameterLists.add(inputOrOutputParameterList);
				}
				if (tableParameterList != null) {
					parameterLists.add(tableParameterList);
				}
				if (parameterLists.size() > 0) {
					Handler handler = Handler.getHandler(parameterLists, log);
					try {
						XmlUtils.parseXml(message, handler);
					} catch (Exception e) {
						throw new SapException("exception parsing message", e);
					}
				}
			}
		}
	}

	/**
	 * This method must be called from configure().
	 * @param ft
	 */
	protected void calculateStaticFieldIndices(JCoFunctionTemplate ft) {
		log.info("calculate static field indexes for JCOFunctionTemplate [{}]", ft != null ? ft.getName() : "unknown");
		if (getRequestFieldIndex()== 0) {
			if (StringUtils.isEmpty(getRequestFieldName())) {
				setRequestFieldIndex(-1);
				log.debug("requestFieldName not set, using index [-1]");
			} else {
				if (ft!=null) {
					setRequestFieldIndex(1+ft.getImportParameterList().indexOf(getRequestFieldName()));
					log.debug("searching for requestFieldName [{}] in JCOFunctionTemplate Parameters [{}]", getRequestFieldName(), ft.getImportParameterList());
				}
			}
		}
		if (getReplyFieldIndex()== 0) {
			if (StringUtils.isEmpty(getReplyFieldName())) {
				setReplyFieldIndex(-1);
				log.debug("replyFieldIndex not set, using index [-1]");
			} else {
				if (ft!=null) {
					setReplyFieldIndex(1+ft.getExportParameterList().indexOf(getReplyFieldName()));
					log.debug("searching for replyFieldName [{}] in JCOFunctionTemplate Parameters [{}]", getReplyFieldName(), ft.getImportParameterList());
				}
			}
		}
		if (getCorrelationIdFieldIndex()== 0) {
			if (StringUtils.isEmpty(getCorrelationIdFieldName())) {
				setCorrelationIdFieldIndex(-1);
				log.debug("correlationIdFieldIndex not set, using index [-1]");
			} else {
				if (ft!=null) {
					setCorrelationIdFieldIndex(1+ft.getImportParameterList().indexOf(getCorrelationIdFieldName()));
					log.debug("searching for correlationIdFieldName [{}] in JCOFunctionTemplate Parameters [{}]", getCorrelationIdFieldName(), ft.getImportParameterList());
				}
			}
		}
	}

	/**
	 * Calculate the index of the field that corresponds with the message as a whole.
	 *
	 * return values
	 *  >0 : the required index
	 *  0  : no index found, convert all fields to/from xml.
	 */
	protected int findFieldIndex(JCoParameterList params, int index, String name) {
		if(name != null && params != null && log.isTraceEnabled())
			log.trace("find FieldIndex for name [{}] in JCoParameterList [{}]", name, params);

		if (index!=0 || StringUtils.isEmpty(name)) {
			return index;
		}
		try {
			return 1+params.getListMetaData().indexOf(name);
		} catch (Exception e) {
			log.warn("[{}] exception finding FieldIndex for name [{}]", getName(), name, e);
			return 0;
		}
	}

	public String getCorrelationIdFromField(JCoFunction function) {
		JCoParameterList input = function.getImportParameterList();
		int correlationIdFieldIndex = findFieldIndex(input, getCorrelationIdFieldIndex(), getCorrelationIdFieldName());
		if (correlationIdFieldIndex>0 && input!=null) {
				return input.getString(correlationIdFieldIndex-1);
			}
		return null;
	}

	public Message functionCall2message(JCoFunction function) {
		JCoParameterList input = function.getImportParameterList();

		int messageFieldIndex = findFieldIndex(input, getRequestFieldIndex(), getRequestFieldName());
		String result=null;
		if (messageFieldIndex > 0) {
			if (input!=null) {
				result = input.getString(messageFieldIndex-1);
			}
		}
		else {
			result = "<request function=\""+function.getName()+"\">";

			JCoParameterList tables = function.getTableParameterList();

			if (input!=null) {
				result+=input.toXML();
			}
			if (tables!=null) {
				result+=tables.toXML();
			}
			result+="</request>";
		}

		return new Message(result);
	}

	public Message functionResult2message(JCoFunction function) {
		JCoParameterList export = function.getExportParameterList();

		int replyFieldIndex = findFieldIndex(export, getReplyFieldIndex(), getReplyFieldName());
		String result=null;
		if (replyFieldIndex>0) {
			if (export!=null) {
				result = export.getString(replyFieldIndex-1);
			}
		} else {
			result = "<response function=\""+function.getName()+"\">";

			JCoParameterList tables = function.getTableParameterList();

			if (export!=null) {
				result+=export.toXML();
			}
			if (tables!=null) {
				result+=tables.toXML();
			}
			result+="</response>";
		}
		return new Message(result);
	}

	public void message2FunctionCall(JCoFunction function, String request, String correlationId, ParameterValueList pvl) throws SapException {
		JCoParameterList input = function.getImportParameterList();
		int requestFieldIndex = findFieldIndex(input, getRequestFieldIndex(), getRequestFieldName());
		setParameters(input, function.getTableParameterList(), request, requestFieldIndex);
		if (pvl!=null) {
			for(ParameterValue pv : pvl) {
				String name = pv.getDefinition().getName();
				String value = pv.asStringValue("");
				int slashPos=name.indexOf('/');
				if (slashPos<0) {
					input.setValue(name,value);
				} else {
					String structName=name.substring(0,slashPos);
					String elemName=name.substring(slashPos+1);
					JCoStructure struct=input.getStructure(structName);
					struct.setValue(elemName,value);
				}
			}
		}
		int correlationIdFieldIndex = findFieldIndex(input, getCorrelationIdFieldIndex(), getCorrelationIdFieldName());
		if (correlationIdFieldIndex>0 && input!=null) {
			input.setValue(correlationIdFieldIndex-1, correlationId);
		}
	}

	public void message2FunctionResult(JCoFunction function, String result) throws SapException {
		JCoParameterList output = function.getExportParameterList();
		int replyFieldIndex = findFieldIndex(output, getReplyFieldIndex(), getReplyFieldName());
		setParameters(output, function.getTableParameterList(), result, replyFieldIndex);
	}

	public SapSystemImpl getSapSystem() throws SapException {
		if(sapSystem==null) {
			throw new SapException("no fixed sapSystem specified");
		}
		return sapSystem;
	}
	public SapSystemImpl getSapSystem(String systemName) throws SapException {
		SapSystemImpl sapSystem = SapSystemImpl.getSystem(systemName);
		if(sapSystem==null) {
			throw new SapException("cannot find sapSystem ["+systemName+"]");
		}
		return sapSystem;
	}

	protected JCoFunctionTemplate getFunctionTemplate() throws SapException {
		if(ftemplate==null) {
			throw new SapException("no fixed functionName specified");
		}
		return ftemplate;
	}

	public JCoFunctionTemplate getFunctionTemplate(SapSystemImpl sapSystem, String functionName) throws SapException {
		JCoFunctionTemplate functionTemplate;
		try {
			functionTemplate = sapSystem.getJcoRepository().getFunctionTemplate(functionName);
		} catch (Exception e) {
			throw new SapException(getLogPrefix()+"exception obtaining template for function ["+functionName+"] from sapSystem ["+sapSystem.getName()+"]", e);
		}
		if (functionTemplate == null) {
			throw new SapException(getLogPrefix()+"could not obtain template for function ["+functionName+"] from sapSystem ["+sapSystem.getName()+"]");
		}
		return functionTemplate;
	}


	/** Name of the Ibis-object */
	@Override
	public void setName(String string) {
		name = string;
	}

	/** Name of the {@link SapSystem} used by this object */
	@Override
	public void setSapSystemName(String string) {
		sapSystemName = string;
	}

	/**
	 * Index of the field in the ImportParameterList of the RFC function that contains the correlationId
	 * @ff.default 0
	 */
	@Override
	public void setCorrelationIdFieldIndex(int i) {
		correlationIdFieldIndex = i;
	}

	/** Name of the field in the ImportParameterList of the RFC function that contains the correlationId */
	@Override
	public void setCorrelationIdFieldName(String string) {
		correlationIdFieldName = string;
	}

	/**
	 * Index of the field in the ImportParameterList of the RFC function that contains the whole request message contents
	 * @ff.default 0
	 */
	@Override
	public void setRequestFieldIndex(int i) {
		requestFieldIndex = i;
	}

	/** Name of the field in the ImportParameterList of the RFC function that contains the whole request message contents */
	@Override
	public void setRequestFieldName(String string) {
		requestFieldName = string;
	}

	/**
	 * Index of the field in the ExportParameterList of the RFC function that contains the whole reply message contents
	 * @ff.default 0
	 */
	@Override
	public void setReplyFieldIndex(int i) {
		replyFieldIndex = i;
	}

	/** Name of the field in the ExportParameterList of the RFC function that contains the whole reply message contents */
	@Override
	public void setReplyFieldName(String string) {
		replyFieldName = string;
	}

	/**
	 * Listeners and IdocSenders don't use a functionName
	 */
	protected abstract String getFunctionName();

}
