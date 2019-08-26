package com.highstakes.autointerface;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import java.io.IOException;
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
          .filter(it ->
              it.getKind().equals(ElementKind.CLASS))
          .map(TypeElement.class::cast)
          .forEach(this::generateInterface);
      return true;
    }
    return false;
  }

  private void generateInterface(TypeElement rootTypeElemenet) {
    AutoInterface autoInterface = rootTypeElemenet.getAnnotation(AutoInterface.class);

    TypeSpec.Builder typeSpecbuilder = getTypeSpec(
        rootTypeElemenet, autoInterface.name(), autoInterface.pkg())
        .addMethods(
            filterInterfaceMethods(
                autoInterface.includeInherted() ?  elementUtils.getAllMembers(rootTypeElemenet) :
                rootTypeElemenet.getEnclosedElements()
            ).stream()
                .map(it -> getMethodSpec(rootTypeElemenet, it))
                .collect(Collectors.toList()));

    String packageName =  !"".equals(autoInterface.pkg()) ? autoInterface.pkg() :
        elementUtils.getPackageOf(rootTypeElemenet).getQualifiedName().toString();
    JavaFile javaFile =
        JavaFile.builder(packageName, typeSpecbuilder.build()).indent("    ").build();
    try {
      javaFile.writeTo(filer);
    } catch (IOException e) {
      messager.printMessage(Diagnostic.Kind.ERROR,
          "AutoInterface: Error generating interface: " + e.getMessage());
    }
  }

  private List<ExecutableElement> filterInterfaceMethods(List<? extends Element> elementList) {
    return elementList.stream()
        .filter(it -> it.getKind().equals(ElementKind.METHOD))
        .filter(it -> ElementKind.CLASS.equals(it.getEnclosingElement().getKind()))
        .filter(it -> !objectMethods.contains(it.toString()))
        .map(ExecutableElement.class::cast)
        .filter(it -> !it.getModifiers().contains(Modifier.STATIC))
        .filter(it -> it.getModifiers().contains(Modifier.PUBLIC))
        .collect(Collectors.toList());
  }

  private TypeSpec.Builder getTypeSpec(TypeElement type, String name, String pkg) {
    return TypeSpec.interfaceBuilder(getInterfaceClass(type, name, pkg))
        .addModifiers(Modifier.PUBLIC)
        .addTypeVariables(getTypeVariables(type))
        .addOriginatingElement(type);
  }

  private MethodSpec getMethodSpec(TypeElement rootType, ExecutableElement method) {
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

  private ClassName getInterfaceClass(TypeElement type, String name, String pkg) {
    String interfaceName = !"".equals(name) ? name : getClassName(type) + "Interface";
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
