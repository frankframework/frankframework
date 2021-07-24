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

import java.nio.charset.StandardCharsets;
import java.util.Enumeration;

import javax.jms.Destination;
import javax.jms.JMSException;

import javax.jms.BytesMessage;

public class TestJMSMessage implements BytesMessage {

	private byte[] messageContent = null;
	private int byteOffset = 0;
	
	@Override
	public String getJMSMessageID() throws JMSException {
		return "testmessageac13ecb1--30fe9225_16caa708707_-7fb1";
	}

	@Override
	public void setJMSMessageID(String id) throws JMSException {
		// TODO Auto-generated method stub

	}

	@Override
	public long getJMSTimestamp() throws JMSException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setJMSTimestamp(long timestamp) throws JMSException {
		// TODO Auto-generated method stub

	}

	@Override
	public byte[] getJMSCorrelationIDAsBytes() throws JMSException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setJMSCorrelationIDAsBytes(byte[] correlationID) throws JMSException {
		// TODO Auto-generated method stub

	}

	@Override
	public void setJMSCorrelationID(String correlationID) throws JMSException {
		// TODO Auto-generated method stub

	}

	@Override
	public String getJMSCorrelationID() throws JMSException {
		return "testmessageac13ecb1--30fe9225_16caa708707_-7fb2";
	}

	@Override
	public Destination getJMSReplyTo() throws JMSException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setJMSReplyTo(Destination replyTo) throws JMSException {
		// TODO Auto-generated method stub

	}

	@Override
	public Destination getJMSDestination() throws JMSException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setJMSDestination(Destination destination) throws JMSException {
		// TODO Auto-generated method stub

	}

	@Override
	public int getJMSDeliveryMode() throws JMSException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setJMSDeliveryMode(int deliveryMode) throws JMSException {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean getJMSRedelivered() throws JMSException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setJMSRedelivered(boolean redelivered) throws JMSException {
		// TODO Auto-generated method stub

	}

	@Override
	public String getJMSType() throws JMSException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setJMSType(String type) throws JMSException {
		// TODO Auto-generated method stub

	}

	@Override
	public long getJMSExpiration() throws JMSException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setJMSExpiration(long expiration) throws JMSException {
		// TODO Auto-generated method stub

	}

	@Override
	public int getJMSPriority() throws JMSException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setJMSPriority(int priority) throws JMSException {
		// TODO Auto-generated method stub

	}

	@Override
	public void clearProperties() throws JMSException {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean propertyExists(String name) throws JMSException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean getBooleanProperty(String name) throws JMSException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public byte getByteProperty(String name) throws JMSException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public short getShortProperty(String name) throws JMSException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getIntProperty(String name) throws JMSException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getLongProperty(String name) throws JMSException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public float getFloatProperty(String name) throws JMSException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getDoubleProperty(String name) throws JMSException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getStringProperty(String name) throws JMSException {
		return StandardCharsets.ISO_8859_1.name();
	}

	@Override
	public Object getObjectProperty(String name) throws JMSException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Enumeration getPropertyNames() throws JMSException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setBooleanProperty(String name, boolean value) throws JMSException {
		// TODO Auto-generated method stub

	}

	@Override
	public void setByteProperty(String name, byte value) throws JMSException {
		// TODO Auto-generated method stub

	}

	@Override
	public void setShortProperty(String name, short value) throws JMSException {
		// TODO Auto-generated method stub

	}

	@Override
	public void setIntProperty(String name, int value) throws JMSException {
		// TODO Auto-generated method stub

	}

	@Override
	public void setLongProperty(String name, long value) throws JMSException {
		// TODO Auto-generated method stub

	}

	@Override
	public void setFloatProperty(String name, float value) throws JMSException {
		// TODO Auto-generated method stub

	}

	@Override
	public void setDoubleProperty(String name, double value) throws JMSException {
		// TODO Auto-generated method stub

	}

	@Override
	public void setStringProperty(String name, String value) throws JMSException {
		// TODO Auto-generated method stub

	}

	@Override
	public void setObjectProperty(String name, Object value) throws JMSException {
		// TODO Auto-generated method stub

	}

	@Override
	public void acknowledge() throws JMSException {
		// TODO Auto-generated method stub

	}

	@Override
	public void clearBody() throws JMSException {
		// TODO Auto-generated method stub

	}

	@Override
	public long getBodyLength() throws JMSException {
		// TODO Auto-generated method stub
		return messageContent.length;
	}

	@Override
	public boolean readBoolean() throws JMSException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public byte readByte() throws JMSException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int readUnsignedByte() throws JMSException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public short readShort() throws JMSException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int readUnsignedShort() throws JMSException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public char readChar() throws JMSException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int readInt() throws JMSException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long readLong() throws JMSException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public float readFloat() throws JMSException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double readDouble() throws JMSException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String readUTF() throws JMSException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int readBytes(byte[] value) throws JMSException {
		int length = value.length;
		int to = byteOffset+length;
		int from = byteOffset;
		
		int newLength = to - from;
        if (newLength < 0) throw new IllegalArgumentException(from + " > " + to);
        int ceil = messageContent.length-from;
        int len = (ceil < newLength) ? ceil : newLength;
        System.arraycopy(messageContent, from, value, 0, len);
		
		byteOffset += length;
		return length;
	}

	@Override
	public int readBytes(byte[] value, int length) throws JMSException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeBoolean(boolean value) throws JMSException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void writeByte(byte value) throws JMSException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void writeShort(short value) throws JMSException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void writeChar(char value) throws JMSException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void writeInt(int value) throws JMSException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void writeLong(long value) throws JMSException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void writeFloat(float value) throws JMSException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void writeDouble(double value) throws JMSException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void writeUTF(String value) throws JMSException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void writeBytes(byte[] value) throws JMSException {
		// TODO Auto-generated method stub
		messageContent = value;
		
	}

	@Override
	public void writeBytes(byte[] value, int offset, int length) throws JMSException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void writeObject(Object value) throws JMSException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void reset() throws JMSException {
		// TODO Auto-generated method stub
		
	}

}
