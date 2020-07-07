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
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
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

import com.fasterxml.jackson.databind.ObjectMapper;

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
			String url = getRawUrl(path); //May contain QueryParameters

			return String.format("%S:%s", method, url);
		}

		private String getRawUrl(String path) { //String QueryParams off path
			String url = path;
			if(!url.startsWith("/")) {
				url = "/"+url;
			}
			int questionMark = url.indexOf("?");
			if(questionMark != -1) {
				url = url.substring(0, questionMark);
			}
			return url;
		}

		private Method findRequest(String rsResourceKey) {
			String uriSegments[] = rsResourceKey.split("/");

			for (Iterator<String> it = rsRequests.keySet().iterator(); it.hasNext();) {
				String pattern = it.next();
				String patternSegments[] = pattern.split("/");

				if (patternSegments.length != uriSegments.length || patternSegments.length < uriSegments.length) {
					continue;
				}

				int matches = 0;
				for (int i = 0; i < uriSegments.length; i++) {
					if(patternSegments[i].equals(uriSegments[i]) || (patternSegments[i].startsWith("{") && patternSegments[i].endsWith("}"))) {
						matches++;
					} else {
						continue;
					}
				}
				if(matches == uriSegments.length) {
					return rsRequests.get(pattern);
				}
			}
			return null;
		}

		public Response dispatchRequest(String httpMethod, String url) {
			return dispatchRequest(httpMethod, url, null);
		}

		public Response dispatchRequest(String httpMethod, String url, Object jsonOrFormdata) {
			String rsResourceKey = compileKey(httpMethod, url);
			System.out.println("trying to dispatch request to ["+rsResourceKey+"]");

			if(httpMethod.equalsIgnoreCase("GET") && jsonOrFormdata != null) {
				fail("can't use arguments on a GET request");
			}

			Method method = findRequest(rsResourceKey);
			if(method == null) {
				fail("can't find resource ["+url+"] method ["+httpMethod+"]");
			}
			String methodPath = method.getAnnotation(Path.class).value();
			System.out.println("found JAX-RS resource ["+compileKey(httpMethod, methodPath)+"]");

			try {
				Parameter[] parameters = method.getParameters();
				Object[] methodArguments = new Object[parameters.length]; //TODO: figure out how to deal with QueryParams and MultipartRequests

				for (int i = 0; i < parameters.length; i++) {
					Parameter parameter = parameters[i];

					boolean isPathParameter = parameter.isAnnotationPresent(PathParam.class);
					boolean isQueryParameter = parameter.isAnnotationPresent(QueryParam.class);

					if(isPathParameter) {
						String pathValue = findPathParameter(parameter, methodPath, url);

						System.out.println("adding method argument ["+i+"] with value ["+pathValue+"]");
						methodArguments[i] = pathValue;
					} else if(isQueryParameter) {
						Object value = findQueryParameter(parameter, url);

						System.out.println("adding method argument ["+i+"] value ["+value+"] type ["+value.getClass().getSimpleName()+"]");
						methodArguments[i] = value;
					} else {
						Consumes consumes = AnnotationUtils.findAnnotation(method, Consumes.class);
						if(consumes == null) {
							fail("found additional argument without consumes!");
						}
						if(jsonOrFormdata == null) {
							fail("found unset parameter ["+parameter+"] on method!");
						}

						String mediaType = consumes.value()[0];
						if(mediaType.equals(MediaType.APPLICATION_JSON)) { //We need to convert our input to json
							Object value = convertInputToParameterType(parameter, jsonOrFormdata);

							System.out.println("adding method argument ["+i+"] value ["+value+"] type ["+value.getClass().getSimpleName()+"]");
							methodArguments[i] = value;
						}
					}
				}

				validateIfAllArgumentsArePresent(methodArguments, parameters);

				ResponseImpl response = (ResponseImpl) method.invoke(jaxRsResource, methodArguments);
				MultivaluedMap<String, Object> meta = new MetadataMap<>();

				Produces produces = AnnotationUtils.findAnnotation(method, Produces.class);
				if(produces != null) {
					String mediaType = produces.value()[0];
					MediaType type = MediaType.valueOf(mediaType);
					meta.add(HttpHeaders.CONTENT_TYPE, type);

					if(mediaType.equals(MediaType.APPLICATION_JSON)) {
						ObjectMapper objectMapper = new ObjectMapper();
						String json = objectMapper.writeValueAsString(response.getEntity());
						response.setEntity(json, null);
					}
				}

				response.addMetadata(meta); //Headers
				return response;
			} catch (Exception e) {
				e.printStackTrace();
				fail("error dispatching request ["+rsResourceKey+"]");
				return null;
			}
		}

		// Convert the json input to the parameter class type
		private Object convertInputToParameterType(Parameter parameter, Object jsonOrFormdata) {
			ObjectMapper objectMapper = new ObjectMapper();
			try {
				return objectMapper.readValue((String) jsonOrFormdata, parameter.getType());
			} catch (Throwable e) {
				fail("error transforming json to type ["+parameter.getType()+"]");
				return null;
			}
		}

		private String findPathParameter(Parameter parameter, String methodPath, String url) {
			PathParam pathParameter = parameter.getAnnotation(PathParam.class);
			String path = String.format("{%s}", pathParameter.value());
			System.out.println("looking up path ["+path+"]");

			String pathSegments[] = methodPath.split("/");
			String urlSegments[] = getRawUrl(url).split("/");
			String pathValue = null;
			for (int j = 0; j < pathSegments.length; j++) {
				String segment = pathSegments[j];
				if(segment.equals(path)) {
					pathValue = urlSegments[j];
					break;
				}
			}
			if(pathValue == null) {
				fail("unable to populate path param ["+path+"]");
			}
			return pathValue;
		}

		private Object findQueryParameter(Parameter parameter, String url) {
			QueryParam queryParameter = parameter.getAnnotation(QueryParam.class);
			int questionMark = url.indexOf("?");
			if(questionMark == -1) {
				fail("found query parameter ["+parameter+"] but not in URL ["+url+"]");
			}
	
			String urlQueryParameters = url.substring(questionMark +1);
			String urlQuerySegments[] = urlQueryParameters.split("&");
			String queryValue = null;
			for (String querySegment : urlQuerySegments) {
				if(querySegment.startsWith(queryParameter.value()+"=")) {
					queryValue = querySegment.substring(querySegment.indexOf("=")+1);
					break;
				}
			}
			if(queryValue == null) {
				fail("unable to populate query param ["+queryParameter.value()+"]");
			}
	
			Object value = null;
			switch (parameter.getType().toGenericString()) {
				case "boolean":
					value = Boolean.parseBoolean(queryValue);
					break;
		
				default:
					fail("parameter type ["+parameter.getType()+"] not implemented");
			}
			return value;
		}

		private void validateIfAllArgumentsArePresent(Object[] methodArguments, Parameter[] parameters) {
			for (int j = 0; j < methodArguments.length; j++) {
				Object object = methodArguments[j];
				if(object == null) {
					fail("missing argument ["+j+"] for method, expecting type ["+parameters[j]+"]");
				}
			}
		}
	}
}
