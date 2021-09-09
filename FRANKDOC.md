# Frank!Doc user manual for F!F developers

## Introduction

By editing the Java code of the Frank!Framework, you update both the behavior of the F!F and the documentation of that behavior as expressed in the Frank!Doc. See [CONTRIBUTING.md](./CONTRIBUTING.md).

The Frank!Doc documents the following:
* XML elements that Frank developers can use in their Frank config XML documents.
* XML attributes allowed in these XML elements.
* Descriptions that a Frank developer sees for the elements and the attributes.
* The default values of XML attributes.
* Groups shown in the Frank!Doc webapplication.
* Parameters.

The Frank!Framework parses a Frank configuration into a Java object of type [Configuration](./core/src/main/java/nl/nn/adapterframework/configuration/Configuration.java). This object recursively contains child objects. These objects have JavaBean properties that are configured with setter methods (property `xyz` has setter `setXyz()`. The Frank!Framework accesses these objects to do the job that the Frank developer intends.

This document helps you to write the right Java code, including JavaDocs and Java annotations, in such a way that the Frank!Doc remains correct and useful. It explains how to define the object relations that can exist within a configuration. It also shows how to define properties that are usually configured by Frank developers through XML attributes. And there is additional information about groups in the Frank!Doc web application and parameters.

## Expressing the root object

#### Root object and root XML elements automatic

A Frank configuration is always expressed as an object of class [Configuration](./core/src/main/java/nl/nn/adapterframework/configuration/Configuration.java). The Frank!Doc defines a corresponding XML element `<Configuration>` that should be the root element of a Frank configuration. This happens automatically. You do not have to add any JavaDoc or annotation to class [Configuration](./core/src/main/java/nl/nn/adapterframework/configuration/Configuration.java) to define it as the root object.

The Frank!Doc also defines another element that can be the root of an XML file, namely `<Module>`. This will be the case when pull request https://github.com/ibissource/iaf/pull/2186 will have been merged. `<Module>` is the only XML element that has no relation to a Java class within this repository. You do not have to add any Java class, Java annotation or JavaDoc to introduce `<Module>`. XML files starting with a `<Module>` element are meant to be included as XML entity references. Using entity references is explained in the [Frank!Manual](https://frank-manual.readthedocs.io). When the Frank!Doc parses a Frank configuration, it resolves the entity references and ignores the `<Module>` elements. The child XML elements that are allowed for `<Module>` are the same as the child elements allowed for `<Configuration>`.

#### Description

The JavaDoc comment within file [Configuration.java](./core/src/main/java/nl/nn/adapterframework/configuration/Configuration.java) above the class declaration appears as description in the Frank!Doc. The first sentence appears as a tooltip help in the Frank developer's text editor. To illustrate this, assume that a Frank developer types the following in Visual Studio Code:

```
<Configuration
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:noNamespaceSchemaLocation="./frankdoc.xsd">
</Configuration>
```

If she hovers over the `<Configuration>` element, the following text appears:

```
nl.nn.adapterframework.configuration.Configuration - The Configuration is placeholder of all configuration objects.
```

The full text of the JavaDoc comment in [Configuration.java](./core/src/main/java/nl/nn/adapterframework/configuration/Configuration.java) appears in the Frank!Doc web application as shown below:

![FrankDoc website description of Configuration](./webapp-configuration.jpg)

The first sentence of class Configuration's JavaDoc has to end with ". ", not only ".". You can have a first sentence like "This object accesses x.y.z to do my job.". A dot directly followed by a newline is also considered as the end of a sentence.

## Expressing nested objects

## Expression object properties

## Groups

## Parameters
