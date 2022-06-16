/*
   Copyright 2022 WeAreFrank!

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
package nl.nn.adapterframework.filesystem;

import com.microsoft.aad.msal4j.HttpRequest;
import com.microsoft.aad.msal4j.IHttpClient;
import com.microsoft.aad.msal4j.IHttpResponse;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.http.HttpResponseHandler;
import nl.nn.adapterframework.http.HttpSenderBase;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.StreamUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.entity.ByteArrayEntity;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MsalClientAdapter extends HttpSenderBase implements IHttpClient {
    private final String METHOD_SESSION_KEY = "HTTP_METHOD";
    private final String HEADERS_SESSION_KEY = "HTTP_HEADERS";
    private final String URL_SESSION_KEY = "URL";
    private final String STATUS_CODE_SESSION_KEY = "HTTP_STATUSCODE";

    public MsalClientAdapter() {
        setName("MSAL");
        Parameter urlParameter = new Parameter();
        urlParameter.setName("url");
        urlParameter.setSessionKey(URL_SESSION_KEY);

        addParameter(urlParameter);

        setResultStatusCodeSessionKey(STATUS_CODE_SESSION_KEY);
    }

    @Override
    public IHttpResponse send(HttpRequest httpRequest) throws Exception {
        PipeLineSession session = prepareSession(httpRequest);
        Message request = new Message(httpRequest.body());

        try {
            Message response = sendMessage(request, session);
            IHttpResponse translatedResponse = translateResponse(response, session);

            return translatedResponse;
        } catch (Exception e){
            log.error("An exception occurred whilst connecting with MSAL HTTPS call to ["+httpRequest.url().toString()+"]");
            throw e;
        } finally {
            session.close();
        }
    }

    private PipeLineSession prepareSession(HttpRequest httpRequest){
        PipeLineSession session = new PipeLineSession();
        session.put(URL_SESSION_KEY, httpRequest.url().toString());
        if (log.isDebugEnabled()) log.debug("Put value ["+httpRequest.url().toString()+"] in session under key ["+URL_SESSION_KEY+"]");

        session.put(METHOD_SESSION_KEY, translateHttpMethod(httpRequest.httpMethod()));
        if (log.isDebugEnabled()) log.debug("Put value ["+translateHttpMethod(httpRequest.httpMethod())+"] in session under key ["+METHOD_SESSION_KEY+"]");

        session.put(HEADERS_SESSION_KEY, httpRequest.headers());
        if (log.isDebugEnabled()) log.debug("Put value ["+httpRequest.headers()+"] in session under key ["+HEADERS_SESSION_KEY+"]");

        return session;
    }

    @Override
    protected HttpRequestBase getMethod(URI uri, Message message, ParameterValueList parameters, PipeLineSession session) throws SenderException {
        HttpSenderBase.HttpMethod httpMethod = (HttpMethod) session.get(METHOD_SESSION_KEY);
        Map<String, String> headers = (Map<String, String>) session.get(HEADERS_SESSION_KEY);

        if(uri == null){
            throw new SenderException("Unknown URI to connect to! " + uri);
        }

        boolean queryParametersAppended = false;
        StringBuffer relativePath = new StringBuffer(uri.getRawPath());
        if (!StringUtils.isEmpty(uri.getQuery())) {
            relativePath.append("?"+uri.getQuery());
            queryParametersAppended = true;
        }
        try {
            switch(httpMethod){
                case GET:
                    if (parameters!=null) {
                        queryParametersAppended = appendParameters(queryParametersAppended,relativePath,parameters);
                        if (log.isDebugEnabled()) log.debug(getLogPrefix()+"path after appending of parameters ["+relativePath+"]");
                    }
                    HttpGet getMethod = new HttpGet(relativePath+(parameters==null? message.asString():""));

                    if (log.isDebugEnabled()) log.debug(getLogPrefix()+"HttpSender constructed GET-method ["+getMethod.getURI().getQuery()+"]");
                    if (null != getFullContentType()) { //Manually set Content-Type header
                        getMethod.setHeader("Content-Type", getFullContentType().toString());
                    }

                    return appendHeaders(headers, getMethod);
                case POST:
                    String messageString = message.asString();
                    if (parameters!=null) {
                        StringBuffer msg = new StringBuffer(messageString);
                        appendParameters(true,msg,parameters);
                        if (StringUtils.isEmpty(messageString) && msg.length()>1) {
                            messageString=msg.substring(1);
                        } else {
                            messageString=msg.toString();
                        }
                    }
                    HttpEntity entity = new ByteArrayEntity(messageString.getBytes(StreamUtil.DEFAULT_INPUT_STREAM_ENCODING), getFullContentType());

                    HttpEntityEnclosingRequestBase method = new HttpPost(relativePath.toString());

                    method.setEntity(entity);
                    return appendHeaders(headers, method);
            }
        } catch (Exception e){
            throw new SenderException("An exception occurred whilst getting method.", e);
        }
        return null;
    }

    @Override
    protected Message extractResult(HttpResponseHandler responseHandler, PipeLineSession session) throws SenderException, IOException {
        // Parse headers to comma separated string
        Header[] responseHeaders = responseHandler.getAllHeaders();
        String headers = "";
        for (Header header : responseHeaders) {
            session.put(header.getName(), header.getValue());
            if(headers.length() > 0){
                headers += ",";
            }
            headers += header.getName();
        }
        session.put(HEADERS_SESSION_KEY, headers);

        return new Message(responseHandler.getResponse());
    }

    private HttpRequestBase appendHeaders(Map<String, String> headers, HttpRequestBase request){
        for(Map.Entry<String, String> header : headers.entrySet()){
            String name = header.getKey();
            String value = header.getValue();
            request.setHeader(name, value);
            if (log.isDebugEnabled()) log.debug("Appending header ["+name+"] ["+value+"]");
        }
        return request;
    }

    /*
      com.microsoft.aad.msal4j.HttpMethod only has GET & POST.
      Translates MSAL4J HttpMethod enum to HttpSenderBase HttpMethod enum
   */
    private HttpSenderBase.HttpMethod translateHttpMethod(com.microsoft.aad.msal4j.HttpMethod method){
        if(method.equals(com.microsoft.aad.msal4j.HttpMethod.GET)){
            return HttpSenderBase.HttpMethod.GET;
        } else {
            return HttpSenderBase.HttpMethod.POST;
        }
    }

    private IHttpResponse translateResponse(Message response, PipeLineSession session) {
        return new IHttpResponse() {
            @Override
            public int statusCode() {
                int statusCode = Integer.parseInt( (String) session.get(STATUS_CODE_SESSION_KEY) );
                if (log.isDebugEnabled()) log.debug("Parsing status code ["+statusCode+"]");

                return statusCode;
            }

            @Override
            public Map<String, List<String>> headers() {
                String[] headersAsCsv = ((String) session.get(HEADERS_SESSION_KEY)).split(",");
                Map<String, List<String>> headers = new HashMap<>();

                for (String headerName : headersAsCsv) {
                    List<String> values = new ArrayList<>();
                    String headerValue = (String) session.get(headerName);
                    values.add( headerValue );

                    if (log.isDebugEnabled()) log.debug("Parsing header ["+headerName+"] ["+headerValue+"]");
                    headers.put(headerName, values);
                }

                return headers;
            }

            @Override
            public String body() {
                try {
                    if (log.isDebugEnabled()) log.debug("Parsing body ["+response.asString()+"]");
                    return response.asString();
                } catch (IOException e) {
                    log.error("An exception occurred whilst parsing the response body of MSAL authentication call.", e);
                }
                return "";
            }
        };
    }
}
