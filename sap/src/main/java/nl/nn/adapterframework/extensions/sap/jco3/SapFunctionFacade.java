/*
   Copyright 2013 Nationale-Nederlanden, 2020 WeAreFrank!

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
package nl.nn.adapterframework.extensions.sap.jco3;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.Logger;

import com.sap.conn.jco.JCoFunction;
import com.sap.conn.jco.JCoFunctionTemplate;
import com.sap.conn.jco.JCoParameterList;
import com.sap.conn.jco.JCoStructure;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.extensions.sap.ISapFunctionFacade;
import nl.nn.adapterframework.extensions.sap.SapException;
import nl.nn.adapterframework.extensions.sap.jco3.handlers.Handler;
import nl.nn.adapterframework.parameters.ParameterValue;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.XmlUtils;
/**
 * Wrapper round SAP-functions, either SAP calling Ibis, or Ibis calling SAP.
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setName(String) name}</td><td>Name of the Ibis-object</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSapSystemName(String) sapSystemName}</td><td>name of the {@link SapSystem} used by this object</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setCorrelationIdFieldIndex(int) correlationIdFieldIndex}</td><td>Index of the field in the ImportParameterList of the RFC function that contains the correlationId</td><td>0</td></tr>
 * <tr><td>{@link #setCorrelationIdFieldName(String) correlationIdFieldName}</td><td>Name of the field in the ImportParameterList of the RFC function that contains the correlationId</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setRequestFieldIndex(int) requestFieldIndex}</td><td>Index of the field in the ImportParameterList of the RFC function that contains the whole request message contents</td><td>0</td></tr>
 * <tr><td>{@link #setRequestFieldName(String) requestFieldName}</td><td>Name of the field in the ImportParameterList of the RFC function that contains the whole request message contents</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setReplyFieldIndex(int) replyFieldIndex}</td><td>Index of the field in the ExportParameterList of the RFC function that contains the whole reply message contents</td><td>0</td></tr>
 * <tr><td>{@link #setReplyFieldName(String) replyFieldName}</td><td>Name of the field in the ExportParameterList of the RFC function that contains the whole reply message contents</td><td>&nbsp;</td></tr>
 * </table>
 * N.B. If no requestFieldIndex or requestFieldName is specified, input is converted from/to xml;
 * If no replyFieldIndex or replyFieldName is specified, output is converted from/to xml. 
 * </p>
 * @author  Gerrit van Brakel
 * @author  Jaco de Groot
 * @since   5.0
 */
public abstract class SapFunctionFacade implements ISapFunctionFacade {
	protected static Logger log = LogUtil.getLogger(SapFunctionFacade.class);

	private String name;
	private String sapSystemName;

	private int correlationIdFieldIndex=0;
	private String correlationIdFieldName;
	private int requestFieldIndex=0;
	private String requestFieldName;
	private int replyFieldIndex=0;
	private String replyFieldName;

	private JCoFunctionTemplate ftemplate;
	private SapSystem sapSystem;

	protected String getLogPrefix() {
		return this.getClass().getName()+" ["+getName()+"] ";
	}

	public void configure() throws ConfigurationException {
//		if (StringUtils.isEmpty(getSapSystemName())) {
//			throw new ConfigurationException("attribute sapSystemName must be specified");
//		}
		if (StringUtils.isNotEmpty(getSapSystemName())) {
			sapSystem=SapSystem.getSystem(getSapSystemName());
			if (sapSystem==null) {
				throw new ConfigurationException(getLogPrefix()+"cannot find SapSystem ["+getSapSystemName()+"]");
			}
		} else {
			SapSystem.configureAll();
		}
	}

	public void openFacade() throws SapException {
		if (sapSystem!=null) {
			sapSystem.openSystem();
			log.info("open SapSystem ["+sapSystem.toString()+"]");

			//Something has changed, so remove the cached templates
			SapSystemDataProvider.getInstance().updateSystem(sapSystem);

			if (StringUtils.isNotEmpty(getFunctionName())) { //Listeners and IdocSenders don't use a functionName
				ftemplate = getFunctionTemplate(sapSystem, getFunctionName());
				log.debug("found JCoFunctionTemplate ["+ftemplate.toString()+"]");
				try {
					calculateStaticFieldIndices(ftemplate);
				} catch (Exception e) {
					throw new SapException(getLogPrefix()+"Exception calculation field-indices ["+getFunctionName()+"]", e);
				}
			}
		} else {
			log.info("open ALL SapSystems");
			SapSystem.openSystems();
		}
	}

