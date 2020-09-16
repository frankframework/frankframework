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
package nl.nn.adapterframework.jmx;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;

import javax.management.MBeanOperationInfo;
import javax.management.modelmbean.ModelMBeanOperationInfo;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.jmx.export.assembler.AbstractConfigurableMBeanInfoAssembler;

public class JmxMBeanInfoAssembler extends AbstractConfigurableMBeanInfoAssembler {

	@Override
	protected boolean includeReadAttribute(Method method, String beanKey) {
		return AnnotationUtils.findAnnotation(method, JmxAttribute.class) != null; //AnnotationUtils caches results. Use find so other methods can use the cached results!
	}

	@Override
	protected boolean includeWriteAttribute(Method method, String beanKey) {
		return AnnotationUtils.findAnnotation(method, JmxAttribute.class) != null; //AnnotationUtils caches results. Use find so other methods can use the cached results!
	}

	@Override
	protected boolean includeOperation(Method method, String beanKey) {
		return AnnotationUtils.findAnnotation(method, JmxOperation.class) != null; //AnnotationUtils caches results. Use find so other methods can use the cached results!
	}

	@Override
	protected String getAttributeDescription(PropertyDescriptor propertyDescriptor, String beanKey) {
		Method method = propertyDescriptor.getReadMethod(); //In JMX, attribute setters don't have a description?
		if(method != null) {
			JmxAttribute attr = AnnotationUtils.findAnnotation(method, JmxAttribute.class);
			if(attr != null) {
				return attr.description();
			}
		}

		return super.getAttributeDescription(propertyDescriptor, beanKey);
	}

	@Override
	protected ModelMBeanOperationInfo createModelMBeanOperationInfo(Method method, String name, String beanKey) {
		JmxOperation operation = AnnotationUtils.findAnnotation(method, JmxOperation.class);
		if(operation != null) {
			int operationType;
			if(void.class.equals(method.getReturnType())) {
				operationType = MBeanOperationInfo.ACTION;
			} else {
				operationType = MBeanOperationInfo.ACTION_INFO;
			}

			return new ModelMBeanOperationInfo(method.getName(),
					operation.description(),
					getOperationParameters(method, beanKey),
					method.getReturnType().getName(),
					operationType);
		} else {
			return super.createModelMBeanOperationInfo(method, name, beanKey);
		}
	}
}
