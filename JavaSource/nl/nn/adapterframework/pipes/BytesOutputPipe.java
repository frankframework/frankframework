/*
 * $Log: BytesOutputPipe.java,v $
 * Revision 1.1  2008-03-27 11:00:15  europe\L190409
 * first version
 *
 */
package nl.nn.adapterframework.pipes;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.util.Variant;

import org.apache.commons.lang.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;


/**
 * Output bytes as specified by the input XML. 
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.pipes.BytesOutputPipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Pipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMaxThreads(int) maxThreads}</td><td>maximum number of threads that may call {@link #doPipe(Object, PipeLineSession)} simultaneously</td><td>0 (unlimited)</td></tr>
 * <tr><td>{@link #setDurationThreshold(long) durationThreshold}</td><td>if durationThreshold >=0 and the duration (in milliseconds) of the message processing exceeded the value specified, then the message is logged informatory</td><td>-1</td></tr>
 * <tr><td>{@link #setGetInputFromSessionKey(String) getInputFromSessionKey}</td><td>when set, input is taken from this session key, instead of regular input</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setStoreResultInSessionKey(String) storeResultInSessionKey}</td><td>when set, the result is stored under this session key</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setPreserveInput(boolean) preserveInput}</td><td>when set <code>true</code>, the input of a pipe is restored before processing the next one</td><td>false</td></tr>
 * <tr><td>{@link #setNamespaceAware(boolean) namespaceAware}</td><td>controls namespace-awareness of possible XML parsing in descender-classes</td><td>application default</td></tr>
 * <tr><td>{@link #setTransactionAttribute(String) transactionAttribute}</td><td>Defines transaction and isolation behaviour. Equal to <A href="http://java.sun.com/j2ee/sdk_1.2.1/techdocs/guides/ejb/html/Transaction2.html#10494">EJB transaction attribute</a>. Possible values are: 
 *   <table border="1">
 *   <tr><th>transactionAttribute</th><th>callers Transaction</th><th>Pipe excecuted in Transaction</th></tr>
 *   <tr><td colspan="1" rowspan="2">Required</td>    <td>none</td><td>T2</td></tr>
 * 											      <tr><td>T1</td>  <td>T1</td></tr>
 *   <tr><td colspan="1" rowspan="2">RequiresNew</td> <td>none</td><td>T2</td></tr>
 * 											      <tr><td>T1</td>  <td>T2</td></tr>
 *   <tr><td colspan="1" rowspan="2">Mandatory</td>   <td>none</td><td>error</td></tr>
 * 											      <tr><td>T1</td>  <td>T1</td></tr>
 *   <tr><td colspan="1" rowspan="2">NotSupported</td><td>none</td><td>none</td></tr>
 * 											      <tr><td>T1</td>  <td>none</td></tr>
 *   <tr><td colspan="1" rowspan="2">Supports</td>    <td>none</td><td>none</td></tr>
 * 											      <tr><td>T1</td>  <td>T1</td></tr>
 *   <tr><td colspan="1" rowspan="2">Never</td>       <td>none</td><td>none</td></tr>
 * 											      <tr><td>T1</td>  <td>error</td></tr>
 *  </table></td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setBeforeEvent(int) beforeEvent}</td>      <td>METT eventnumber, fired just before a message is processed by this Pipe</td><td>-1 (disabled)</td></tr>
 * <tr><td>{@link #setAfterEvent(int) afterEvent}</td>        <td>METT eventnumber, fired just after message processing by this Pipe is finished</td><td>-1 (disabled)</td></tr>
 * <tr><td>{@link #setExceptionEvent(int) exceptionEvent}</td><td>METT eventnumber, fired when message processing by this Pipe resulted in an exception</td><td>-1 (disabled)</td></tr>
 * <tr><td>{@link #setForwardName(String) forwardName}</td>  <td>name of forward returned upon completion</td><td>"success"</td></tr>
 * </table>
 * </p>
 * <p><b>Exits:</b>
 * <table border="1">
 * <tr><th>state</th><th>condition</th></tr>
 * <tr><td>"success"</td><td>default</td></tr>
 * <tr><td><i>{@link #setForwardName(String) forwardName}</i></td><td>if specified</td></tr>
 * </table>
 * </p>
 *
 * Actions are taken on every field
 * tag found in the input XML. Every field tag should have a type attribute
 * that specifies the type of conversion that needs to be done on the string
 * specified by the value attribute. A value attribute also needs to be present
 * for every field tag. Currently two types of conversion are supported:
 * 
 * <ul>
 *   <li><code>GetBytesFromString</code>, a conversion from string to bytes as specified by java.lang.String.getBytes(String charsetName)</li>
 *   <li><code>PackedDecimal</code>, a conversion from string to Packed-decimal</li>
 * </ul>
 *  
 * An additional charset attribute is needed for a GetBytesFromString
 * conversion. An input XML that would encode the string &quot; TEST 1234 &quot;
 * into EBCDIC format would look like:
 * 
 * <pre>
 * &lt;fields&gt;
 *   &lt;field type=&quot;GetBytesFromString&quot; value=&quot; TEST 1234 &quot; charset=&quot;Cp037&quot;/&gt;
 * &lt;/fields&gt;
 * </pre>
 * 
 * The Packed-decimal conversion has been implemented according to information
 * found in the following resources:
 * 
 * <ul>
 *   <li>A description as found at <a href="http://www.simotime.com/datapk01.htm">http://www.simotime.com/datapk01.htm</a></li>
 *   <li>AS400PackedDecimal.java from jtopen_6_1_source.zip downloaded at <a href="http://jt400.sourceforge.net/">http://jt400.sourceforge.net/</a></li>
 *   <li>The WebSphere Studio COBOL for Windows Programming Guide from <a href="http://www-1.ibm.com/support/docview.wss?uid=swg27005151">http://www-1.ibm.com/support/docview.wss?uid=swg27005151</a></li>
 * </ul>
 * 
 * Some examples:
 * 
 * <ul>
 *   <li>The string +12345 will be translated to three bytes with the following hexadecimal representation: 12 34 5C</li>
 *   <li>The string -12345 will be translated to three bytes with the following hexadecimal representation: 12 34 5D</li>
 *   <li>The string 12345 will be translated to three bytes with the following hexadecimal representation: 12 34 5F</li>
 *   <li>The string 1234 will be translated to three bytes with the following hexadecimal representation: 01 23 4F</li>
 * </ul>
 *
 * The Packed-decimal is prefixed with zero's when the specified size is bigger
 * than the number of decimals. An exception is thrown when the specified size
 * is smaller than the number of decimals.
 *
 * An input XML that would generate a number of Packed-decimals could look like:
 * 
 * <pre>
 * &lt;fields&gt;
 *   &lt;field type=&quot;PackedDecimal&quot; value=&quot;+12345&quot; size=&quot;16&quot;/&gt;
 *   &lt;field type=&quot;PackedDecimal&quot; value=&quot;+67890&quot; size=&quot;16&quot;/&gt;
 *   &lt;field type=&quot;PackedDecimal&quot; value=&quot;+1234&quot; size=&quot;16&quot;/&gt;
 *   &lt;field type=&quot;PackedDecimal&quot; value=&quot;-12345&quot; size=&quot;16&quot;/&gt;
 *   &lt;field type=&quot;PackedDecimal&quot; value=&quot;-67890&quot; size=&quot;16&quot;/&gt;
 *   &lt;field type=&quot;PackedDecimal&quot; value=&quot;-1234&quot; size=&quot;16&quot;/&gt;
 *   &lt;field type=&quot;PackedDecimal&quot; value=&quot;12345&quot; size=&quot;16&quot;/&gt;
 *   &lt;field type=&quot;PackedDecimal&quot; value=&quot;67890&quot; size=&quot;16&quot;/&gt;
 *   &lt;field type=&quot;PackedDecimal&quot; value=&quot;1234&quot; size=&quot;16&quot;/&gt;
 *   &lt;field type=&quot;PackedDecimal&quot; value=&quot;+1&quot; size=&quot;16&quot;/&gt;
 *   &lt;field type=&quot;PackedDecimal&quot; value=&quot;-1&quot; size=&quot;16&quot;/&gt;
 *   &lt;field type=&quot;PackedDecimal&quot; value=&quot;+12&quot; size=&quot;16&quot;/&gt;
 *   &lt;field type=&quot;PackedDecimal&quot; value=&quot;-12&quot; size=&quot;16&quot;/&gt;
 *   &lt;field type=&quot;PackedDecimal&quot; value=&quot;+123&quot; size=&quot;16&quot;/&gt;
 *   &lt;field type=&quot;PackedDecimal&quot; value=&quot;-123&quot; size=&quot;16&quot;/&gt;
 *   &lt;field type=&quot;PackedDecimal&quot; value=&quot;+1234&quot; size=&quot;16&quot;/&gt;
 *   &lt;field type=&quot;PackedDecimal&quot; value=&quot;-1234&quot; size=&quot;16&quot;/&gt;
 *   &lt;field type=&quot;PackedDecimal&quot; value=&quot;+12345&quot; size=&quot;16&quot;/&gt;
 *   &lt;field type=&quot;PackedDecimal&quot; value=&quot;-12345&quot; size=&quot;16&quot;/&gt;
 *   &lt;field type=&quot;PackedDecimal&quot; value=&quot;+123456&quot; size=&quot;16&quot;/&gt;
 *   &lt;field type=&quot;PackedDecimal&quot; value=&quot;-123456&quot; size=&quot;16&quot;/&gt;
 *   &lt;field type=&quot;PackedDecimal&quot; value=&quot;+1234567&quot; size=&quot;16&quot;/&gt;
 *   &lt;field type=&quot;PackedDecimal&quot; value=&quot;-1234567&quot; size=&quot;16&quot;/&gt;
 *   &lt;field type=&quot;PackedDecimal&quot; value=&quot;+12345678&quot; size=&quot;16&quot;/&gt;
 *   &lt;field type=&quot;PackedDecimal&quot; value=&quot;-12345678&quot; size=&quot;16&quot;/&gt;
 * &lt;/fields&gt;
 * </pre>
 * 
 * @author  Jaco de Groot (***@dynasol.nl)
 * @since   4.9
 * @version Id
 */
