/*
   Copyright 2021 WeAreFrank!

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
package nl.nn.adapterframework.pipes;

import java.io.Reader;
import java.util.Map.Entry;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.xml.sax.SAXException;

import lombok.Getter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.MessageOutputStream;
import nl.nn.adapterframework.stream.StreamingPipe;
import nl.nn.adapterframework.xml.SaxDocumentBuilder;
import nl.nn.adapterframework.xml.SaxElementBuilder;

public class CsvParserPipe extends StreamingPipe {
	
	private @Getter Boolean fileContainsHeader;
	private @Getter String fieldNames;
	private @Getter String fieldSeparator;
	
	private CSVFormat format = CSVFormat.DEFAULT;
	
	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if (StringUtils.isNotEmpty(getFieldNames())) {
			format = format.withHeader(getFieldNames().split(","))
						.withSkipHeaderRecord(getFileContainsHeader()!=null && getFileContainsHeader());
		} else {
			if (getFileContainsHeader()==null || getFileContainsHeader()) {
				format = format.withFirstRecordAsHeader();
			} else {
				throw new ConfigurationException("No fieldNames specified, and fileContainsHeader=false");
			}
		}
		if (StringUtils.isNotEmpty(getFieldSeparator())) {
			String separator = getFieldSeparator();
			if (separator.length()>1) {
				throw new ConfigurationException("Illegal value for fieldSeparator ["+separator+"], can only be a single character");
			}
			format = format.withDelimiter(getFieldSeparator().charAt(0));
		}
	}

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		try (MessageOutputStream target=getTargetStream(session)) {
			try (Reader reader = message.asReader()) {
				try (SaxDocumentBuilder document = new SaxDocumentBuilder("csv", target.asContentHandler())) {
					CSVParser csvParser = format.parse(reader);
					for (CSVRecord record : csvParser) {
						try (SaxElementBuilder element = document.startElement("record")) {
							for(Entry<String,String> entry:record.toMap().entrySet()) {
								element.addElement(entry.getKey(), entry.getValue());
							}
						} catch (SAXException e) {
							throw new PipeRunException(this, "Exception caught at line ["+record.getRecordNumber()+"] pos ["+record.getCharacterPosition()+"]", e);
						}
					}		
				}
			}
			return target.getPipeRunResult();
		} catch (Exception e) {
			if (e instanceof PipeRunException) {
				throw (PipeRunException)e;
			}
			throw new PipeRunException(this, "Cannot parse CSV", e);
		}	
	}

	@IbisDoc({"1", "Specifies if the first line should be treated as header or as data", "true"})
	public void setFileContainsHeader(boolean fileContainsHeader) {
		this.fileContainsHeader = fileContainsHeader;
	}

	@IbisDoc({"2", "Comma separated list of header names. If set, then fileContainsHeader defaults to false. If not set, headers are taken from the first line"})
	public void setFieldNames(String fieldNames) {
		this.fieldNames = fieldNames;
	}

	@IbisDoc({"3", "Character that separates fields",","})
	public void setFieldSeparator(String fieldSeparator) {
		this.fieldSeparator = fieldSeparator;
	}

}
