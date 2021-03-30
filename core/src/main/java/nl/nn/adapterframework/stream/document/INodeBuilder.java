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

public interface INodeBuilder extends AutoCloseable {

	public ArrayBuilder startArray(String elementName) throws DocumentException;
	public ObjectBuilder startObject() throws DocumentException;
	public void setValue(String value) throws DocumentException;
	public void setValue(long value) throws DocumentException;
	public void setValue(boolean value) throws DocumentException;
	
	@Override
	public void close() throws DocumentException;

}
