/*
   Copyright 2020-2022 WeAreFrank!

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
package nl.nn.adapterframework.management.web;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
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
import javax.ws.rs.core.UriInfo;

import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.ContentDisposition;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.apache.cxf.jaxrs.impl.ResponseImpl;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;

import com.fasterxml.jackson.databind.ObjectMapper;

import nl.nn.adapterframework.util.LogUtil;

public abstract class FrankApiTestBase<M extends FrankApiBase> extends Mockito {
	public static final String STUBBED_SPRING_BUS_CONFIGURATION = "stubbedBusApplicationContext.xml";

	private Logger log = LogUtil.getLogger(FrankApiTestBase.class);
	public enum IbisRole {
		IbisWebService, IbisObserver, IbisDataAdmin, IbisAdmin, IbisTester;
	}

	protected MockDispatcher dispatcher = new MockDispatcher();
	protected M jaxRsResource;
	private SecurityContext securityContext = mock(SecurityContext.class);
	private static ApplicationContext applicationContext;

	public abstract M createJaxRsResource();

	@BeforeEach
	public void setUp() throws Exception {
		M resource = createJaxRsResource();
		checkContextFields(resource);
		jaxRsResource = spy(resource);

		MockServletContext servletContext = new MockServletContext();
		MockServletConfig servletConfig = new MockServletConfig(servletContext, "JAX-RS-MockDispatcher");
		jaxRsResource.servletConfig = servletConfig;
		jaxRsResource.setApplicationContext(getApplicationContext());
		jaxRsResource.securityContext = mock(SecurityContext.class);
		jaxRsResource.servletRequest = new MockHttpServletRequest();
		jaxRsResource.uriInfo = mock(UriInfo.class);

		dispatcher.register(jaxRsResource);
	}

	private final ApplicationContext getApplicationContext() {
		if(FrankApiTestBase.applicationContext == null) {
			ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext();
			applicationContext.setConfigLocation(STUBBED_SPRING_BUS_CONFIGURATION);
			applicationContext.setDisplayName("STUB [stubbed-frank-management-bus]");

			applicationContext.refresh();

			FrankApiTestBase.applicationContext = applicationContext;
		}

		return FrankApiTestBase.applicationContext;
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
			Path basePathAnnotation = AnnotationUtils.findAnnotation(jaxRsResource.getClass(), Path.class);
			final String basePath = (basePathAnnotation != null) ? basePathAnnotation.value() : "";
			for(Method classMethod : classMethods) {
				int modifiers = classMethod.getModifiers();
				if (Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers)) {
					Path path = AnnotationUtils.findAnnotation(classMethod, Path.class);
					HttpMethod httpMethod = AnnotationUtils.findAnnotation(classMethod, HttpMethod.class);
					if(path != null && httpMethod != null) {
						String pathValue = basePath + path.value();
						String rsResourceKey = compileKey(httpMethod.value(), pathValue);

						log.info("adding new JAX-RS resource key [{}] method [{}] path [{}]", ()->rsResourceKey, classMethod::getName, ()->pathValue);
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
			String[] uriSegments = rsResourceKey.split("/");

			for (Iterator<String> it = rsRequests.keySet().iterator(); it.hasNext();) {
				String pattern = it.next();
				String[] patternSegments = pattern.split("/");

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
			log.info("trying to dispatch request to [{}]", rsResourceKey);

			if(httpMethod.equalsIgnoreCase("GET") && jsonOrFormdata != null) {
				fail("can't use arguments on a GET request");
			}
			applyIbisRole(role);

			Method method = findRequest(rsResourceKey);
			if(method == null) {
				fail("can't find resource ["+url+"] method ["+httpMethod+"]");
			}

			Path basePathAnnotation = AnnotationUtils.findAnnotation(method.getDeclaringClass(), Path.class);
			final String basePath = (basePathAnnotation != null) ? basePathAnnotation.value() : "";
			final String methodPath = basePath + method.getAnnotation(Path.class).value();

			log.debug("found JAX-RS resource [{}]", ()->compileKey(httpMethod, methodPath));

			try {
				Parameter[] parameters = method.getParameters(); //find all parameters in the JAX-RS method, and try to populate them.
				Object[] methodArguments = new Object[parameters.length];

				for (int i = 0; i < parameters.length; i++) {
					Parameter parameter = parameters[i];

					boolean isPathParameter = parameter.isAnnotationPresent(PathParam.class);
					boolean isQueryParameter = parameter.isAnnotationPresent(QueryParam.class);

					if(isPathParameter) {
						String pathValue = findPathParameter(parameter, methodPath, url);

						log.debug("setting method argument [{}] to value [{}]", i, pathValue);
						methodArguments[i] = pathValue;
					} else if(isQueryParameter) {
						Object value = findQueryParameter(parameter, url);

						log.debug("setting method argument [{}] to value [{}] with type [{}]", i, value, value.getClass().getSimpleName());
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

							log.debug("setting method argument [{}] to value [{}] with type [{}]", i, value, value.getClass().getSimpleName());
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
				MultivaluedMap<String, Object> meta = response.getMetadata();

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
				if(e instanceof InvocationTargetException && e.getCause() instanceof ApiException) {
					throw (ApiException) e.getCause();
				}
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
			log.debug("looking up path [{}]", path);

			String[] pathSegments = methodPath.split("/");
			String[] urlSegments = getPath(url).split("/");
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
				log.info("found query parameter [{}] on method but it was not present in the URL [{}]", queryParameter.value(), url);
			}

			String urlQueryParameters = url.substring(questionMark +1);
			String[] urlQuerySegments = urlQueryParameters.split("&");
			String queryValue = null;
			for (String querySegment : urlQuerySegments) {
				if(querySegment.startsWith(queryParameter.value()+"=")) {
					queryValue = querySegment.substring(querySegment.indexOf("=")+1);
					break;
				}
			}
			if(queryValue == null) {
				DefaultValue defaultValue = parameter.getAnnotation(DefaultValue.class);
				if(defaultValue != null) {
					queryValue = defaultValue.value();
				}
			}
			if(queryValue == null) {
				fail("unable to populate query param ["+queryParameter.value()+"]");
			}

			Object value = null;
			switch (parameter.getType().getTypeName()) {
				case "boolean":
				case "java.lang.Boolean":
					value = Boolean.parseBoolean(queryValue);
					break;
				case "int":
				case "java.lang.Integer":
					value = Integer.parseInt(queryValue);
					break;
				case "java.lang.String":
					value = queryValue;
					break;

				default:
					fail("parameter type ["+parameter.getType().getTypeName()+"] not implemented");
			}
			log.info("resolved value [{}] to type [{}]", queryValue, value.getClass());
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


	protected class FileAttachment extends Attachment {
		private InputStream stream;

		public FileAttachment(String id, InputStream stream, String filename) {
			super(id, (InputStream) null, new ContentDisposition("attachment;filename="+filename));
			this.stream = stream;
		}

		@Override
		@SuppressWarnings("unchecked")
		public <T> T getObject(Class<T> cls) {
			return (T) getObject();
		}

		@Override
		public Object getObject() {
			return stream;
		}
	}

	protected class StringAttachment extends Attachment {

		public StringAttachment(String name, String value) {
			super(name, "text/plain", new ByteArrayInputStream(value.getBytes()));
		}

		@Override
		@SuppressWarnings("unchecked")
		public <T> T getObject(Class<T> cls) {
			return (T) getObject();
		}
	}
}
