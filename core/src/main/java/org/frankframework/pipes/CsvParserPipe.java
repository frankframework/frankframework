/*
   Copyright 2021-2024 WeAreFrank!

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
package org.frankframework.pipes;

import java.io.IOException;
import java.io.Reader;
import java.util.Map.Entry;

import lombok.Getter;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.doc.EnterpriseIntegrationPattern;
import org.frankframework.doc.EnterpriseIntegrationPattern.Type;
import org.frankframework.stream.Message;
import org.frankframework.stream.MessageBuilder;
import org.frankframework.xml.SaxDocumentBuilder;
import org.frankframework.xml.SaxElementBuilder;
import org.xml.sax.SAXException;

/**
 * Reads a message in CSV format, and turns it into XML.
 *
 * @author Gerrit van Brakel
 *
 */
@EnterpriseIntegrationPattern(Type.TRANSLATOR)
public class CsvParserPipe extends FixedForwardPipe {

	private @Getter Boolean fileContainsHeader;
	private @Getter String fieldNames;
	private @Getter String fieldSeparator;
	private @Getter HeaderCase headerCase=null;
	private @Getter boolean prettyPrint=false;
	private boolean useControlCodes;

	private CSVFormat format;

	public enum HeaderCase {
		LOWERCASE,
		UPPERCASE
	}

	@Override
	public void configure() throws ConfigurationException {
		super.configure();

		CSVFormat.Builder builder = CSVFormat.Builder.create(CSVFormat.DEFAULT);

		if (StringUtils.isNotEmpty(getFieldNames())) {
			builder.setHeader(getFieldNames().split(","));
			builder.setSkipHeaderRecord(getFileContainsHeader()!=null && getFileContainsHeader());
		} else {
			if (getFileContainsHeader()==null || getFileContainsHeader()) {
				builder.setHeader();
				builder.setSkipHeaderRecord(true);
			} else {
				throw new ConfigurationException("No fieldNames specified, and fileContainsHeader=false");
			}
		}

		if(StringUtils.isNotEmpty(getFieldSeparator()) && useControlCodes) {
			throw new ConfigurationException("cannot use fieldSeparator in combination with useControlCodes");
		}

		if(useControlCodes) {
			builder.setRecordSeparator((char) 30);
			builder.setDelimiter((char) 31);
		} else if (StringUtils.isNotEmpty(getFieldSeparator())) {
			String separator = getFieldSeparator();
			if (separator.length()>1) {
				throw new ConfigurationException("Illegal value for fieldSeparator ["+separator+"], can only be a single character");
			}
			builder.setDelimiter(getFieldSeparator().charAt(0));
		}

		format = builder.build();
	}

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		try {
			MessageBuilder messageBuilder = new MessageBuilder();
			try (Reader reader = message.asReader()) {
				try (SaxDocumentBuilder document = new SaxDocumentBuilder("csv", messageBuilder.asXmlWriter(), isPrettyPrint())) {
					CSVParser csvParser = format.parse(reader);
					for (CSVRecord csvRecord : csvParser) {
						processCsvRecord(csvRecord, document);
					}
				}
			}
			return new PipeRunResult(getSuccessForward(), messageBuilder.build());
		} catch (IOException | SAXException e) {
			throw new PipeRunException(this, "Cannot parse CSV", e);
		}
	}

	private void processCsvRecord(final CSVRecord csvRecord, final SaxDocumentBuilder document) throws PipeRunException {
		try (SaxElementBuilder element = document.startElement("record")) {
			for (Entry<String,String> entry: csvRecord.toMap().entrySet()) {
				String key = entry.getKey();
				if (getHeaderCase() != null) {
					key = getHeaderCase() == HeaderCase.LOWERCASE ? key.toLowerCase() : key.toUpperCase();
				}
				element.addElement(key, entry.getValue());
			}
		} catch (SAXException e) {
			throw new PipeRunException(this, "Exception caught at line ["+ csvRecord.getRecordNumber()+"] pos ["+ csvRecord.getCharacterPosition()+"]", e);
		}
	}

	/**
	 * Specifies if the first line should be treated as header or as data
	 * @ff.default true
	 */
	public void setFileContainsHeader(Boolean fileContainsHeader) {
		this.fileContainsHeader = fileContainsHeader;
	}

	/** Comma separated list of header names. If set, then <code>fileContainsHeader</code> defaults to false. If not set, headers are taken from the first line */
	public void setFieldNames(String fieldNames) {
		this.fieldNames = fieldNames;
	}

	/**
	 * Character that separates fields
	 * @ff.default ,
	 */
	public void setFieldSeparator(String fieldSeparator) {
		this.fieldSeparator = fieldSeparator;
	}

	/** When set, character casing will be changed for the header */
	public void setHeaderCase(HeaderCase headerCase) {
		this.headerCase = headerCase;
	}

	/** Format the XML output in easy legible way */
	public void setPrettyPrint(boolean prettyPrint) {
		this.prettyPrint = prettyPrint;
	}

	/**
	 * Enables the ASCII {@code (RS) Record Separator} and {@code (US) Unit Separator} Control Code field delimiters.
	 * May not be used in combination with attribute {@code fieldSeparator}.
	 * See <a href="https://en.wikipedia.org/wiki/C0_and_C1_control_codes#Field_separators">WIKI Control Codes</a>.
	 */
	public void setUseControlCodes(boolean useControlCodes) {
		this.useControlCodes = useControlCodes;
	}

}
