# Frank!Doc user manual for F!F developers

## General usage

The Maven build of this project includes the execution of a doclet, which is implemented in the sub-project `frankDoc`. The doclet produces the following files:
* `./target/frankDoc/js/frankDoc.json`. This file is read by a webapplication implemented in sub-project `webapp`. This web application will render the information in the JSON file. Frank developers use the website as a reference manual. See https://ibis4example.ibissource.org/iaf/frankdoc.
* `./target/frankDoc/xml/xsd/FrankConfig-strict.xsd`. This file is given to Frank developers. They reference this XSD in their Frank config XML files. When they open an XML file, their text editor will use `FrankConfig-strict.xsd` to support autocomplete and to provide tooltip information.
* `./target/frankDoc/xml/xsd/FrankConfig-compatibility.xsd`. This file is added to the Frank!Framework .jar file during the Maven build. The file is then used at runtime to parse Frank configurations.

The frankDoc doclet generates these files based on the Java source code of this repository. The remainder of this document explains how the Java source code is used to define the syntax of Frank configurations. This file does not explain the exact XML schema code produced, but focuses on the rules that have to be followed by Frank developers when they write their configurations. This document also explains what documentation is provided to Frank developers.

## Mapping of XML to Java objects

## Type validation

## Default values and descriptions

## Fine-tuning