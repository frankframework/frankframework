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
import java.util.List;
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
import javax.ws.rs.core.SecurityContext;

import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.impl.ResponseImpl;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.mockito.Mockito;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;

import com.fasterxml.jackson.databind.ObjectMapper;

import nl.nn.adapterframework.configuration.ApplicationWarnings;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeLineExit;
import nl.nn.adapterframework.pipes.EchoPipe;
import nl.nn.adapterframework.testutil.TestConfiguration;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.MessageKeeper;

public abstract class ApiTestBase<M extends Base> extends Mockito {
	private Logger log = LogUtil.getLogger(ApiTestBase.class);
	public enum IbisRole {
		IbisWebService, IbisObserver, IbisDataAdmin, IbisAdmin, IbisTester;
	}

	protected MockDispatcher dispatcher = new MockDispatcher();
	private M jaxRsResource;
	private SecurityContext securityContext = mock(SecurityContext.class);
	private TestConfiguration configuration = null;

	abstract public M createJaxRsResource();

	@Before
	public void setUp() throws Exception {
		ApplicationWarnings.removeInstance(); //Remove old instance if present
		M resource = createJaxRsResource();
		checkContextFields(resource);
		jaxRsResource = spy(resource);

		MockServletContext servletContext = new MockServletContext();
		MockServletConfig servletConfig = new MockServletConfig(servletContext, "JAX-RS-MockDispatcher");
		jaxRsResource.servletConfig = servletConfig;
		jaxRsResource.securityContext = mock(SecurityContext.class);
		IbisContext ibisContext = mock(IbisContext.class);
		configuration = new TestConfiguration();
		IbisManager ibisManager = configuration.getIbisManager();
		ibisManager.setIbisContext(ibisContext);
		MessageKeeper messageKeeper = new MessageKeeper();
		doReturn(messageKeeper).when(ibisContext).getMessageKeeper();
		doReturn(ibisManager).when(ibisContext).getIbisManager();
		doReturn(ibisContext).when(jaxRsResource).getIbisContext();
		doReturn(configuration.getBean("applicationWarnings")).when(ibisContext).getBean(eq("applicationWarnings"), any());
		registerAdapter(configuration);

		dispatcher.register(jaxRsResource);
	}

	@After
	public final void tearDown() {
		if(configuration != null) {
			configuration.close();
		}
	}

	public TestConfiguration getConfiguration() {
		return configuration;
	}

	protected void registerAdapter(TestConfiguration configuration) throws Exception{
		Adapter adapter = configuration.createBean(Adapter.class);
		adapter.setName("dummyAdapter");
		try {
			PipeLine pipeline = new PipeLine();
			PipeLineExit exit = new PipeLineExit();
			exit.setPath("EXIT");
			exit.setState("success");
			pipeline.registerPipeLineExit(exit);
			EchoPipe pipe = new EchoPipe();
			pipe.setName("myPipe");
			pipeline.addPipe(pipe);
			adapter.setPipeLine(pipeline);
			configuration.registerAdapter(adapter);
		} catch (ConfigurationException e) {
			e.printStackTrace();
			fail("error registering adapter ["+adapter+"] " + e.getMessage());
		}
		configuration.getConfigurationWarnings().add((Object) null, log, "hello I am a configuration warning!");
	}

	//This has to happen before it's proxied by Mockito (spy method)
	public void checkContextFields(M resource) {
		for(Field field : resource.getClass().getDeclaredFields()) {
			Context context = AnnotationUtils.findAnnotation(field, Context.class); //Injected JAX-WS Resources

			if(context != null) {
				field.setAccessible(true);
				if(field.getType().isAssignableFrom(Request.class)) {
					Request request = new MockHttpRequest();
					try {
						field.set(resource, request);
					} catch (IllegalArgumentException | IllegalAccessException e) {
						e.printStackTrace();
						fail("unable to inject Request");
					}
				} else if(field.getType().isAssignableFrom(SecurityContext.class)) {
					try {
						field.set(resource, securityContext);
					} catch (IllegalArgumentException | IllegalAccessException e) {
						e.printStackTrace();
						fail("unable to inject Request");
					}
				}
			}
		}
	}

	public class MockDispatcher {
		public Map<String, Method> rsRequests = new HashMap<String, Method>();

		/**
		 * scan all methods in the JAX-RS class
		 */
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

		/**
		 * uppercase the HTTP method and combine with the resource path
		 */
		public String compileKey(String method, String path) {
			String url = getPath(path);
			return String.format("%S:%s", method, url);
		}

