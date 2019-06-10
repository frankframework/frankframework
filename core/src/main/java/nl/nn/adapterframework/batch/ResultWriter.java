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
package nl.nn.adapterframework.batch;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.doc.IbisDescription; 
import nl.nn.adapterframework.parameters.ParameterResolutionContext;

import org.apache.commons.lang.StringUtils;



/** 
 * @author  Gerrit van Brakel
 * @since   4.7
 */
@IbisDescription(
	"Baseclass for resulthandlers that write the transformed record to a writer." 
)
public abstract class ResultWriter extends AbstractResultHandler {
	
	private String onOpenDocument="<document name=\"#name#\">";
	private String onCloseDocument="</document>";
	private String onOpenBlock="<#name#>";
	private String onCloseBlock="</#name#>";
	private String blockNamePattern="#name#";
	
	private Map openWriters = Collections.synchronizedMap(new HashMap());
	
	protected abstract Writer createWriter(IPipeLineSession session, String streamId, ParameterResolutionContext prc) throws Exception;

	public void openDocument(IPipeLineSession session, String streamId, ParameterResolutionContext prc) throws Exception {
		super.openDocument(session, streamId, prc);
		getWriter(session, streamId, true, prc);
		write(session,streamId,replacePattern(getOnOpenDocument(),streamId), prc);
	}

	public void closeDocument(IPipeLineSession session, String streamId, ParameterResolutionContext prc) {
		Writer w = (Writer)openWriters.remove(streamId);
		if (w != null) {
			try {
				w.close();
			} catch (IOException e) {
				log.error("Exception closing ["+streamId+"]",e);
			}
		}
		super.closeDocument(session,streamId, prc);
	}

	public Object finalizeResult(IPipeLineSession session, String streamId, boolean error, ParameterResolutionContext prc) throws Exception {
		log.debug("finalizeResult ["+streamId+"]");
		write(session,streamId,replacePattern(getOnCloseDocument(),streamId), prc);
		return null;
	}
	

	
	public void handleResult(IPipeLineSession session, String streamId, String recordKey, Object result, ParameterResolutionContext prc) throws Exception {
		if (result instanceof String) {
			write(session, streamId, (String)result, prc);
		}
		else if (result instanceof String[]) {
			write(session, streamId, (String[])result, prc);
		}
	}
	
	protected void writeNewLine(Writer w) throws IOException {
		if (w instanceof BufferedWriter) {
			((BufferedWriter)w).newLine();
		} else {
			w.write("\n");
		}
	}
	
	private void write(IPipeLineSession session, String streamId, String line, ParameterResolutionContext prc) throws Exception {
		if (line!=null) {
			Writer w = getWriter(session, streamId, false, prc);
			if (w==null) {
				throw new NullPointerException("No Writer Found for stream ["+streamId+"]");
			}
			w.write(line);
			writeNewLine(w);
		}
	}

	private void write(IPipeLineSession session, String streamId, String[] lines, ParameterResolutionContext prc) throws Exception {
		Writer w = getWriter(session, streamId, false, prc);
		for (int i = 0; i < lines.length; i++) {
			if (lines[i]!=null) {
				w.write(lines[i]);
				writeNewLine(w);
			}
		}
	}
	
	public void openRecordType(IPipeLineSession session, String streamId, ParameterResolutionContext prc) throws Exception {
		Writer w = getWriter(session, streamId, false, prc);
		if (w != null && ! StringUtils.isEmpty(getPrefix())) {
			write(session, streamId, getPrefix(), prc);
		}
	}

	public void closeRecordType(IPipeLineSession session, String streamId, ParameterResolutionContext prc) throws Exception {
		Writer w = getWriter(session, streamId, false, prc);
		if (w != null && ! StringUtils.isEmpty(getSuffix())) {
			write(session, streamId, getSuffix(), prc);
		}
	}

	protected String replacePattern(String target, String blockName) {
		if (StringUtils.isEmpty(target)) {
			return null;
		}
		if (StringUtils.isEmpty(getBlockNamePattern())) {
			return target;
		}
		String result=target.replaceAll(getBlockNamePattern(),blockName);
		//if (log.isDebugEnabled()) log.debug("target ["+target+"] pattern ["+getBlockNamePattern()+"] value ["+blockName+"] result ["+result+"]");
		return result;   
	}

	public void openBlock(IPipeLineSession session, String streamId, String blockName, ParameterResolutionContext prc) throws Exception  {
		write(session,streamId, replacePattern(getOnOpenBlock(),blockName), prc);
	}
	public void closeBlock(IPipeLineSession session, String streamId, String blockName, ParameterResolutionContext prc) throws Exception {
		write(session,streamId, replacePattern(getOnCloseBlock(),blockName), prc);
	}


	protected Writer getWriter(IPipeLineSession session, String streamId, boolean create, ParameterResolutionContext prc) throws Exception {
		//log.debug("getWriter ["+streamId+"], create ["+create+"]");
		Writer writer;
		writer = (Writer)openWriters.get(streamId);
		if (writer != null) {
			return writer;
		}
		
		if (!create) {
			return null;
		}
		writer = createWriter(session,streamId, prc);
		if (writer==null) {
			throw new IOException("cannot get writer for stream ["+streamId+"]");
		}
		openWriters.put(streamId,writer);
		return writer;		
	}

	
	@IbisDoc({"string that is written before any data of results is written", "&lt;document name=&quot;#name#&quot;&gt;"})
	public void setOnOpenDocument(String line) {
		onOpenDocument = line;
	}
	public String getOnOpenDocument() {
		return onOpenDocument;
	}

	@IbisDoc({"string that is written after all data of results is written", "&lt;/document&gt;"})
	public void setOnCloseDocument(String line) {
		onCloseDocument = line;
	}
	public String getOnCloseDocument() {
		return onCloseDocument;
	}

	@IbisDoc({"string that is written before the start of each logical block, as defined in the flow", "&lt;#name#&gt;"})
	public void setOnOpenBlock(String line) {
		onOpenBlock = line;
	}
	public String getOnOpenBlock() {
		return onOpenBlock;
	}

	@IbisDoc({"string that is written after the end of each logical block, as defined in the flow", "&lt;/#name#&gt;"})
	public void setOnCloseBlock(String line) {
		onCloseBlock = line;
	}
	public String getOnCloseBlock() {
		return onCloseBlock;
	}

	@IbisDoc({"string that is replaced by name of block or name of stream in above strings", "#name#"})
	public void setBlockNamePattern(String pattern) {
		blockNamePattern = pattern;
	}
	public String getBlockNamePattern() {
		return blockNamePattern;
	}

}
