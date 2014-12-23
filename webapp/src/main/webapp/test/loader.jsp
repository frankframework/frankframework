<%@ page
	language="java"
	contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"
	import = "java.io.IOException"
	import = "java.io.File"
	import = "java.io.FileReader"
	import = "java.io.LineNumberReader"
	import = "java.util.HashMap"
	import = "nl.nn.adapterframework.jms.JmsSender"
	import = "nl.nn.adapterframework.jms.JmsListener"
	import = "nl.nn.adapterframework.core.ListenerException"
	import = "nl.nn.adapterframework.core.SenderException"
	import = "nl.nn.adapterframework.receivers.JavaProxy"
	import = "nl.nn.adapterframework.util.AppConstants"
	import = "nl.nn.adapterframework.util.XmlUtils"
	import = "javax.jms.*"
	import = "javax.naming.*"
%><%--

    Author Jaco de Groot

--%><!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>

<head>
  <title>Ibis4Loader Test</title>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
</head>
<body>

<a href="loader.jsp?test=juiceLoader">juice loader</a>
|
<a href="loader.jsp?test=pdgLoader">pdg loader</a>
|
<a href="loader.jsp?test=juiceErrors">juice errors (write test message to reply queue)</a>

<br/>

<%
	String test = request.getParameter("test");
	if ("juiceLoader".equals(test) || "pdgLoader".equals(test)) {
		String queue;
		String filename;
		if ("juiceLoader".equals(test)) {
			queue = "jms/i4ld_migratepolicy_req";
			filename = "G:/AMSpecials/Afdeling/Solutions/1_OO_straat/3_Projecten/NN IOS Adapter Framework/Projecten/Ibis4Loader/testbestanden/UICS_JUICE_c3_20050927192743.xml";
		} else {
			queue = "jms/i4ld_policypremdata_msg";
			filename = "G:/AMSpecials/Afdeling/Solutions/1_OO_straat/3_Projecten/NN IOS Adapter Framework/Projecten/Ibis4Loader/testbestanden/UICS_PDG_c3_20050825200303.xml";
		}
		// Lezen met via jndi en jms:
		InitialContext initialContext = new InitialContext();
		javax.jms.QueueConnectionFactory connectionFactory = (javax.jms.QueueConnectionFactory)initialContext.lookup("jms/qcf");
		Destination destination = (Destination)initialContext.lookup(queue);
		javax.jms.QueueConnection queueConnection = ((QueueConnectionFactory)connectionFactory).createQueueConnection();
		javax.jms.QueueSession queueSession = queueConnection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
		LineNumberReader reader = new LineNumberReader(new FileReader(filename));
		QueueReceiver queueReceiver = queueSession.createReceiver((Queue)destination);
		queueConnection.start();
		javax.jms.Message message = queueReceiver.receive(3000);
		int success = 0;
		while (message != null) {
			if (test.equals("juiceLoader")) {
				String migrationRef = message.getStringProperty("MigrationRef");
				String migrationDate = message.getStringProperty("MigrationDate");
				String jmsReplyTo = message.getJMSReplyTo().toString();
				out.println("migrationRef = " + migrationRef + ", migrationDate = " + migrationDate + ", jmsReplyTo = " + jmsReplyTo + " ");
				if (migrationRef == null || migrationDate == null || jmsReplyTo == null) {
					out.println("ERROR</br>");
				}
			}
			TextMessage textMessage = (TextMessage)message;
			String jmsMessage = textMessage.getText();
			String fileMessage = getNextMessage(reader);
			if (jmsMessage != null && jmsMessage.length() > 0 && fileMessage != null && fileMessage.length() > 0 && jmsMessage.equals(fileMessage)) {
				success++;
				out.println("OK</br>");
			} else {
				out.println("<textarea>" + jmsMessage + "</textarea>");
				out.println("<textarea>" + fileMessage + "</textarea>");
				out.println("ERROR</br>");
			}
			message = queueReceiver.receive(3000);
		}
		reader.close();
		queueReceiver.close();
		queueSession.close();
		queueConnection.close();
		out.println("Success: " + success + "<br/>");
	} else if ("juiceErrors".equals(test)) {
		JmsSender jmsSender = new JmsSender();
		jmsSender.setName("Test JmsSender");
		jmsSender.setDestinationName("jms/i4ld_migratepolicy_rpl");
		jmsSender.setDestinationType("QUEUE");
		jmsSender.setAcknowledgeMode("auto");
		jmsSender.setJmsRealm("default");
		jmsSender.setPersistent(true);
		jmsSender.open();
		try {
			jmsSender.sendMessage("Test correlation id", "TEST MESSAGE LINE1\nTEST MESSAGE LINE2\n");
		} catch(Exception e) {
			out.println("ERROR: " + e.getMessage() + "<br/>");
		}
		jmsSender.close();
	}
%>

</body>

</html>

<%!
	String getNextMessage(LineNumberReader reader) {
		String message = null;
		try {
			String line = reader.readLine();
			while (line != null && message == null) {
				if (line.startsWith("<ServiceRequest")) {
					message = line;
				}
				line = reader.readLine();
			}
		} catch(IOException e) {
			message = e.getMessage();
		}
		return message;
	}
%>
