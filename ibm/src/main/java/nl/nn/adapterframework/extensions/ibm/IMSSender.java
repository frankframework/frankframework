/*
   Copyright 2020 WeAreFrank!

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
package nl.nn.adapterframework.extensions.ibm;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Session;
import javax.naming.NamingException;
import javax.xml.transform.TransformerException;

import org.apache.commons.lang.StringUtils;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.jms.JmsSender;
import nl.nn.adapterframework.soap.SoapWrapper;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.StreamUtil;

/**
 * JMS sender which will add an IMS header to the message and call the MQ specific logic.
 *
 * <p>See {@link JmsSender} for configuration</p>
 *
 * @author Ricardo van Holst
 */

public class IMSSender extends MQSender {
	// IMS Header length
	private static final int IIH_HEADERSIZE = 88;
	
	// IMS Header fields
	private static final String	  IIH_HEADER_STRUCT_ID		= "IIH ";		// MQIIH_STRUC_ID (4 pos) 
	private static final int	  IIH_HEADER_VERSION		= 1;			// MQIIH_VERSION_1 (4 pos)
	private static final int	  IIH_HEADER_LENGTH			= 84;			// MQIIH_LENGTH_1 (4 pos)
	private static final int	  IIH_HEADER_ENCODING		= 0;			// MQ reserved (4 pos)
	private static final int	  IIH_HEADER_CODECHARSET	= 0;			// MQ reserved (4 pos)
	private static final String   IIH_HEADER_FORMAT			= "MQIMSVS ";	// MQFMT-IMS-VAR-STRING (8 pos)
	private	static final int      IIH_HEADER_FLAGS			= 0;			// MQIIH-NONE (4 pos)
	private static final String   IIH_HEADER_LTERM_OR		= "        ";	// (8 pos)
	private static final String   IIH_HEADER_MFS_MAPNAME	= "MQIMSVS ";	// (8 pos)
	private static final String   IIH_HEADER_REPLY_FORMAT	= "MQIMSVS ";	// (8 pos)
	private	static final String   IIH_HEADER_MFS_AUTH		= "        ";	// Password (8 pos)
	private static final byte[]   IIH_HEADER_TRAN_INSTANCE	= new byte[16];	// Only relevant for conversation (16 pos)
	private static final String   IIH_HEADER_TRAN_STATE		= " ";			// MQITS_NOT_IN_CONVERSATION (1 pos)
	private static final String   IIH_HEADER_COMMIT_MODE	= "1";			// MQICM-SEND-THEN-COMMIT (1 pos)
	private static final String   IIH_HEADER_SECURITY_SCOPE	= "C";			// MQISS-CHECK (1 pos)
	private static final String   IIH_HEADER_RESERVED		= " ";			// Reserved (1 pos)
	
	// MQ Fields
	private static final String MQC_MQFMT_IMS	= "MQIMS   ";	// (8 pos)
	private static final int MQENC_NATIVE = 273;				// copied from com.ibm.mq.MQC in com.ibm.mq.jar
	private static final int CCSID_ISO_8859_1 = 819;
	
	private static final Charset CHARSET = StandardCharsets.ISO_8859_1;
	
	private String transactionCode;
	
	/**
	 * The transaction code that should be added in the header, must be 8 characters
	 */
	@IbisDoc({"transaction code that should be added to the header, must be 8 characters", ""})
	public void setTransactionCode(String transactionCode) {
		this.transactionCode = transactionCode;
	}
	public String getTransactionCode() {
		return transactionCode;
	}

	@Override
	public void configure() throws ConfigurationException {
		if (StringUtils.isEmpty(getTransactionCode())) {
			throw new ConfigurationException("transactionCode must be specified");
		}
		if (StringUtils.length(getTransactionCode()) != 8) {
			throw new ConfigurationException("transactionCode must be 8 positions, current code [" + getTransactionCode() + "] with length [" + StringUtils.length(getTransactionCode()) + "]");
		}
		super.configure();
	}
	
