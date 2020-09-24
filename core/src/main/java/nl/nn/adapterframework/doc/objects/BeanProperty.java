/* 
Copyright 2019, 2020 WeAreFrank! 

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

package nl.nn.adapterframework.doc.objects;

import java.lang.reflect.Method;

import lombok.Getter;
import lombok.Setter;

public class BeanProperty {
	private @Getter @Setter String name;
	private @Getter @Setter Method method;
	private @Getter @Setter boolean isExcluded;
	private @Getter @Setter boolean hasDocumentation;
	private @Getter @Setter String description;
	private @Getter @Setter String defaultValue;
	private @Getter @Setter int order;
}
