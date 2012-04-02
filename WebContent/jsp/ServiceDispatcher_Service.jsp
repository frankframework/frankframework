<?xml version="1.0" encoding="UTF-8"?>
<%@page contentType="text/xml"%>
<%--

This JSP provides a dynamic WSDL. The name of the service and location are dynamically built, so that 
it is not sensitive to where it is deployed. This is automatically done in AXIS, so becomes 
deprecated when axis is used.

--%>
<%-- determine full service name like http://www.servicedispatcherservice.com/ServiceDispatcher--%>
<% String soapServletName= "/servlet/rpcrouter";
   String fullServerContext= "http://" + request.getServerName() + ":" + request.getServerPort() +request.getContextPath();
%>

<definitions 
	targetNamespace="urn:AdapterFramework/ServiceDispatcher" 
	xmlns="http://schemas.xmlsoap.org/wsdl/" 
	xmlns:soap="http://schemas.xmlsoap.org/wsdl/soap/" 
	xmlns:tns="urn:AdapterFramework/ServiceDispatcher" 
	xmlns:xsd="http://www.w3.org/1999/XMLSchema/">
	
	<message name="IndispatchRequestRequest">
		<part name="ServiceListenerName" type="xsd:string"/>
		<part name="RequestMessage" type="xsd:string"/>
	</message>
	<message name="OutdispatchRequestResponse">
		<part name="ResponseMessage" type="xsd:string"/>
	</message>
	
	<portType name="ServiceDispatcher_Service">
		<operation name="dispatchRequest">
			<input message="tns:IndispatchRequestRequest"/>
			<output message="tns:OutdispatchRequestResponse"/>
		</operation>
	</portType>

	<binding name="ServiceDispatcher_ServiceBinding" type="tns:ServiceDispatcher_Service">
		<soap:binding style="rpc" transport="http://schemas.xmlsoap.org/soap/http"/>
		<operation name="dispatchRequest">
			<soap:operation soapAction="urn:service-dispatcher"/>
			<input>
				<soap:body encodingStyle="http://schemas.xmlsoap.org/soap/encoding/" 
					                namespace="urn:service-dispatcher" use="encoded"/>
			</input>
			<output>
				<soap:body encodingStyle="http://schemas.xmlsoap.org/soap/encoding/" 
							  namespace="urn:service-dispatcher" use="encoded"/>
			</output>
		</operation>
	</binding>
	
	<service name="urn:service-dispatcher">
		<documentation>Service definition for webservice interface to IOS Adapterframework</documentation>
		<port binding="tns:ServiceDispatcher_ServiceBinding" name="ServiceDispatcher_ServicePort">
			<soap:address location="<%=fullServerContext%><%=soapServletName%>"/>
		</port>
	</service>
</definitions>