	@Override
	public javax.jms.Message createMessage(Session session, String correlationID, String message) throws NamingException, JMSException {
		
		BytesMessage bytesMessage = null;
		bytesMessage = session.createBytesMessage();
		
		setMessageCorrelationID(bytesMessage, correlationID);
		
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		
		try {
			bos.write(IIH_HEADER_STRUCT_ID.getBytes(CHARSET));
			bos.write(intToBytes(IIH_HEADER_VERSION));
			bos.write(intToBytes(IIH_HEADER_LENGTH));
			bos.write(intToBytes(IIH_HEADER_ENCODING));
			bos.write(intToBytes(IIH_HEADER_CODECHARSET));
			bos.write(IIH_HEADER_FORMAT.getBytes(CHARSET));
			bos.write(intToBytes(IIH_HEADER_FLAGS));
			bos.write(IIH_HEADER_LTERM_OR.getBytes(CHARSET));
			bos.write(IIH_HEADER_MFS_MAPNAME.getBytes(CHARSET));
			bos.write(IIH_HEADER_REPLY_FORMAT.getBytes(CHARSET));
			bos.write(IIH_HEADER_MFS_AUTH.getBytes(CHARSET));
			bos.write(IIH_HEADER_TRAN_INSTANCE);
			bos.write(IIH_HEADER_TRAN_STATE.getBytes(CHARSET));
			bos.write(IIH_HEADER_COMMIT_MODE.getBytes(CHARSET));
			bos.write(IIH_HEADER_SECURITY_SCOPE.getBytes(CHARSET));
			bos.write(IIH_HEADER_RESERVED.getBytes(CHARSET));
			
			byte[] data = message.getBytes(CHARSET);

			bos.write(shortToBytes(data.length + 13)); //LL, +13 is for LL, ZZ and transaction code bytes
			bos.write(new byte[2]); //ZZ
			bos.write((transactionCode + " ").getBytes(CHARSET));
			
			bos.write(data);
			
			bos.toByteArray();
		} catch (IOException e) {
			// Should never happen
			throw new RuntimeException(e);
		}
		
		bytesMessage.writeBytes(bos.toByteArray());
		
		// Set Properties
		bytesMessage.setIntProperty("JMS_IBM_Encoding", MQENC_NATIVE);
		bytesMessage.setIntProperty("JMS_IBM_Character_Set", CCSID_ISO_8859_1);	
		bytesMessage.setStringProperty("JMS_IBM_Format", MQC_MQFMT_IMS);	

		return bytesMessage;
	}

	@Override
	public Message extractMessage(Object rawMessage, Map<String,Object> context, boolean soap, String soapHeaderSessionKey, SoapWrapper soapWrapper) throws JMSException, SAXException, TransformerException, IOException {
		BytesMessage message;
		try {
			message = (BytesMessage)rawMessage;
		} catch (ClassCastException e) {
			log.error("message received by listener on ["+ getDestinationName()+ "] was not of type BytesMessage, but ["+rawMessage.getClass().getName()+"]", e);
			return null;
		}
		
		String charset = message.getStringProperty("JMS_IBM_Character_Set");
		
		byte[] headerBuffer = new byte[IIH_HEADERSIZE];
		message.readBytes(headerBuffer);
		
		// Put header fields in the context
		ByteBuffer byteBuffer = ByteBuffer.wrap(headerBuffer);
		context.put("MQIIH_StrucID", byteToString(byteBuffer, charset, 4));
		context.put("MQIIH_Version", byteBuffer.getInt());
		context.put("MQIIH_StrucLength", byteBuffer.getInt());
		context.put("MQIIH_Encoding", byteBuffer.getInt());
		context.put("MQIIH_CodedCharSetId", byteBuffer.getInt());
		context.put("MQIIH_Format", byteToString(byteBuffer, charset, 8));
		context.put("MQIIH_Flags", byteBuffer.getInt());
		context.put("MQIIH_LTermOverride", byteToString(byteBuffer, charset, 8));
		context.put("MQIIH_MFSMapName", byteToString(byteBuffer, charset, 8));
		context.put("MQIIH_ReplyToFormat", byteToString(byteBuffer, charset, 8));
		context.put("MQIIH_Authenticator", byteToString(byteBuffer, charset, 8));
		context.put("MQIIH_TranInstanceId", byteToString(byteBuffer, charset, 16));
		context.put("MQIIH_TranState", byteToString(byteBuffer, charset, 1));
		context.put("MQIIH_CommitMode", byteToString(byteBuffer, charset, 1));
		context.put("MQIIH_SecurityScope", byteToString(byteBuffer, charset, 1));
		context.put("MQIIH_Reserved", byteToString(byteBuffer, charset, 1));
		
		int readBufferLength = (int)message.getBodyLength() - IIH_HEADERSIZE; // Get the length of the message to extract
		
		byte[] readBuffer = new byte[readBufferLength];
		message.readBytes(readBuffer);
		
		if (StreamUtil.DEFAULT_INPUT_STREAM_ENCODING.equals(charset)) {
			return new Message(readBuffer);
		}
		return new Message(new String(readBuffer, charset));
	}
	
	private String byteToString(ByteBuffer byteBuffer, String charset, int size) throws UnsupportedEncodingException {
		byte[] bytes = new byte[size];
		byteBuffer.get(bytes);
		
		return new String(bytes, charset);
	}
	
	private byte[] intToBytes(int i) {
		ByteBuffer bb = ByteBuffer.allocate(4);
		bb.putInt(i);
		return bb.array(); 
	}
	
	private byte[] shortToBytes(int i) {
		ByteBuffer bb = ByteBuffer.allocate(2);
		bb.putShort((short) i);
		return bb.array(); 
	}
}