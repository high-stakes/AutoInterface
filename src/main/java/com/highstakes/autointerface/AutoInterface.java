package com.highstakes.autointerface;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Automatically generate interface for class files
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE})
public @interface AutoInterface {
  String value() default "";

  /**
   * The generated interface's name. Leave default for autogeneration.
   */
  String name() default "";

  /**
   * The generated interface's package. Leave default for the same package as
   * the class file
   */
  String pkg() default "";

  /**
   * Whether to include inherited methods of the class in the interface. Defaults to false.
   */
  boolean includeInherted() default false;

  boolean createDecorator() default false;

  String decoratorName() default "";
}