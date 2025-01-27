/*
   Copyright 2013, 2020 Nationale-Nederlanden

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
import java.io.UnsupportedEncodingException;

import org.apache.commons.lang3.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.doc.EnterpriseIntegrationPattern;
import org.frankframework.doc.EnterpriseIntegrationPattern.Type;
import org.frankframework.stream.Message;
import org.frankframework.util.XmlUtils;


/**
 * Output bytes as specified by the input XML.
 *
 * Actions are taken on every field
 * tag found in the input XML. Every field tag should have a type attribute
 * that specifies the type of conversion that needs to be done on the string
 * specified by the value attribute. A value attribute also needs to be present
 * for every field tag. Currently, two types of conversion are supported:
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
 * The Packed-decimal is prefixed with zeroes when the specified size is bigger
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
 */
@EnterpriseIntegrationPattern(Type.TRANSLATOR)
public class BytesOutputPipe extends FixedForwardPipe {

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		try {
			FieldsContentHandler fieldsContentHandler = new FieldsContentHandler();
			XmlUtils.parseXml(message.asInputSource(), fieldsContentHandler);
			byte[] result = fieldsContentHandler.getResult();
			return new PipeRunResult(getSuccessForward(), result);
		} catch (SAXException e) {
			throw new PipeRunException(this, "SAXException", e);
		} catch (IOException e) {
			throw new PipeRunException(this, "IOException", e);
		}
	}

	private static class FieldsContentHandler extends DefaultHandler {
		private byte[] result = new byte[0];

		@Override
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
					if (value.isEmpty()) {
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
					if (sizeString.isEmpty()) {
						throw new SAXException("Size is empty for field with type [" + type + "] and value [" + value + "]");
					}
					for (int i = 0; i < sizeString.length(); i++) {
						if (!Character.isDigit(sizeString.charAt(i))) {
							throw new SAXException("Size [" + sizeString + "] is not a valid number for field with type [" + type + "] and value [" + value + "]");
						}
					}
					int size = Integer.parseInt(sizeString);
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

