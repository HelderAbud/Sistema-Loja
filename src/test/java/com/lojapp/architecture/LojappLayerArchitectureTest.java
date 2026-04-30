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

    @ArchTest
    static final ArchRule services_do_not_depend_on_controllers =
            noClasses()
                    .that()
                    .resideInAPackage("..service..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("..controller..")
                    .because("Serviços não devem conhecer a camada web");

    @ArchTest
    static final ArchRule application_use_cases_do_not_depend_on_controllers =
            noClasses()
                    .that()
                    .resideInAPackage("..application..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("..controller..")
                    .because("Use cases da aplicação devem ser independentes de adapters HTTP");

    @ArchTest
    static final ArchRule repositories_do_not_depend_on_services_or_controllers =
            noClasses()
                    .that()
                    .resideInAPackage("..repository..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage("..service..", "..controller..")
                    .because("Repositórios representam acesso a dados e não orquestram regra de negócio");

    @ArchTest
    static final ArchRule controllers_use_service_and_application_contracts_only =
            noClasses()
                    .that()
                    .resideInAPackage("..controller..")
                    .should()
                    .dependOnClassesThat()
                    .haveFullyQualifiedName("com.lojapp.service.DashboardService")
                    .orShould()
                    .dependOnClassesThat()
                    .haveFullyQualifiedName("com.lojapp.service.LojappCatalogService")
                    .orShould()
                    .dependOnClassesThat()
                    .haveFullyQualifiedName("com.lojapp.service.LojappHierarchyService")
                    .orShould()
                    .dependOnClassesThat()
                    .haveFullyQualifiedName("com.lojapp.application.sale.CreatePosSaleUseCase")
                    .orShould()
                    .dependOnClassesThat()
                    .haveFullyQualifiedName("com.lojapp.application.cash.OpenCashSessionUseCase")
                    .orShould()
                    .dependOnClassesThat()
                    .haveFullyQualifiedName("com.lojapp.application.cash.CloseCashSessionUseCase")
                    .orShould()
                    .dependOnClassesThat()
                    .haveFullyQualifiedName("com.lojapp.application.cash.GetCurrentCashSessionUseCase")
                    .orShould()
                    .dependOnClassesThat()
                    .haveFullyQualifiedName("com.lojapp.application.cash.GetCashSessionClosePreviewUseCase")
                    .orShould()
                    .dependOnClassesThat()
                    .haveFullyQualifiedName("com.lojapp.application.nfe.ImportNfeUseCase")
                    .orShould()
                    .dependOnClassesThat()
                    .haveFullyQualifiedName(
                            "com.lojapp.application.nfe.ApplyNfeImportSuggestionsUseCase")
                    .because(
                            "Controllers padronizados devem depender de contratos e não de classes concretas de serviço/caso de uso");
}
