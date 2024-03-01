# Frank!Doc user manual for contributors

The Frank!Doc provides reference information for Frank developers as explained in [CONTRIBUTING.md](./CONTRIBUTING.md). It is produced by a doclet that assembles the Frank!Doc from the Java sources. You can control the Frank!Doc using custom JavaDoc tags and using Frank!Doc-related Java annotations. These annotations and tags are summarized in the table below:

| JavaDoc Tag | Java annotation | Appears on | Function |
| ---- | ---- | ---------- | -------- |
| | `@FrankDocGroup` | Class or interface | Define group as shown in top-left of webapp. |
| `@ff.parameters` | | Class | Describes how parameters (Frank config `<Param>`) are used. |
| `@ff.parameter` | | Class | Describes the meaning of a specific parameter. First argument is name of parameter. Second argument is description of that parameter. |
| `@ff.forward` | | Class | Describes a forward (e.g. `success`, `failure`). First argument is name of forward. Second argument is description. |
| `@ff.tag` | | Class | Tag that classifies the Java class. First argument is tag name, second argument is tag value. |
| `@ff.default` | `@Default` | Attribute setter | Describes default value. |
| `@ff.ref` | `@ReferTo` | Attribute setter | References another Java method from which the description and the default value should be obtained. The reference can give only the class name that has the method, or in case of the JavaDoc tag the class name and the method name separated by a dot. Always use the full class name. |
| `@ff.protected` | `@Protected` | Attribute setter, child setter or class | Suppresses declaration and inheritance of attribute, child or element. Annotation is inherited. |
| `@ff.excludeFromType` | `@ExcludeFromType` | Class | Omit as element of specific Frank!Doc types (e.g. ITransactionalStorage). Annotation is inherited. |
| `@ff.mandatory` | `@Mandatory` | Attribute or child setter | Makes attribute or child mandatory in Frank config. |
| `@ff.optional` | `@Optional` | Attribute or child setter | Undoes inherited `@ff.mandatory`, making the attribute or child optional even if it overrides a mandatory attribute or child.
| `@ff.defaultElement` | | Child setter | Set default value of `className` attribute in XSD syntax 1 element. |
| | `@EnumLabel` | Enum constant | Set representation required in Frank configs. |
| `@ff.reintroduce` | `@Reintroduce` | Attribute or child setter | Used to change the order of config children or attributes; see Frank!Doc's README file for details. |
| - | `@Label` | Java Annotation | When the target annotation is placed on a Java class, a label is added in the Frank!Doc webapp. The value of the label is the `value()` field of the target annotation. The name of the label comes from the `@Label` annotation within the definition of the target annotation, attribute `name`.

**Annotation @FrankDocGroup, on interface:**  When a Java class implements an interface that has a `@FrankDocGroup` annotation, then the class is put in the group non-exclusively. A class can belong to multiple groups when it implements multiple interfaces with different groups.

**Annotation @FrankDocGroup, on class:** When a class has a `@FrankDocGroup` annotation, then the class only belongs to that group. `@FrankDocGroup` annotations on classes are inherited by descendant classes, so descendant classes are also in the specified group exclusively.

**@ff.mandatory and @Mandatory:** If you give the JavaDoc tag the value `ignoreInCompatibilityMode`, the attribute or config child will not be mandatory in `FrankConfig-compatibility.xsd`. This behavior may be useful for backward compatibility. The annotation has a Boolean field `ignoreInCompatibilityMode` that does the same.

**@ff.protected and @Protected:** If a config child setter has a non-interface Java class as its argument and if that class has or inherits this annotation, then no config child is created.
