package com.persistentbit.core.doc.annotations;

import java.lang.annotation.*;

/**
 * TODOC
 *
 * @author petermuys
 * @since 21/04/17
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PACKAGE})
public @interface DUsesComponents{
	DUsesComponent[] value();
}