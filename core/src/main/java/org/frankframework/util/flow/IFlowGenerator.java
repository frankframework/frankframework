/*
   Copyright 2020,2022 WeAreFrank!

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
package org.frankframework.util.flow;

import java.io.OutputStream;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.MediaType;

/**
 * Used by the FlowDiagramManager to turn a dot file into an image.
 * This class is a prototype bean, the destroy will never be called by Spring.
 */
public interface IFlowGenerator extends InitializingBean, DisposableBean {

	public String getFileExtension();
	public MediaType getMediaType();

	public void generateFlow(String xml, OutputStream outputStream) throws FlowGenerationException;
}
