# AutoInterface - Java annotation processor
[![Build Status](https://travis-ci.org/high-stakes/AutoInterface.svg?branch=master)](https://travis-ci.org/high-stakes/AutoInterface)
![Maven Central](https://img.shields.io/maven-central/v/io.github.high-stakes/autointerface.svg)
![Bintray](https://img.shields.io/bintray/v/high-stakes/github-maven/io.github.high-stakes%3Aautointerface.svg)

Generate Java interfaces from annotated class files based on their public methods.

## Benefits

* No need to manually maintain interfaces when there is only a single implementation
* Help with dependency injection for frameworks such as Spring
  * Spring runtime injection requires an Interface by default, otherwise spring uses a CGLIB proxy.  
    By auto-generating interfaces you do not need to use CGLIB and be limited by it.
* Help with 3rd party generated code or classes you want to keep `final`

## Usage

```groovy
//Reference dependency
dependencies {
  annotationProcessor 'io.github.high-stakes:<version>'
}
```

```java
//Annotated the class you wish to generate an interface for
@AutoInterface
public final class MyClass {
  public void myMethod() {}
}

//Generates
public interface MyClassInterface {
  void myMethod();
}
```
