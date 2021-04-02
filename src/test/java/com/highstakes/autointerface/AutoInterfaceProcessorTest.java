package com.highstakes.autointerface;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import javax.tools.JavaFileObject;
import org.junit.Test;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

public class AutoInterfaceProcessorTest {

  @Test
  public void shouldInheritGenericsAndSubstituteTypes() {
    JavaFileObject superSuperClass = JavaFileObjects.forSourceLines("test.SuperSuperClass",
        "package test;",
        "import java.util.ArrayList;",
        "",
        "public class SuperSuperClass extends ArrayList<Boolean> {",
        "    public void superSuperMethod1() {}",
        "}");
    JavaFileObject superClass = JavaFileObjects.forSourceLines("test.SuperClass",
        "package test;",
        "import java.util.List;",
        "",
        "public class SuperClass<B> extends SuperSuperClass implements List<Boolean> {",
        "    public boolean superMethod1(String input1, boolean input2) throws Exception { return input2; }",
        "    public void superSuperMethod1() {}",
        "    public B superMethod2(B input) throws Exception {return input;}",
        "}");
    JavaFileObject baseClass = JavaFileObjects.forSourceLines("test.BasicClass",
        "package test;",
        "",
        "import com.highstakes.autointerface.AutoInterface;",
        "",
        "@AutoInterface(includeInherted = true)",
        "public class BasicClass<T extends Integer> extends SuperClass<Long> {",
        "    public T method1(T input) { return input; }",
        "    public Integer method2(Integer input) throws Exception { return input; }",
        "}");

    JavaFileObject generatedInterface = JavaFileObjects.forSourceLines("test.BasicClassInterface",
        "package test;\n"
            + "\n"
            + "import java.lang.Boolean;\n"
            + "import java.lang.Exception;\n"
            + "import java.lang.Integer;\n"
            + "import java.lang.Long;\n"
            + "import java.lang.Object;\n"
            + "import java.lang.String;\n"
            + "import java.util.Collection;\n"
            + "import java.util.Comparator;\n"
            + "import java.util.Iterator;\n"
            + "import java.util.List;\n"
            + "import java.util.ListIterator;\n"
            + "import java.util.Spliterator;\n"
            + "import java.util.function.Consumer;\n"
            + "import java.util.function.Predicate;\n"
            + "import java.util.function.UnaryOperator;\n"
            + "\n"
            + "public interface BasicClassInterface<T extends Integer> {\n"
            + "    boolean containsAll(Collection<?> c);\n"
            + "\n"
            + "    void trimToSize();\n"
            + "\n"
            + "    void ensureCapacity(int minCapacity);\n"
            + "\n"
            + "    int size();\n"
            + "\n"
            + "    boolean isEmpty();\n"
            + "\n"
            + "    boolean contains(Object o);\n"
            + "\n"
            + "    int indexOf(Object o);\n"
            + "\n"
            + "    int lastIndexOf(Object o);\n"
            + "\n"
            + "    Object[] toArray();\n"
            + "\n"
            + "    <T> T[] toArray(T[] a);\n"
            + "\n"
            + "    Boolean get(int index);\n"
            + "\n"
            + "    Boolean set(int index, Boolean element);\n"
            + "\n"
            + "    boolean add(Boolean e);\n"
            + "\n"
            + "    void add(int index, Boolean element);\n"
            + "\n"
            + "    Boolean remove(int index);\n"
            + "\n"
            + "    boolean remove(Object o);\n"
            + "\n"
            + "    void clear();\n"
            + "\n"
            + "    boolean addAll(Collection<? extends Boolean> c);\n"
            + "\n"
            + "    boolean addAll(int index, Collection<? extends Boolean> c);\n"
            + "\n"
            + "    boolean removeAll(Collection<?> c);\n"
            + "\n"
            + "    boolean retainAll(Collection<?> c);\n"
            + "\n"
            + "    ListIterator<Boolean> listIterator(int index);\n"
            + "\n"
            + "    ListIterator<Boolean> listIterator();\n"
            + "\n"
            + "    Iterator<Boolean> iterator();\n"
            + "\n"
            + "    List<Boolean> subList(int fromIndex, int toIndex);\n"
            + "\n"
            + "    void forEach(Consumer<? super Boolean> action);\n"
            + "\n"
            + "    Spliterator<Boolean> spliterator();\n"
            + "\n"
            + "    boolean removeIf(Predicate<? super Boolean> filter);\n"
            + "\n"
            + "    void replaceAll(UnaryOperator<Boolean> operator);\n"
            + "\n"
            + "    void sort(Comparator<? super Boolean> c);\n"
            + "\n"
            + "    boolean superMethod1(String input1, boolean input2) throws Exception;\n"
            + "\n"
            + "    void superSuperMethod1();\n"
            + "\n"
            + "    Long superMethod2(Long input) throws Exception;\n"
            + "\n"
            + "    T method1(T input);\n"
            + "\n"
            + "    Integer method2(Integer input) throws Exception;\n"
            + "}"
    );

    Compilation compilation = javac().withProcessors(new AutoInterfaceProcessor())
        .compile(baseClass, superClass, superSuperClass);
    assertThat(compilation).succeededWithoutWarnings();
    assertThat(compilation)
        .generatedSourceFile("test.BasicClassInterface")
        .hasSourceEquivalentTo(generatedInterface);
  }


