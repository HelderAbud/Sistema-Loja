package com.lojapp.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(
        packages = "com.lojapp",
        importOptions = ImportOption.DoNotIncludeTests.class)
class LojappLayerArchitectureTest {

    @ArchTest
    static final ArchRule controllers_do_not_touch_repositories =
            noClasses()
                    .that()
                    .resideInAPackage("..controller..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("..repository..")
                    .because("Controllers delegam a serviços / casos de uso, não a repositórios JPA");
}