public class BytesOutputPipe extends FixedForwardPipe {

	public PipeRunResult doPipe(Object input, PipeLineSession session) throws PipeRunException {
		Object result = null;
		Variant in = new Variant(input);

		try {
			XMLReader parser = XMLReaderFactory.createXMLReader();
			FieldsContentHandler fieldsContentHandler = new FieldsContentHandler();
			parser.setContentHandler(fieldsContentHandler);
			parser.parse(in.asXmlInputSource());
			result = fieldsContentHandler.getResult();
		} catch (SAXException e) {
			throw new PipeRunException(this, "SAXException", e);
		} catch (IOException e) {
			throw new PipeRunException(this, "IOException", e);
		}
		return new PipeRunResult(getForward(), result);
	}

	private class FieldsContentHandler extends DefaultHandler {
		private byte[] result = new byte[0];

		public void startElement(String namespaceURI, String localName, String qName, Attributes attributes) throws SAXException {
			if ("field".equals(qName)) {
				String type = attributes.getValue("type");
				if ("GetBytesFromString".equals(type)) {
					String charset = attributes.getValue("charset");
					if (StringUtils.isNotEmpty(charset)) {
						String value = attributes.getValue("value");
						if (value != null) {
							try {
								appendToResult(value.getBytes(charset));
							} catch (UnsupportedEncodingException e) {
								throw new SAXException("UnsupportedEncodingException for charset ["	+ charset + "] for value [" + value + "]", e);
							}
						} else {
							throw new SAXException("No value found for field with type ["+ type + "] and charset [" + charset + "]");
						}
					} else {
						throw new SAXException("No charset specified for field with type ["+ type + "]");
					}
				} else if ("PackedDecimal".equals(type)) {
					String value = attributes.getValue("value");
					boolean positiveSign = false;
					boolean negativeSign = false;
					if (value == null) {
						throw new SAXException("Value is null for field with type [" + type + "]");
					}
					if (value.length() < 1) {
						throw new SAXException("Value is empty for field with type [" + type + "]");
					}
					if (value.length() > 1) {
						if (value.charAt(0) == '+') {
							positiveSign = true;
							value = value.substring(1);
						} else if (value.charAt(0) == '-') {
							negativeSign = true;
							value = value.substring(1);
						}
					}
					for (int i = 0; i < value.length(); i++) {
						if (!Character.isDigit(value.charAt(i))) {
							throw new SAXException("Value [" + value + "] is not a valid number for field with type [" + type + "]");
						}
					}
					String sizeString = attributes.getValue("size");
					if (sizeString == null) {
						throw new SAXException("Size is null for field with type [" + type + "] and value [" + value + "]");
					}
					if (sizeString.length() < 1) {
						throw new SAXException("Size is empty for field with type [" + type + "] and value [" + value + "]");
					}
					for (int i = 0; i < sizeString.length(); i++) {
						if (!Character.isDigit(sizeString.charAt(i))) {
							throw new SAXException("Size [" + sizeString + "] is not a valid number for field with type [" + type + "] and value [" + value + "]");
						}
					}
					int size = new Integer(sizeString).intValue();
					byte[] bytes = new byte[size];
					for (int i = 0; i < bytes.length; i++) {
						bytes[i] = 0;
					}
					if (size < 1) {
						throw new SAXException("Size is smaller than 1 for field with type [" + type + "] and value [" + value + "]");
					}
					int firstNibble;
					int secondNibble;
					if (positiveSign) {
						secondNibble = 0x000C;
					} else if (negativeSign) {
						secondNibble = 0x000D;
					} else {
						secondNibble = 0x000F;
					}
					int valuePos = value.length() - 1;
					int bytesPos = bytes.length - 1;
					firstNibble = (value.charAt(valuePos) & 0x000F) << 4;
					bytes[bytesPos] = (byte)(firstNibble + secondNibble);
					valuePos--;
					bytesPos--;
					while (valuePos > -1 && bytesPos > -1) {
						secondNibble = value.charAt(valuePos) & 0x000F;
						if (valuePos == 0) {
							firstNibble = ('0' & 0x000F) << 4;
						} else {
							valuePos--;
							firstNibble = (value.charAt(valuePos) & 0x000F) << 4;
						}
						bytes[bytesPos] = (byte)(firstNibble + secondNibble);
						valuePos--;
						bytesPos--;
					}
					if (valuePos > -1) {
						throw new SAXException("Packed-decimal value doesn't fit in the specified size [" + size + "] for value [" + value + "]");
					}
					appendToResult(bytes);
				} else {
					throw new SAXException("Unsupported conversion type [" + type + "]");
				}
			}
		}

		private void appendToResult(byte[] bytes) {
			byte[] newResult = new byte[result.length + bytes.length];
			System.arraycopy(result, 0, newResult, 0, result.length);
			System.arraycopy(bytes, 0, newResult, result.length, bytes.length);
			result = newResult;
		}
	
		public byte[] getResult() {
			return result;
		}
	}


}

