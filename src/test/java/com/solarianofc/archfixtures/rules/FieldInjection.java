package com.solarianofc.archfixtures.rules;

import org.springframework.beans.factory.annotation.Autowired;

/** Fixture for the "no field injection" rule (D-116): deliberately injects into a field. */
public class FieldInjection {

    @Autowired
    private Object dependency = new Object();

    public Object getDependency() {
        return dependency;
    }
}
