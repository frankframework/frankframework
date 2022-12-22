/*
   Copyright 2017 Nationale-Nederlanden

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
package nl.nn.adapterframework.align.content;

import java.util.Stack;

import org.apache.xerces.xs.XSSimpleTypeDefinition;
import org.apache.xerces.xs.XSTypeDefinition;

public abstract class TreeContentContainer<E extends ElementContainer> implements DocumentContainer {

	private Stack<E> elementStack=new Stack<>();
	private E root=createElementContainer(null, false, false, null);
	private E elementContainer=root;

	protected abstract E createElementContainer(String localName, boolean xmlArrayContainer, boolean repeatedElement, XSTypeDefinition typeDefinition);
	protected abstract void addContent(E parent, E child);

	@Override
	public void startElementGroup(String localName, boolean xmlArrayContainer, boolean repeatedElement, XSTypeDefinition typeDefinition) {
	}

	@Override
	public void endElementGroup(String localName) {
	}

	@Override
	public void startElement(String localName, boolean xmlArrayContainer, boolean repeatedElement, XSTypeDefinition typeDefinition) {
		elementStack.push(elementContainer);
		elementContainer=createElementContainer(localName, xmlArrayContainer, repeatedElement, typeDefinition);
	}
	@Override
	public void endElement(String localName) {
		E result=elementContainer;
		elementContainer=elementStack.pop();
		addContent(elementContainer,result);
	}

	@Override
	public void setNull() {
		elementContainer.setNull();
	}

	@Override
	public void setAttribute(String name, String value, XSSimpleTypeDefinition attTypeDefinition) {
		elementContainer.setAttribute(name, value, attTypeDefinition);
	}

	@Override
	public void characters(char[] ch, int start, int length) {
		elementContainer.characters(ch, start, length);
	}
	public E getRoot() {
		return root;
	}
	public E getCurrentElement() {
		return elementContainer;
	}

}
