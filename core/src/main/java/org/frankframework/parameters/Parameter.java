/*
   Copyright 2024 WeAreFrank!

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
package org.frankframework.parameters;

import lombok.Getter;

import org.frankframework.doc.Default;

/**
 * Placeholder class to allow legacy configuration notations <code>&lt;param type='number' /&gt;</code> in the new Frank!Config XSD.
 * <p>
 * The attribute <code>type</code> has been removed in favor of explicit ParameterTypes such as: <code>NumberParameter</code>, <code>DateParameter</code> and <code>BooleanParameter</code>.
 * Using the new elements enables the use of auto-completion for the specified type.
 *
 * @author Niels Meijer
 */
// Should never be instantiated directly. See {@link ParameterFactory} and {@link ParameterType} for more information.
// To be deprecated(since = "8.2.0")
public class Parameter extends AbstractParameter {

	private @Getter ParameterType type;

	public Parameter() {
		//throw new NotImplementedException(); // will be replaced by appropriate executor class in ParameterFactory, based on function
	}

	/** utility constructor, useful for unit testing */
	public Parameter(String name, String value) {
		this();
		setName(name);
		setValue(value);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(super.toString());
		if(type != null) builder.append(" type [").append(type).append("]");
		return builder.toString();
	}

	@Override
	@Default("STRING")
	/** The target data type of the parameter, related to the database or XSLT stylesheet to which the parameter is applied. */
	public void setType(ParameterType type) {
		this.type = type;
		super.setType(type);
	}
}