	public void closeFacade() {
		log.info("trying to close all SapSystem resources");

		if (sapSystem != null) {
			sapSystem.closeSystem();
			log.debug("closed local defined sapSystem");
		} else {
			SapSystem.closeSystems();
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

	static protected void setParameters(JCoParameterList inputOrOutputParameterList, JCoParameterList tableParameterList, String message, int fieldIndex) throws SapException {
		if (StringUtils.isNotEmpty(message)) {
			if (fieldIndex>0) {
				if (inputOrOutputParameterList != null) {
					inputOrOutputParameterList.setValue(fieldIndex-1,message);
				}
			} else {
				List<JCoParameterList> parameterLists = new ArrayList<JCoParameterList>();
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
		log.info("calculate static field indexes for JCOFunctionTemplate ["+(ft!=null?ft.getName():"unknown")+"]");
		if (getRequestFieldIndex()== 0) {
			if (StringUtils.isEmpty(getRequestFieldName())) {
				setRequestFieldIndex(-1);
				log.debug("requestFieldName not set, using index [-1]");
			} else {
				if (ft!=null) {
					setRequestFieldIndex(1+ft.getImportParameterList().indexOf(getRequestFieldName()));
					log.debug("searching for requestFieldName ["+getRequestFieldName()+"] in JCOFunctionTemplate Parameters ["+ft.getImportParameterList()+"]");
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
					log.debug("searching for replyFieldName ["+getReplyFieldName()+"] in JCOFunctionTemplate Parameters ["+ft.getImportParameterList()+"]");
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
					log.debug("searching for correlationIdFieldName ["+getCorrelationIdFieldName()+"] in JCOFunctionTemplate Parameters ["+ft.getImportParameterList()+"]");
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
			log.trace("find FieldIndex for name ["+name+"] in JCoParameterList ["+params.toString()+"]");

		if (index!=0 || StringUtils.isEmpty(name)) {
			return index;
		}
		try {
			return (1+params.getListMetaData().indexOf(name));
		} catch (Exception e) {
			log.warn("["+getName()+"] exception finding FieldIndex for name ["+name+"]", e);
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
			for (int i=0; i<pvl.size(); i++) {
				ParameterValue pv = pvl.getParameterValue(i);
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

	public SapSystem getSapSystem() throws SapException {
		if(sapSystem==null) {
			throw new SapException("no fixed sapSystem specified");
		}
		return sapSystem;
	}
	public SapSystem getSapSystem(String systemName) throws SapException {
		SapSystem sapSystem = SapSystem.getSystem(systemName);
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

	protected JCoFunctionTemplate getFunctionTemplate(SapSystem sapSystem, String functionName) throws SapException {
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

	
	public int getCorrelationIdFieldIndex() {
		return correlationIdFieldIndex;
	}

	public String getCorrelationIdFieldName() {
		return correlationIdFieldName;
	}

	public int getReplyFieldIndex() {
		return replyFieldIndex;
	}

	public String getReplyFieldName() {
		return replyFieldName;
	}

	public int getRequestFieldIndex() {
		return requestFieldIndex;
	}

	public String getRequestFieldName() {
		return requestFieldName;
	}

	@Override
	public void setCorrelationIdFieldIndex(int i) {
		correlationIdFieldIndex = i;
	}

	@Override
	public void setCorrelationIdFieldName(String string) {
		correlationIdFieldName = string;
	}

	@Override
	public void setReplyFieldIndex(int i) {
		replyFieldIndex = i;
	}

	@Override
	public void setReplyFieldName(String string) {
		replyFieldName = string;
	}

	@Override
	public void setRequestFieldIndex(int i) {
		requestFieldIndex = i;
	}

	@Override
	public void setRequestFieldName(String string) {
		requestFieldName = string;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String string) {
		name = string;
	}

	public String getSapSystemName() {
		return sapSystemName;
	}

	@Override
	public void setSapSystemName(String string) {
		sapSystemName = string;
	}

	/**
	 * Listeners and IdocSenders don't use a functionName
	 */
	protected abstract String getFunctionName();

}
