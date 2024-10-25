/*
   Copyright 2013 Nationale-Nederlanden, 2020-2022 WeAreFrank!

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
package org.frankframework.batch;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;

import org.frankframework.core.PipeLineSession;

/**
 * Baseclass for resulthandlers that write the transformed record to a writer.
 *
 * @author  Gerrit van Brakel
 * @since   4.7
 */
public abstract class ResultWriter extends AbstractResultHandler {

	private @Getter String onOpenDocument="<document name=\"#name#\">";
	private @Getter String onCloseDocument="</document>";
	private @Getter String onOpenBlock="<#name#>";
	private @Getter String onCloseBlock="</#name#>";
	private @Getter String blockNamePattern="#name#";

	private final Map<String, Writer> openWriters = Collections.synchronizedMap(new HashMap<>());

	protected abstract Writer createWriter(PipeLineSession session, String streamId) throws Exception;

	@Override
	public void openDocument(PipeLineSession session, String streamId) throws Exception {
		super.openDocument(session, streamId);
		getWriter(session, streamId, true);
		write(session,streamId,replacePattern(getOnOpenDocument(),streamId));
	}

	@Override
	public void closeDocument(PipeLineSession session, String streamId) {
		try (Writer ignored = openWriters.remove(streamId)) {
			// just close the writer
		} catch (IOException e) {
			log.error("Exception closing [{}]", streamId, e);
		}
		super.closeDocument(session,streamId);
	}

	@Override
	public String finalizeResult(PipeLineSession session, String streamId, boolean error) throws Exception {
		log.debug("finalizeResult [{}]", streamId);
		write(session,streamId,replacePattern(getOnCloseDocument(),streamId));
		return null;
	}

	@Override
	public void handleResult(PipeLineSession session, String streamId, String recordKey, String result) throws Exception {
		write(session, streamId, result);
	}

	protected void writeNewLine(Writer w) throws IOException {
		if (w instanceof BufferedWriter writer) {
			writer.newLine();
		} else {
			w.write("\n");
		}
	}

	private void write(PipeLineSession session, String streamId, String line) throws Exception {
		if (line!=null) {
			Writer w = getWriter(session, streamId, false);
			if (w==null) {
				throw new NullPointerException("No Writer Found for stream ["+streamId+"]");
			}
			w.write(line);
			writeNewLine(w);
		}
	}

	@Override
	public void openRecordType(PipeLineSession session, String streamId) throws Exception {
		Writer w = getWriter(session, streamId, false);
		if (w != null && ! StringUtils.isEmpty(getPrefix())) {
			write(session, streamId, getPrefix());
		}
	}

	@Override
	public void closeRecordType(PipeLineSession session, String streamId) throws Exception {
		Writer w = getWriter(session, streamId, false);
		if (w != null && ! StringUtils.isEmpty(getSuffix())) {
			write(session, streamId, getSuffix());
		}
	}

	protected String replacePattern(String target, String blockName) {
		if (StringUtils.isEmpty(target)) {
			return null;
		}
		if (StringUtils.isEmpty(getBlockNamePattern())) {
			return target;
		}
		return target.replaceAll(getBlockNamePattern(),blockName);
	}

	@Override
	public void openBlock(PipeLineSession session, String streamId, String blockName, Map<String, Object> blocks) throws Exception  {
		write(session,streamId, replacePattern(getOnOpenBlock(),blockName));
	}

	@Override
	public void closeBlock(PipeLineSession session, String streamId, String blockName, Map<String, Object> blocks) throws Exception {
		write(session,streamId, replacePattern(getOnCloseBlock(),blockName));
	}

	protected Writer getWriter(PipeLineSession session, String streamId, boolean create) throws Exception {
		Writer writer = openWriters.get(streamId);
		if (writer != null) {
			return writer;
		}
		if (!create) {
			return null;
		}
		writer = createWriter(session,streamId);
		if (writer==null) {
			throw new IOException("cannot get writer for stream ["+streamId+"]");
		}
		openWriters.put(streamId,writer);
		return writer;
	}

	/**
	 * string that is written before any data of results is written
	 * @ff.default &lt;document name=&quot;#name#&quot;&gt;
	 */
	public void setOnOpenDocument(String line) {
		onOpenDocument = line;
	}

	/**
	 * string that is written after all data of results is written
	 * @ff.default &lt;/document&gt;
	 */
	public void setOnCloseDocument(String line) {
		onCloseDocument = line;
	}

	/**
	 * string that is written before the start of each logical block, as defined in the flow
	 * @ff.default &lt;#name#&gt;
	 */
	public void setOnOpenBlock(String line) {
		onOpenBlock = line;
	}

	/**
	 * string that is written after the end of each logical block, as defined in the flow
	 * @ff.default &lt;/#name#&gt;
	 */
	public void setOnCloseBlock(String line) {
		onCloseBlock = line;
	}

	/**
	 * string that is replaced by name of block or name of stream in above strings
	 * @ff.default #name#
	 */
	public void setBlockNamePattern(String pattern) {
		blockNamePattern = pattern;
	}

}
