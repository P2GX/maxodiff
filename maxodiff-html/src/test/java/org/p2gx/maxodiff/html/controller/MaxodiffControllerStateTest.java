package org.p2gx.maxodiff.html.controller;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MaxodiffControllerStateTest {

    /** The controller is a singleton shared by all requests, so it must not hold mutable state. */
    @Test
    void controllerHasNoMutableInstanceFields() {
        for (Field field : MaxodiffController.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            assertTrue(Modifier.isFinal(field.getModifiers()),
                    "MaxodiffController." + field.getName() + " must be final (the controller is shared by all requests)");
        }
    }
}