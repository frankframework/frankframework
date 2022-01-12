package nl.nn.adapterframework.http;

import java.io.ByteArrayOutputStream;
import java.net.URI;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import nl.nn.adapterframework.util.XmlUtils;

import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.entity.ByteArrayEntity;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


public class HttpReport extends HttpEntityEnclosingRequestBase {

	public final static String METHOD_NAME = "REPORT";

	/**
	 * @param uri to connect to
	 * @param element entity
	 * @throws TransformerException 
	 * @throws IllegalArgumentException if the uri is invalid.
	 */
	public HttpReport(final URI uri, Element element) throws TransformerException {
		super();
		setURI(uri);
		setHeader("Depth", "0");
		Document doc = element.getOwnerDocument();
		Source xmlSource = new DOMSource(doc);
		Transformer t = XmlUtils.getTransformerFactory(2).newTransformer();

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		Result outputTarget = new StreamResult(outputStream);
		t.transform(xmlSource, outputTarget);

		setEntity(new ByteArrayEntity(outputStream.toByteArray()));
	}

	/**
	 * @param uri to connect to
	 * @param element entity
	 * @throws TransformerException 
	 * @throws IllegalArgumentException if the uri is invalid.
	 */
	public HttpReport(final String uri, Element element) throws TransformerException {
		this(URI.create(uri), element);
	}

	@Override
	public String getMethod() {
		return METHOD_NAME;
	}
}
