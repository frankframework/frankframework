# Frank!Doc user manual for F!F developers

## General usage

The Maven build of this project includes the execution of a doclet, which is implemented in the sub-project `frankDoc`. The doclet produces the following files:
* `./target/frankDoc/js/frankDoc.json`. This file is read by a webapplication implemented in sub-project `webapp`. This web application will render the information in the JSON file. Frank developers use the website as a reference manual. See https://ibis4example.ibissource.org/iaf/frankdoc.
* `./target/frankDoc/xml/xsd/FrankConfig-strict.xsd`. This file is given to Frank developers. They reference this XSD in their Frank config XML files. When they open an XML file, their text editor will use `FrankConfig-strict.xsd` to support autocomplete and to provide tooltip information.
* `./target/frankDoc/xml/xsd/FrankConfig-compatibility.xsd`. This file is added to the Frank!Framework .jar file during the Maven build. The file is then used at runtime to parse Frank configurations.

These files define the syntax of Frank configurations written by Frank developers. They also provide documentation about the semantics, the meaning, of the XML elements and attributes in a Frank config.

The frankDoc doclet generates these files based on the Java source code of this repository, including the JavaDoc comments. The remainder of this document explains how the Java source code is used to define the syntax of Frank configurations. This file does not explain the exact XML schema code produced, but focuses on the rules that have to be followed by Frank developers when they write their configurations.

## Config children

An XML document read by the F!F, a Frank configuration, consists of nested elements that each can have attributes. Each element that is allowed in the XML has a corresponding Java class. The top-level XML element of a configuration is `<Configuration>`, which corresponds to Java class [Configuration](./core/src/main/java/nl/nn/adapterframework/configuration/Configuration.java). For each allowed sub-element of an element, the element's Java class has a method (config child setter). As an example, Java class `Configuration` has the following config child setter:

```
	public void registerAdapter(Adapter adapter)
```

The frankDoc uses this method to introduce a sub-element of `<Configuration>` that matches with Java type of the method argument, [Adapter](./core/src/main/java/nl/nn/adapterframework/core/Adapter.java). From the import statements of class Configuration you can see that this is `nl.nn.adapterframework.core.Adapter`.

What XML tag will be introduced to reference Java class Adapter? This tag name is based on a file [digester-rules.xml](./core/src/main/resources/digester-rules.xml). This file has the following line:

```
	<rule pattern="*/adapter" registerMethod="registerAdapter" />
```

This line mentions method `registerAdapter` as attribute "registerMethod" and links it to "pattern" `*/adapter`. The string after the last `/` is called the *role name*. The argument type [Adapter](./core/src/main/java/nl/nn/adapterframework/core/Adapter.java) is not a Java interface. In this case, the introduced XML element is the role-name transformed to camel case, which is `<Adapter>`.

When a configuration is parsed, the F!F creates an object of of type [Configuration](./core/src/main/java/nl/nn/adapterframework/configuration/Configuration.java). When the tag `<Adapter>` is encountered, an [Adapter](./core/src/main/java/nl/nn/adapterframework/core/Adapter.java) object is created. The Configuration's method `registerAdapter()` is called with the Adapter object as the argument. This way, the Configuration object gets access to the Adapter object. The same is done recursively for the other config child setters of class [Configuration](./core/src/main/java/nl/nn/adapterframework/configuration/Configuration.java) and class [Adapter](./core/src/main/java/nl/nn/adapterframework/core/Adapter.java). The parse process produces an object of type [Configuration](./core/src/main/java/nl/nn/adapterframework/configuration/Configuration.java) that has a member variable pointing to objects of type [Adapter](./core/src/main/java/nl/nn/adapterframework/core/Adapter.java). There are also member variables pointing to other objects that correspond to the the other child XML elements. The child objects of the Configuration](./core/src/main/java/nl/nn/adapterframework/configuration/Configuration.java) in turn have child objects that correspond to the child XML elements nested in the children of `<Configuration>`.

The config child setter `registerAdapter()` has a non-interface argument. Config child setters that have a Java interface as argument have a different rule for the corresponding XML element. As an example, consider the following config child setter of class [Receiver](./core/src/main/java/nl/nn/adapterframework/receivers/Receiver):

```
	public void setErrorSender(ISender errorSender)
```

This config child setter introduces many different XML elements. For every implementation of Java interface `ISender` an XML element is introduced. As an example, consider implementation [LogSender](./src/main/java/nl/nn/adapterframework/senders/LogSender). To find the name of the XML tag, first the class simple name `LogSender` is taken. Then it is checked if the class name ends with the interface name reduced with `I`. For `LogSender` this applies: the name ends with `Sender`. Then the suffix is removed and the camel-cased role name `ErrorSender` obtained from [digester-rules.xml](/core/src/main/resources/digester-rules.xml) is appended. The introduced XML tag is `LogErrorSender`.

This config child setter also introduces an XML tag `<ErrorSender>`, derived from the role name. It has a mandatory attribute `className` that should be the full name of a Java class that implements interface `ISender`. This way, we support that the Frank!Framework uses custom code. Any class on the Java classpath can be referenced this way, also if it is not part of this repository.

Please note that [Receiver](./core/src/main/java/nl/nn/adapterframework/receivers/Receiver) also has a config child setter

```
	public void setSender(ISender sender)
```

This config child setter introduces XML tags `<LogSender>` and similar XML elements for other implementations of `ISender`, and the general element option `<Sender>` that has a mandatory `className` attribute. XML elements `<LogErrorSender>` and `<logSender>` reference the same Java class, so what is the difference? The difference is the config child setter that is applied to register the `LogSender` Java object with the `Receiver` object. The role played by the `LogSender` within the `Receiver` object is different.

Configuration children are inherited. A `<LogSender>` can have a `<Param>` tag because [LogSender](./core/src/main/java/nl/nn/adapterframework/senders/LogSender.java) extends
[SenderWithParametersBase](./core/src/main/java/nl/nn/adapterframework/senders/SenderWithParametersBase.java) which has config child setter `public void addParameter(Parameter p)`.

Not every Java method introduces a config child. Here are the rules for config child setters:
* A config child setter is public, returns void and takes one argument.
* The argument is a non-primitive Java type and it is also not a boxed primitive type. The argument can be of type String, which is a special case that will be explained later. Normally, the argument type is a Java class that can be used as a building block of a configuration.
* Only classes that are used as config children themselves can have config child setters. Consider a hypothetic class Xyz on the classpath. It is not the argument of a config child setter. It also does not implement any interface that is the argument of a config child setter. Then no method of Xyz is a config child setter.
* Only methods that have a role name in [digester-rules.xml](./core/src/main/resources/digester-rules.xml) can be config child setters.
* A config child setter starting with the string `set` results in an XML element that can be added only once within its direct parent element.
* A confic child setter starting with the string `add` or the string `register` results in an XML element that is allowed to occur multiple times.

## Attributes

## Default values and descriptions

## Fine-tuning