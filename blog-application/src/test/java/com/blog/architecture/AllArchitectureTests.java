package com.blog.architecture;

import com.blog.architecture.rules.ApiRule;
import com.blog.architecture.rules.DesignPatternRule;
import com.blog.architecture.rules.LayerRule;
import com.blog.architecture.rules.ModuleRule;
import com.blog.architecture.rules.NamingRule;
import com.blog.architecture.rules.PlantUMLRule;
import org.junit.jupiter.api.Test;

class AllArchitectureTests {

    @Test
    void validateArchitecture() {
        LayerRule.check(ArchitectureTest.CLASSES);
        ModuleRule.check();
        NamingRule.check();
        DesignPatternRule.check();
        ApiRule.check();
        PlantUMLRule.check();
    }
}