  @Test
  public void shouldFollowCustomNaming() {
    JavaFileObject sourceClass = JavaFileObjects.forSourceLines("test.BaseClass",
        "package test;",
        "import com.highstakes.autointerface.AutoInterface;",
        "",
        "@AutoInterface(name = \"BaseInt\")",
        "public class BaseClass {",
        "    public void baseClassMethod1() {}",
        "}");

    JavaFileObject generatedInterface = JavaFileObjects.forSourceLines("test.BaseInt",
        "package test;\n"
            + "\n"
            + "public interface BaseInt {\n"
            + "    void baseClassMethod1();\n"
            + "}"
    );

    Compilation compilation = javac().withProcessors(new AutoInterfaceProcessor())
        .compile(sourceClass);
    assertThat(compilation).succeededWithoutWarnings();
    assertThat(compilation)
        .generatedSourceFile("test.BaseInt")
        .hasSourceEquivalentTo(generatedInterface);
  }

  @Test
  public void shouldFollowCustomPkg() {
    JavaFileObject sourceClass = JavaFileObjects.forSourceLines("test.BaseClass",
        "package test;",
        "import com.highstakes.autointerface.AutoInterface;",
        "",
        "@AutoInterface(pkg = \"result\")",
        "\n",
        "public class BaseClass {",
        "    public void baseClassMethod1() {}",
        "}");

    JavaFileObject generatedInterface = JavaFileObjects.forSourceLines("result.BaseClassInterface",
        "package result;\n"
            + "\n"
            + "public interface BaseClassInterface {\n"
            + "    void baseClassMethod1();\n"
            + "}"
    );

    Compilation compilation = javac().withProcessors(new AutoInterfaceProcessor())
        .compile(sourceClass);
    assertThat(compilation).succeededWithoutWarnings();
    assertThat(compilation)
        .generatedSourceFile("result.BaseClassInterface")
        .hasSourceEquivalentTo(generatedInterface);
  }

  @Test
  public void shouldWorkWithStaticClasses() {
    JavaFileObject sourceClass = JavaFileObjects.forSourceLines("test.BaseClass",
            "package test;",
            "import com.highstakes.autointerface.AutoInterface;",
            "",
            "public class BaseClass {",
            "    public void baseClassMethod1() {}",
            "    @AutoInterface(pkg = \"result\")",
            "    public static class InnerClass {",
            "           public void innerClassMethod1() {}",
            "    }",
            "}");

    JavaFileObject generatedInterface = JavaFileObjects.forSourceLines("result.BaseClassInterface",
            "package result;\n"
                    + "\n"
                    + "public interface InnerClassBaseClassInterface {\n"
                    + "    void innerClassMethod1();\n"
                    + "}"
    );

    Compilation compilation = javac().withProcessors(new AutoInterfaceProcessor())
            .compile(sourceClass);
    assertThat(compilation).succeededWithoutWarnings();
    assertThat(compilation)
            .generatedSourceFile("result.InnerClassBaseClassInterface")
            .hasSourceEquivalentTo(generatedInterface);
  }