		/**
		 * Makes sure the url begins with a slash and strips all QueryParams off the URL
		 */
		private String getPath(String path) {
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

		/**
		 * Tries to find the correct JAX-RS method (with optional path parameter)
		 * @param rsResourceKey unique key to identify all JAX-RS resources in the given resource class 
		 * @return JAX-RS method or `null` when no resource is found
		 */
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
			return dispatchRequest(httpMethod, url, jsonOrFormdata, null);
		}

		/**
		 * Dispatches the mocked request.
		 * @param httpMethod GET PUT POST DELETE
		 * @param url the relative path of the FF! API
		 * @param jsonOrFormdata when using PUT/POST requests, a json string of formdata object
		 * @param role IbisRole if you want to test authorization as well
		 */
		public Response dispatchRequest(String httpMethod, String url, Object jsonOrFormdata, IbisRole role) {

			String rsResourceKey = compileKey(httpMethod, url);
			System.out.println("trying to dispatch request to ["+rsResourceKey+"]");

			if(httpMethod.equalsIgnoreCase("GET") && jsonOrFormdata != null) {
				fail("can't use arguments on a GET request");
			}
			applyIbisRole(role);

			Method method = findRequest(rsResourceKey);
			if(method == null) {
				fail("can't find resource ["+url+"] method ["+httpMethod+"]");
			}
			String methodPath = method.getAnnotation(Path.class).value();
			System.out.println("found JAX-RS resource ["+compileKey(httpMethod, methodPath)+"]");

			try {
				Parameter[] parameters = method.getParameters(); //find all parameters in the JAX-RS method, and try to populate them.
				Object[] methodArguments = new Object[parameters.length];

				for (int i = 0; i < parameters.length; i++) {
					Parameter parameter = parameters[i];

					boolean isPathParameter = parameter.isAnnotationPresent(PathParam.class);
					boolean isQueryParameter = parameter.isAnnotationPresent(QueryParam.class);

					if(isPathParameter) {
						String pathValue = findPathParameter(parameter, methodPath, url);

						System.out.println("setting method argument ["+i+"] to value ["+pathValue+"]");
						methodArguments[i] = pathValue;
					} else if(isQueryParameter) {
						Object value = findQueryParameter(parameter, url);

						System.out.println("setting method argument ["+i+"] to value ["+value+"] with type ["+value.getClass().getSimpleName()+"]");
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

							System.out.println("setting method argument ["+i+"] to value ["+value+"] with type ["+value.getClass().getSimpleName()+"]");
							methodArguments[i] = value;
						} else if(mediaType.equals(MediaType.MULTIPART_FORM_DATA)){
							if(jsonOrFormdata instanceof List<?>) {
								@SuppressWarnings("unchecked")
								MultipartBody multipartBody = new MultipartBody((List<Attachment>) jsonOrFormdata);
								methodArguments[i] = multipartBody;
							}
						} else {
							fail("mediaType ["+mediaType+"] not yet implemented"); //TODO: figure out how to deal with MultipartRequests
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
				fail("error dispatching request ["+rsResourceKey+"] " + e.getMessage());
				return null;
			}
		}

		/**
		 * If set, apply the IbisRole to the mocked request.
		 */
		protected void applyIbisRole(IbisRole role) {
			if(role != null) {
				doReturn(true).when(securityContext).isUserInRole(role.name());
			}
		}

		/**
		 * Convert the json input to the parameters class type
		 */
		private Object convertInputToParameterType(Parameter parameter, Object jsonOrFormdata) {
			ObjectMapper objectMapper = new ObjectMapper();
			try {
				return objectMapper.readValue((String) jsonOrFormdata, parameter.getType());
			} catch (Throwable e) {
				fail("error transforming json to type ["+parameter.getType()+"]");
				return null;
			}
		}

		/**
		 * If a path parameter is used, try to find it
		 * @return the resolved path parameter value
		 */
		private String findPathParameter(Parameter parameter, String methodPath, String url) {
			PathParam pathParameter = parameter.getAnnotation(PathParam.class);
			String path = String.format("{%s}", pathParameter.value());
			System.out.println("looking up path ["+path+"]");

			String pathSegments[] = methodPath.split("/");
			String urlSegments[] = getPath(url).split("/");
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

		/**
		 * If a query parameter is used, try to find it
		 * @return the resolved query parameter value
		 */
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

		/**
		 * Validate that all arguments are correctly set on the (JAX-RS resource) method
		 */
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
