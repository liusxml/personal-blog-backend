package com.blog.architecture;

import com.blog.architecture.rules.*;
import org.junit.jupiter.api.Test;

class AllArchitectureTests {

    @Test
    void validate_architecture() {
        LayerRule.check(ArchitectureTest.CLASSES);
        ModuleRule.check();
        NamingRule.check();
        DesignPatternRule.check();
    }
}