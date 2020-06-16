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
package nl.nn.adapterframework.webcontrol.api;

import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.impl.ResponseImpl;
import org.junit.Before;
import org.mockito.Mockito;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;

import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.util.MessageKeeper;

public abstract class ApiTestBase<M extends Base> extends Mockito {

	protected MockDispatcher dispatcher = new MockDispatcher();
	private M jaxRsResource;

	abstract public M createJaxRsResource();

	@Before
	public void setUp() {
		M resource = createJaxRsResource();
		checkContextFields(resource);
		jaxRsResource = spy(resource);

		ConfigurationWarnings globalConfigWarnings = ConfigurationWarnings.getInstance();
		globalConfigWarnings.clear();

		MockServletContext servletContext = new MockServletContext();
		MockServletConfig servletConfig = new MockServletConfig(servletContext, "JAX-RS-MockDispatcher");
		jaxRsResource.servletConfig = servletConfig;
		IbisContext ibisContext = mock(IbisContext.class);
		IbisManager ibisManager = new MockIbisManager();
		ibisManager.setIbisContext(ibisContext);
		MessageKeeper messageKeeper = new MessageKeeper();
		doReturn(messageKeeper).when(ibisContext).getMessageKeeper();
		doReturn(ibisManager).when(ibisContext).getIbisManager();
		doReturn(ibisContext).when(jaxRsResource).getIbisContext();

		dispatcher.register(jaxRsResource);
	}

	//This has to happen before it's proxied by Mockito (spy method)
	public void checkContextFields(M resource) {
		for(Field field : resource.getClass().getDeclaredFields()) {
			Context context = AnnotationUtils.findAnnotation(field, Context.class); //Injected JAX-WS Resources

			if(context != null && field.getType().isAssignableFrom(Request.class)) {
				Request request = new MockHttpRequest();
				try {
					field.set(resource, request);
				} catch (IllegalArgumentException | IllegalAccessException e) {
					e.printStackTrace();
					fail("unable to inject Request");
				}
			}
		}
	}

	public class MockDispatcher {
		public Map<String, Method> rsRequests = new HashMap<String, Method>();

		public void register(M jaxRsResource) {
			Method[] classMethods = jaxRsResource.getClass().getDeclaredMethods();
			for(Method classMethod : classMethods) {
				int modifiers = classMethod.getModifiers();
				if (Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers)) {
					Path path = AnnotationUtils.findAnnotation(classMethod, Path.class);
					HttpMethod httpMethod = AnnotationUtils.findAnnotation(classMethod, HttpMethod.class);
					if(path != null && httpMethod != null) {
						String rsResourceKey = compileKey(httpMethod.value(), path.value());

						System.out.println("adding new JAX-RS resource key ["+rsResourceKey+"] method ["+classMethod.getName()+"]");
						rsRequests.put(rsResourceKey, classMethod);
					}
					//Ignore security for now
				}
			}
		}

		//Uppercase method, make sure url begins with a /
		public String compileKey(String method, String path) {
			String url = path; //May contain QueryParameters
			if(!url.startsWith("/")) {
				url = "/"+url;
			}
			int questionMark = url.indexOf("?");
			if(questionMark != -1) {
				url = url.substring(0, questionMark);
			}

			return String.format("%S:%s", method, url);
		}

		public Response dispatchRequest(String httpMethod, String url) {
			String rsResourceKey = compileKey(httpMethod, url);
			System.out.println("trying to dispatch request to ["+rsResourceKey+"]");

			Method method = rsRequests.get(rsResourceKey);
//			Class<?>[] parameters = method.getParameterTypes();
			try {
				Object[] object = new Object[0]; //TODO: figure out how to deal with QueryParams and MultipartRequests
				ResponseImpl response = (ResponseImpl) method.invoke(jaxRsResource, object);
				MultivaluedMap<String, Object> meta = new MetadataMap<>();

				Produces produces = AnnotationUtils.findAnnotation(method, Produces.class);
				if(produces != null) {
					String mediaType = produces.value()[0];
					MediaType type = MediaType.valueOf(mediaType);
					meta.add(HttpHeaders.CONTENT_TYPE, type);
				}

				response.addMetadata(meta);
				return response;
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				e.printStackTrace();
				fail("error dispatching request ["+rsResourceKey+"]");
				return null;
			}
		}
	}
}
