package com.highstakes.autointerface;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import org.checkerframework.checker.signature.qual.FullyQualifiedName;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

@AutoService(Processor.class)
@SupportedAnnotationTypes({"com.highstakes.autointerface.AutoInterface"})
public class AutoInterfaceProcessor extends AbstractProcessor {
  private Elements elementUtils;
  private Types typeUtils;
  private Filer filer;
  private Set<String> objectMethods;
  private Messager messager;

  @Override public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latest();
  }

  @Override
  public synchronized void init(ProcessingEnvironment env) {
    super.init(env);
    this.elementUtils = env.getElementUtils();
    this.typeUtils = env.getTypeUtils();
    this.filer = env.getFiler();
    this.messager = env.getMessager();
    this.objectMethods = getObjectMethodSignatures();
  }

  @Override
  public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
    if (!roundEnvironment.processingOver()) {
      roundEnvironment.getElementsAnnotatedWith(AutoInterface.class)
          .stream()
          .filter(it -> Arrays.asList(ElementKind.CLASS, ElementKind.INTERFACE).contains(it.getKind()))
          .map(TypeElement.class::cast)
          .forEach(this::generateInterface);
      return true;
    }
    return false;
  }

  private void generateInterface(TypeElement rootTypeElemenet) {
    AutoInterface autoInterface = rootTypeElemenet.getAnnotation(AutoInterface.class);
    String packageName =  !"".equals(autoInterface.pkg()) ? autoInterface.pkg() :
            elementUtils.getPackageOf(rootTypeElemenet).getQualifiedName().toString();
    List<JavaFile> javaFiles = new ArrayList<>();
    boolean mainInterfaceIsRootElement = rootTypeElemenet.getKind().equals(ElementKind.INTERFACE);

    TypeSpec mainInterfaceSpec = createMainInterfaceTypeSpec(
            rootTypeElemenet, autoInterface.name(), autoInterface.pkg(), mainInterfaceIsRootElement)
        .addMethods(createMainInterfaceMethodSpec(autoInterface, rootTypeElemenet, mainInterfaceIsRootElement)).build();
    if (!mainInterfaceIsRootElement) {
      javaFiles.add(JavaFile.builder(packageName, mainInterfaceSpec).indent("    ").build());
    }

    if (autoInterface.createDecorator()) {
      TypeSpec decoratorSpec = createDecoratorTypeSpec(rootTypeElemenet, autoInterface.decoratorName(), autoInterface.pkg(),
              mainInterfaceSpec, mainInterfaceIsRootElement)
              .addMethods(createDecoratorMethodSpec(packageName, rootTypeElemenet, mainInterfaceSpec,
                      mainInterfaceIsRootElement)).build();
      javaFiles.add(JavaFile.builder(packageName, decoratorSpec).indent("    ").build());
    }

    try {
      for (JavaFile javaFile : javaFiles) {
          javaFile.writeTo(filer);
      }
    } catch (IOException e) {
      messager.printMessage(Diagnostic.Kind.ERROR, "AutoInterface: Error generating interface: " + e.getMessage());
    }
  }

  private List<MethodSpec> createDecoratorMethodSpec(String pkg, TypeElement type,
          TypeSpec mainInterfaceSpec, boolean mainInterfaceIsRootElement) {
    List<MethodSpec> methodSpecs = new ArrayList<>();
    methodSpecs.add(MethodSpec.methodBuilder("getDecoratedObject")
            .returns(
                    type.getTypeParameters().isEmpty() ?
                        mainInterfaceIsRootElement ? ClassName.get(type) : ClassName.get(pkg, mainInterfaceSpec.name)
                        : ParameterizedTypeName.get(
                            mainInterfaceIsRootElement ? ClassName.get(type) : ClassName.get(pkg, mainInterfaceSpec.name),
                            type.getTypeParameters().stream().map(it -> TypeName.get(it.asType())).toArray(TypeName[]::new)
                        )
            )
            .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
            .build());
    methodSpecs.addAll(mainInterfaceSpec.methodSpecs.stream()
            .map(it -> MethodSpec.methodBuilder(it.name)
                    .returns(it.returnType)
                    .addExceptions(it.exceptions)
                    .addTypeVariables(it.typeVariables)
                    .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                    .addParameters(it.parameters)
                    .addAnnotation(Override.class)
                    .addCode(
                            CodeBlock.of(
                                    (it.returnType.equals(TypeName.VOID) ? "" : "return ") +
                                    "getDecoratedObject()." + it.name + "("+
                                    it.parameters.stream().map(parameterSpec -> parameterSpec.name)
                                            .collect(Collectors.joining(","))
                                    +");")
                    )
                    .build()
            )
            .collect(Collectors.toList()));
    return methodSpecs;
  }

  private List<MethodSpec> createMainInterfaceMethodSpec(AutoInterface autoInterface, TypeElement rootTypeElemenet,
          boolean mainInterfaceIsRootElement) {
    List<? extends Element> elementList = autoInterface.includeInherted() ?  elementUtils.getAllMembers(rootTypeElemenet) :
            rootTypeElemenet.getEnclosedElements();
    return elementList.stream()
            .filter(it -> it.getKind().equals(ElementKind.METHOD))
            .filter(it -> Arrays.asList(ElementKind.CLASS, ElementKind.INTERFACE).contains(it.getEnclosingElement().getKind()))
            .filter(it -> !objectMethods.contains(it.toString()))
            .map(ExecutableElement.class::cast)
            .filter(it -> !it.getModifiers().contains(Modifier.STATIC))
            .filter(it -> it.getModifiers().contains(Modifier.PUBLIC))
            .map(it -> createMethodSpec(rootTypeElemenet, it))
            .distinct()
            .collect(Collectors.toList());
  }

  private TypeSpec.Builder createMainInterfaceTypeSpec(TypeElement type, String name, String pkg,
          boolean mainInterfaceIsRootElement) {
    return TypeSpec.interfaceBuilder(
            mainInterfaceIsRootElement ? ClassName.get(type) : getInterfaceClass(type, name, pkg, "Interface"))
        .addModifiers(Modifier.PUBLIC)
        .addTypeVariables(getTypeVariables(type))
        .addOriginatingElement(type);
  }

  private TypeSpec.Builder createDecoratorTypeSpec(TypeElement type, String name, String pkg, TypeSpec mainInterfaceSpec,
          boolean mainInterfaceIsRootElement) {
    return TypeSpec.interfaceBuilder(getInterfaceClass(type, name, pkg, "Decorator"))
            .addModifiers(Modifier.PUBLIC)
            .addTypeVariables(getTypeVariables(type))
            .addSuperinterface(
                    type.getTypeParameters().isEmpty() ?
                        mainInterfaceIsRootElement ? ClassName.get(type) : ClassName.get(pkg, mainInterfaceSpec.name)
                        : ParameterizedTypeName.get(
                            mainInterfaceIsRootElement ? ClassName.get(type) : ClassName.get(pkg, mainInterfaceSpec.name),
                            type.getTypeParameters().stream().map(it -> TypeName.get(it.asType())).toArray(TypeName[]::new)
                        )
            )
            .addOriginatingElement(type);
  }

  private MethodSpec createMethodSpec(TypeElement rootType, ExecutableElement method) {
    MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(method.getSimpleName().toString());
    ExecutableType methodType =
        (ExecutableType) typeUtils.asMemberOf((DeclaredType) rootType.asType(), method);

    List<? extends TypeMirror> paramTypes = methodType.getParameterTypes();
    List<? extends VariableElement> params = method.getParameters();
    for (int i = 0; i < paramTypes.size(); i++) {
      methodBuilder.addParameter(
          ParameterSpec.builder(TypeName.get(paramTypes.get(i)),
              params.get(i).getSimpleName().toString())
              .build());
    }
    return methodBuilder
        .returns(ClassName.get(methodType.getReturnType()))
        .addExceptions(methodType.getThrownTypes()
            .stream()
            .map(ClassName::get)
            .collect(Collectors.toList()))
        .addTypeVariables(method.getTypeParameters()
            .stream()
            .map(TypeVariableName::get)
            .collect(Collectors.toList()))
        .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
        .build();
  }

  private ClassName getInterfaceClass(TypeElement type, String name, String pkg, String fallbackSuffix) {
    String interfaceName = !"".equals(name) ? name : getClassName(type) + fallbackSuffix;
    String packageName = !"".equals(pkg) ? pkg : elementUtils.getPackageOf(type).getQualifiedName().toString();
    return ClassName.get(packageName, interfaceName);
  }

  private Iterable<TypeVariableName> getTypeVariables(TypeElement type) {
    return type.getTypeParameters()
        .stream()
        .map(TypeVariableName::get)
        .collect(Collectors.toList());
  }

  private String getClassName(TypeElement type) {
    Stack<TypeElement> typeChain = new Stack<>();
    while (true) {
      typeChain.add(type);
      Element enclosingElement = type.getEnclosingElement();
      if (enclosingElement.getKind().equals(ElementKind.PACKAGE)) {
        return typeChain.stream()
            .map(element -> element.getSimpleName().toString())
            .map(str -> str.substring(0, 1).toUpperCase() + str.substring(1))
            .collect(Collectors.joining());
      }
      type = (TypeElement) enclosingElement;
    }
  }

  private Set<String> getObjectMethodSignatures() {
    return elementUtils.getTypeElement("java.lang.Object")
        .getEnclosedElements().stream()
        .filter(e -> e.getKind().equals(ElementKind.METHOD))
        .map(ExecutableElement.class::cast)
        .filter(e -> !e.getModifiers().contains(Modifier.STATIC))
        .map(ExecutableElement::toString)
        .collect(Collectors.toSet());
  }
}
