package nl.nn.adapterframework.extensions.aspose.pipe;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.pipes.FixedForwardPipe;
import nl.nn.adapterframework.stream.Message;
import com.aspose.pdf.Document;

/**
 * 
 * @author M63H114 Laurens Mäkel
 *
 */
public class AmountOfPagesPipe extends FixedForwardPipe {
	String charset = "ISO-8859-1";

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
	}

	@Override
	public PipeRunResult doPipe(Message input, IPipeLineSession session) throws PipeRunException {
		int result = 0;

		InputStream binaryInputStream;
		try {
			binaryInputStream = input.asInputStream(getCharset());
		} catch (IOException e1) {
			throw new PipeRunException(this,
						getLogPrefix(session) + "cannot encode message using charset [" + getCharset() + "]", e1);
		}
		
        Document doc = new Document(binaryInputStream);
        result = doc.getPages().size();
		
		return new PipeRunResult(getForward(), Integer.toString(result) );
	}

	public void setCharset(String charset){
		this.charset = charset;
	}

	public String getCharset(){
		return charset;
	}
}
