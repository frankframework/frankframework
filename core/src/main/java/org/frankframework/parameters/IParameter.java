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

import org.frankframework.core.IConfigurable;
import org.frankframework.core.ParameterException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.doc.FrankDocGroup;
import org.frankframework.doc.FrankDocGroupValue;
import org.frankframework.parameters.AbstractParameter.ParameterMode;
import org.frankframework.stream.Message;

@FrankDocGroup(FrankDocGroupValue.PARAMETER)
public interface IParameter extends IConfigurable {

	ParameterType getType();

	ParameterMode getMode();

	boolean requiresInputValueForResolution();

	boolean consumesSessionVariable(String sessionKey);

	/**
	 * Key of a PipelineSession-variable. <br/>If specified, the value of the PipelineSession variable is used as input for
	 * the xpathExpression or stylesheet, instead of the current input message. <br/>If no xpathExpression or stylesheet are
	 * specified, the value itself is returned. <br/>If the value '*' is specified, all existing sessionkeys are added as
	 * parameter of which the name starts with the name of this parameter. <br/>If also the name of the parameter has the
	 * value '*' then all existing sessionkeys are added as parameter (except tsReceived)
	 */
	void setSessionKey(String sessionKey);

	String getSessionKey();

	/** The value of the parameter, or the base for transformation using xpathExpression or stylesheet, or formatting. */
	void setValue(String value);

	String getValue();

	Object getValue(ParameterValueList alreadyResolvedParameters, Message message, PipeLineSession session, boolean namespaceAware) throws ParameterException;

	boolean isHidden();

}
