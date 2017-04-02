package com.persistentbit.core.doc;

import java.lang.annotation.*;

@Documented
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface UsedByPeople {

    UsedByPerson[] value();

}
