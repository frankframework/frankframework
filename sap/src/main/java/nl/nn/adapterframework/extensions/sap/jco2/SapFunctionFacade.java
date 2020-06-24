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
package nl.nn.adapterframework.extensions.sap.jco2;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.Logger;

import com.sap.mw.jco.IFunctionTemplate;
import com.sap.mw.jco.IRepository;
import com.sap.mw.jco.JCO;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.extensions.sap.ISapFunctionFacade;
import nl.nn.adapterframework.extensions.sap.SapException;
import nl.nn.adapterframework.parameters.ParameterValue;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.TransformerPool;
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
 * @since   4.2
 */
public class SapFunctionFacade implements ISapFunctionFacade {
	protected Logger log = LogUtil.getLogger(this);

	private String name;
	private String sapSystemName;
	
	private int correlationIdFieldIndex=0;
	private String correlationIdFieldName;
	private int requestFieldIndex=0;
	private String requestFieldName;
	private int replyFieldIndex=0;
	private String replyFieldName;

	private IFunctionTemplate ftemplate;
	private SapSystem sapSystem;

	private static Map<String,TransformerPool> extractors = new HashMap<String,TransformerPool>();

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
			if (!StringUtils.isEmpty(getFunctionName())) {
				ftemplate = getFunctionTemplate(sapSystem, getFunctionName());
				try {
					calculateStaticFieldIndices(ftemplate);
				} catch (Exception e) {
					throw new SapException(getLogPrefix()+"Exception calculation field-indices ["+getFunctionName()+"]", e);
				}
			}
		} else {
			SapSystem.openSystems();
		}
	}
	
	public void closeFacade() {
		if (sapSystem!=null) {
			sapSystem.closeSystem();
		} else {
			SapSystem.closeSystems();
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



	static protected void setParameters(JCO.ParameterList params, String message, int fieldIndex) throws SapException {
		if (params != null && StringUtils.isNotEmpty(message)) {
			if (fieldIndex>0) {
				params.setValue(message,fieldIndex-1);
			} else {
				String paramName=params.getName();
				TransformerPool tp = (TransformerPool)extractors.get(paramName);
				if (tp==null) {
					try {
//						log.debug("creating evaluator for parameter ["+paramName+"]");
						tp = TransformerPool.getInstance(XmlUtils.createXPathEvaluatorSource("/*/"+paramName,"xml"));
						extractors.put(paramName,tp);
					} catch (Exception e) {
						throw new SapException("exception creating Extractor for  ["+paramName+"]", e);
					}
				}
				try {
					String paramsXml = tp.transform(message,null);
					if (StringUtils.isNotEmpty(paramsXml)) {
						//log.debug("parameters ["+params.getName()+"] Xml ["+paramsXml+"]");
						params.fromXML(paramsXml);
					}
				} catch (Exception e) {
					throw new SapException("exception extracting ["+paramName+"]", e);
				}
			}
		}
	}

	static protected void setTables(JCO.ParameterList tableParams, String message) throws SapException {
		if (tableParams != null && StringUtils.isNotEmpty(message)) {
			String paramsName=tableParams.getName();
			TransformerPool tp = (TransformerPool)extractors.get(paramsName);
			if (tp==null) {
				try {
//					log.debug("creating evaluator for parameter ["+paramName+"]");
					tp = TransformerPool.getInstance(XmlUtils.createXPathEvaluatorSource("/*/"+paramsName,"xml"));
					extractors.put(paramsName,tp);
				} catch (Exception e) {
					throw new SapException("exception creating Extractor for  ["+paramsName+"]", e);
				}
			}
			try {
				String paramsXml = tp.transform(message,null);
				TableDigester td = new TableDigester();
				td.digestTableXml(tableParams,paramsXml);
			} catch (Exception e) {
				throw new SapException("exception extracting ["+paramsName+"]", e);
			}
		}
	}


	/**
	 * This method must be called from configure().
	 * @param ft
	 */
	protected void calculateStaticFieldIndices(IFunctionTemplate ft) {
		if (getRequestFieldIndex()== 0) {	
			if (StringUtils.isEmpty(getRequestFieldName())) {
				setRequestFieldIndex(-1);
			} else {
				if (ft!=null) {
					setRequestFieldIndex(1+ft.getImportParameterList().indexOf(getRequestFieldName()));
				}				
			}
		}
		if (getReplyFieldIndex()== 0) {	
			if (StringUtils.isEmpty(getReplyFieldName())) {
				setReplyFieldIndex(-1);
			} else {
				if (ft!=null) {
					setReplyFieldIndex(1+ft.getExportParameterList().indexOf(getReplyFieldName()));
				}				
			}
		}
		if (getCorrelationIdFieldIndex()== 0) {	
			if (StringUtils.isEmpty(getCorrelationIdFieldName())) {
				setCorrelationIdFieldIndex(-1);
			} else {
				if (ft!=null) {
					setCorrelationIdFieldIndex(1+ft.getImportParameterList().indexOf(getCorrelationIdFieldName()));
				}				
			}
		}
	}
			

	/**
	 * Calculate the index of the field that correspondes with the message as a whole.
	 * 
	 * return values
	 *  >0 : the required index
	 *  0  : no index found, convert all fields to/from xml.
	 */
	protected int findFieldIndex(JCO.ParameterList params, int index, String name) {
		if (index!=0 || StringUtils.isEmpty(name)) {
			return index;
		}
		try {
			return (1+params.indexOf(name));
		} catch (Exception e) {
			log.warn("["+getName()+"] exception finding FieldIndex for name ["+name+"]", e);
			return 0;
		}
	}

	public String getCorrelationIdFromField(JCO.Function function) {
		JCO.ParameterList input = function.getImportParameterList();
		int correlationIdFieldIndex = findFieldIndex(input, getCorrelationIdFieldIndex(), getCorrelationIdFieldName());
		if (correlationIdFieldIndex>0 && input!=null) {
				return input.getString(correlationIdFieldIndex-1);
			}
		return null;
	}


	public Message functionCall2message(JCO.Function function) {
		JCO.ParameterList input = function.getImportParameterList();
		
		int messageFieldIndex = findFieldIndex(input, getRequestFieldIndex(), getRequestFieldName());
		String result=null;
		if (messageFieldIndex>0) {
			if (input!=null) {
				result = input.getString(messageFieldIndex-1);
			}
		} else {
			result = "<request function=\""+function.getName()+"\">";
	
			JCO.ParameterList tables = function.getTableParameterList();
			
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

	public Message functionResult2message(JCO.Function function) {
		JCO.ParameterList export = function.getExportParameterList();
		
		int replyFieldIndex = findFieldIndex(export, getReplyFieldIndex(), getReplyFieldName());
		String result=null;
		if (replyFieldIndex>0) {
			if (export!=null) {
				result = export.getString(replyFieldIndex-1);
			}
		} else {
			result = "<response function=\""+function.getName()+"\">";

			JCO.ParameterList tables = function.getTableParameterList();
		
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

	public void message2FunctionCall(JCO.Function function, String request, String correlationId, ParameterValueList pvl) throws SapException {
		JCO.ParameterList input = function.getImportParameterList();
		int requestFieldIndex = findFieldIndex(input, getRequestFieldIndex(), getRequestFieldName());
		setParameters(input, request, requestFieldIndex);
		if (requestFieldIndex<=0) {
			setParameters(function.getTableParameterList(),  request, 0);
		}
		if (pvl!=null) {
			for (int i=0; i<pvl.size(); i++) {
				ParameterValue pv = pvl.getParameterValue(i);
				String name = pv.getDefinition().getName();
				String value = pv.asStringValue("");
				int slashPos=name.indexOf('/');
				if (slashPos<0) {
					input.setValue(value,name);
				} else {
					String structName=name.substring(0,slashPos);
					String elemName=name.substring(slashPos+1);
					JCO.Structure struct=input.getStructure(structName);
					struct.setValue(value,elemName);
				}
			}
		}
		int correlationIdFieldIndex = findFieldIndex(input, getCorrelationIdFieldIndex(), getCorrelationIdFieldName());
		if (correlationIdFieldIndex>0 && input!=null) {
			input.setValue(correlationId, correlationIdFieldIndex-1);
		}
	}

	public void message2FunctionResult(JCO.Function function, String result) throws SapException {
		JCO.ParameterList output = function.getExportParameterList();
		int replyFieldIndex = findFieldIndex(output, getReplyFieldIndex(), getReplyFieldName());
		setParameters(function.getExportParameterList(),result, replyFieldIndex);
		//log.warn("SapFunctionFacade.message2FunctionResult, skipped setting of return table parameters");
		if (replyFieldIndex<=0) {
			log.debug("SapFunctionFacade.message2FunctionResult, setting table parameters");
			setTables(function.getTableParameterList(), result);
		}
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


	protected IFunctionTemplate getFunctionTemplate() throws SapException {
		if(ftemplate==null) {
			throw new SapException("no fixed functionName specified");
		}
		return ftemplate;
	}
	protected IFunctionTemplate getFunctionTemplate(SapSystem sapSystem, String functionName) throws SapException {
		IFunctionTemplate functionTemplate;
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

	public void clearCache() throws SapException {
		IRepository jcoRepository = sapSystem.getJcoRepository();
		String[] cachedFunctionInterfaces=jcoRepository.getCachedFunctionInterfaces();
		if (cachedFunctionInterfaces!=null) {
			for (int i=0;i<cachedFunctionInterfaces.length;i++) {
				jcoRepository.removeFunctionInterfaceFromCache(cachedFunctionInterfaces[i]);
			}
		}
		String[] cachedStructureDefinitions=jcoRepository.getCachedStructureDefinitions();
		if (cachedStructureDefinitions!=null) {
			for (int i=0;i<cachedStructureDefinitions.length;i++) {
				jcoRepository.removeStructureDefinitionFromCache(cachedStructureDefinitions[i]);
			}
		}
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
	protected String getFunctionName() {
		return null;
	}


}
