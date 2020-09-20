
# DazzleConf

Prepare to be dazzled

## Introduction

It's here. The moment you've been waiting for.

A type-safe, thread-safe, fail-fast, user-oriented, easy to setup, extensible and flexible configuration library.

### Objectives

* *Eliminate* stringly-typed spaghetti such as `getString("key")` or `getAsInt("section.subsection")`. Use your configuration just as you would any other immutable bean.

* *Write defaults once* and never both with them again.

* *Validate configuration* in all manners, and fail-fast with informative messages.

* *Easily update* old configurations with the latest keys. No need to version config files.

* *Pave the way* for users with incredibly flexible type conversion.

* *Take advantage of* enums, collections, and nested configuration sections.

### Features

* Lightweight, simple, well-tested library.
* Immutable and thread safe by design. Values loaded once and never modified thereafter.
* Readable for programmers and users. Annotation-driven and supports comments.
* Configuration objects are format-independent.
* Support for writing configs as YAML and JSON out of the box, but allows easy extension with more formats.
* Identify the precise cause of user errors.
* Use a decoupled and testable config interface.

## Usage

### Documentation

The javadocs are published with the artifact and cover the details of specific classes and methods. General documentation is available in the docs/ folder of this repository.

### Example

```java
public interface MyConfig {

  @DefaultInteger(3)
  int someInteger();
  
  @ConfKey("display.user-message")
  @ConfComment("The message shown when a certain thing happens")
  @DefaultString("Hello user")
  String userMessage();
  
  @ConfKey("display.enable-user-message")
  @DefaultBoolean(true)
  boolean enableUserMessage();
  
  @IntegerRange(min = 1, max = Integer.MAX_VALUE + 1)
  @DefaultLong(10)
  long boundedNumeric();
  
  @DefaultString("ONE")
  MyEnum enumValue();
  
  @NestedConfig
  SubSection nestedConfigSection();
  
  @ConfSerialiser(URLSerialiser.class)
  @DefaultString("https://github.com")
  URL validUrl();

}

public enum MyEnum {
  ONE,
  TWO
}


public interface SubSection {

  @ConfComment("Every annotation shown above works here too")
  @DefaultString("Also, all annotations are inherited, enabling inheritable config interfaces")
  String flexibility();

}
```

When using the YAML configuration format, the following result can be generated by writing the default configuration:

```yaml
someInteger: 3
display:
  # The message shown when a certain thing happens
  user-message: 'Hello user'
  enable-user-message: true
boundedNumeric: 10
enumValue: 'ONE'
nestedConfigSection:
  # Every annotation shown above works here too
  flexibility: 'Also, all annotations are inherited, enabling inheritable config interfaces'
validUrl: 'https://github.com'
```

The same document can be reparsed to an instance of the configuration interface. Type and constraint validation is performed when config values are parsed and loaded, not when they are retrieved - having an instance of the config interface is enough to ensure the configuration is valid.

### Requirements

* Java 11
* Java 8: coming soon

DazzleConf uses the module system to strongly encapsulate internals; it also uses the collection factory methods - List.of(), Set.of(), Map.of().

A backport for Java 8, with weaker encapsulation and less efficient immutable collections, is in progress.

### Dependency

The dependency FQDN is `space.arim.dazzleconf:dazzleconf-core:{VERSION}`.

Format extensions are in the form `dazzleconf-ext-{extension}`, the available extensions currently being 'snakeyaml' and 'gson'.

The repository for all artifacts is:

```
https://mvn-repo.arim.space/lesser-gpl3/
```

A thanks to Cloudsmith for providing free repositories for FOSS.

### Documentations

See the docs folder of this repository, specifically these files:

* MainIdeas.md
* Annotations.md
* Options.md

Additionally, the javadocs are attached to the artifact.

### License

LGPL. See the license file for more information.
