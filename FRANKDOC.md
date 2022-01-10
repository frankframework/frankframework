# Frank!Doc user manual for contributors

The Frank!Doc provides reference information for Frank developers as explained in [CONTRIBUTING.md](./CONTRIBUTING.md). It is produced by a doclet that assembles the Frank!Doc from the Java sources. You can control the Frank!Doc using custom JavaDoc tags and using Frank!Doc-related Java annotations. These annotations and tags are summarized in the table below:


| Kind | Name | Appears on | Function |
| ---- | ---- | ---------- | -------- |
| Java annotation | `@FrankDocGroup` | Class or interface | Define group as shown in top-left of webapp. Has fields `name` and `order` (integer).|
| JavaDoc tag | `@ff.parameters` | Class | Describes how parameters (Frank config `<Param>`) are used. |
| JavaDoc tag | `@ff.parameter` | Class | Describes the meaning of a specific parameter. First argument is name of parameter. Second argument is description of that parameter.|
| JavaDoc tag | `@ff.forward` | Class | Describes a forward (e.g. `success`, `failure`). First argument is name of forward. Second argument is description.|
| JavaDoc tag | `@ff.tag` | Class | Tag that classifies the Java class. First argument is tag name, second argument is tag value.|
| JavaDoc tag | `@ff.default` | Attribute setter | Describes default value. |
| Java annotation | `@ProtectedAttribute` | Attribute setter | Suppresses declaration and inheritance of attribute. Annotation is inherited.
| JavaDoc tag | `@ff.mandatory` | Attribute or child setter | Makes attribute or child mandatory in Frank config. |
| JavaDoc tag | `@ff.defaultElement` | Child setter | Set default value of `className` attribute in XSD syntax 1 element. |
| Java annotation | `@EnumLabel` | Enum constant | Set representation required in Frank configs. |

**Annotation @FrankDocGroup, on interface:**  When a Java class implements an interface that has a `@FrankDocGroup` annotation, then the class is put in the group non-exclusively. A class can belong to multiple groups when it implements multiple interfaces with different groups.

**Annotation @FrankDocGroup, on class:** When a class has a `@FrankDocGroup` annotation, then the class only belongs to that group. `@FrankDocGroup` annotations on classes are inherited by descendant classes, so descendant classes are also in the specified group exclusively.
