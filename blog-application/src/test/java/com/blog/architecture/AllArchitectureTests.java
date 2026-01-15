package com.blog.architecture;

import com.blog.architecture.rules.DesignPatternRule;
import com.blog.architecture.rules.LayerRule;
import com.blog.architecture.rules.ModuleRule;
import com.blog.architecture.rules.NamingRule;
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