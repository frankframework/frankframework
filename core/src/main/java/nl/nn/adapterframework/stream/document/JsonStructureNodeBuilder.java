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
package nl.nn.adapterframework.stream.document;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.function.Consumer;

import javax.json.JsonNumber;
import javax.json.JsonString;
import javax.json.JsonValue;

import lombok.Getter;

public class JsonStructureNodeBuilder extends NodeBuilder {

	private Consumer<JsonValue> register;
	private @Getter JsonValue root;
	
	public JsonStructureNodeBuilder(Consumer<JsonValue> register) {
		this.register = register;
	}

	public JsonStructureNodeBuilder() {
		register = (value) -> { root = value; };
	}
	
	@Override
	public void close() throws DocumentException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public ArrayBuilder startArray(String elementName) throws DocumentException {
		return new JsonStructureArrayDocumentBuilder(register);
	}

	@Override
	public ObjectBuilder startObject() throws DocumentException {
		return new JsonStructureObjectDocumentBuilder(register);
	}

	@Override
	public void setValue(String value) throws DocumentException {
		register.accept(new JsonString() {

			@Override
			public ValueType getValueType() {
				return ValueType.STRING;
			}

			@Override
			public String getString() {
				return value;
			}

			@Override
			public CharSequence getChars() {
				return value;
			}
		});
	}

	@Override
	public void setValue(long value) throws DocumentException {
		register.accept(new JsonNumber() {

			@Override
			public ValueType getValueType() {
				return ValueType.NUMBER;
			}

			@Override
			public boolean isIntegral() {
				return true;
			}

			@Override
			public int intValue() {
				return (int)value;
			}

			@Override
			public int intValueExact() {
				return (int)value;
			}

			@Override
			public long longValue() {
				return (int)value;
			}

			@Override
			public long longValueExact() {
				return (int)value;
			}

			@Override
			public BigInteger bigIntegerValue() {
				return new BigInteger(Long.toString(value));
			}

			@Override
			public BigInteger bigIntegerValueExact() {
				return new BigInteger(Long.toString(value));
			}

			@Override
			public double doubleValue() {
				return value;
			}

			@Override
			public BigDecimal bigDecimalValue() {
				return new BigDecimal(Long.toString(value));
			}

			@Override
			public String toString() {
				return Long.toString(value);
			}

		});
	}

	@Override
	public void setValue(boolean value) throws DocumentException {
		register.accept(value ? JsonValue.TRUE: JsonValue.FALSE);
	}
}