  @Test
  public void shouldGenerateDecorator() {
    JavaFileObject sourceClass = JavaFileObjects.forSourceLines("test.BaseClass",
            "package test;",
            "import com.highstakes.autointerface.AutoInterface;",
            "import java.util.ArrayList;",
            "import java.util.List;",
            "",
            "@AutoInterface(pkg = \"result\", createDecorator = true)",
            "public class BaseClass<T, G extends List<T>> {",
            "    public T baseClassMethod1(T p1, ArrayList<? extends ArrayList> p2) { return p1; }",
            "}");

    JavaFileObject generatedInterface = JavaFileObjects.forSourceLines("result.BaseClassInterface",
            "package result;\n"
                    + "import java.util.ArrayList;\n"
                    + "import java.util.List;\n"
                    + "\n"
                    + "public interface BaseClassInterface<T, G extends List<T>> {\n"
                    + "    T baseClassMethod1(T p1, ArrayList<? extends ArrayList> p2);\n"
                    + "}"
    );

    JavaFileObject generatedDecorator = JavaFileObjects.forSourceLines("result.BaseClassInterface",
            "package result;\n"
                    + "\n"
                    + "import java.lang.Override;\n"
                    + "import java.util.ArrayList;\n"
                    + "import java.util.List;\n"
                    + "\n"
                    + "public interface BaseClassDecorator<T, G extends List<T>> extends BaseClassInterface<T, G> {\n"
                    + "    BaseClassInterface<T, G> getDecoratedObject();\n"
                    + "\n"
                    + "    @Override\n"
                    + "    default T baseClassMethod1(T p1, ArrayList<? extends ArrayList> p2) {\n"
                    + "        return getDecoratedObject().baseClassMethod1(p1,p2);\n"
                    + "    }\n"
                    + "}"
    );

    Compilation compilation = javac().withProcessors(new AutoInterfaceProcessor())
            .compile(sourceClass);
    assertThat(compilation).succeededWithoutWarnings();
    assertThat(compilation)
            .generatedSourceFile("result.BaseClassDecorator")
            .hasSourceEquivalentTo(generatedDecorator);
    assertThat(compilation)
            .generatedSourceFile("result.BaseClassInterface")
            .hasSourceEquivalentTo(generatedInterface);
  }

  @Test
  public void shouldCreateDecoratorOnlyIfRootIsInterface() {
    JavaFileObject sourceClass = JavaFileObjects.forSourceLines("test.BaseInterface",
            "package test;",
            "import com.highstakes.autointerface.AutoInterface;",
            "",
            "@AutoInterface(pkg = \"result\", createDecorator = true)",
            "public interface BaseInterface {",
            "    public void baseClassMethod1();",
            "}");

    JavaFileObject generatedInterface = JavaFileObjects.forSourceLines("result.BaseClassInterface",
            "package result;\n",
                    "import java.lang.Override;\n"
                    + "import test.BaseInterface;\n"
                    + "\n"
                    + "public interface BaseInterfaceDecorator extends BaseInterface {\n"
                    + "    BaseInterface getDecoratedObject();\n"
                    + "\n"
                    + "    @Override\n"
                    + "    default void baseClassMethod1() {\n"
                    + "        getDecoratedObject().baseClassMethod1();\n"
                    + "    }\n"
                    + "}"
    );

    Compilation compilation = javac().withProcessors(new AutoInterfaceProcessor())
            .compile(sourceClass);
    assertThat(compilation).succeededWithoutWarnings();
    assertThat(compilation)
            .generatedSourceFile("result.BaseInterfaceDecorator")
            .hasSourceEquivalentTo(generatedInterface);
  }
}
