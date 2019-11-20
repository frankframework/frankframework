/*
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

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.doc.IbisDoc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import com.itextpdf.html2pdf.HtmlConverter;


/**
 * This pipe can handle HTML documents and convert it to a document format of your choice. Output is a ByteArrayInputStream.
 * @author Tom van der Heijden
 */
public class HtmlToDocumentPipe extends FixedForwardPipe {
	
	private String documentFormat = "pdf";
    
	@Override
	public PipeRunResult doPipe(Object input, IPipeLineSession session) throws PipeRunException {
		InputStream stream = new ByteArrayInputStream(String.valueOf(input).getBytes(StandardCharsets.UTF_8));        
		ByteArrayInputStream inStream = null;
		
		if("pdf".equals(documentFormat)) {
			inStream = convertToPdf(stream);
		}
		// more document formats to be added later
   		return new PipeRunResult(getForward(), inStream);
	}
	
	private ByteArrayInputStream convertToPdf(InputStream stream) throws PipeRunException{
		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		ByteArrayInputStream inStream = null;
        try {
			HtmlConverter.convertToPdf(stream, outStream);
			inStream = new ByteArrayInputStream(outStream.toByteArray());
		} catch (Exception e) {
			throw new PipeRunException(this,e.toString() + " The HTML might be invalid.");
		}
        return inStream;
	}
	
	@IbisDoc({"Sets the output document format, choices are: 'pdf'","pdf"})
	public void setDocumentFormat(String documentFormat) {
		this.documentFormat = documentFormat;
	}
	
	public String getDocumentFormat() {
		return documentFormat;
	}

}
