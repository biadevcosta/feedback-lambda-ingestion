package com.feedback.ingestion;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** Guards the dependency rule: adapter → application → domain, never the other way. */
@AnalyzeClasses(packages = "com.feedback.ingestion", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule domainDependsOnlyOnJava = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideOutsideOfPackages("..domain..", "java..")
            .because("business rules must not know about AWS, JSON or Lambda");

    @ArchTest
    static final ArchRule applicationDependsOnlyOnDomain = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat()
            .resideOutsideOfPackages("..application..", "..domain..", "java..")
            .because("use cases talk to the outside world only through ports");

    @ArchTest
    static final ArchRule adaptersDoNotDependOnConfig = noClasses()
            .that().resideInAPackage("..adapter..")
            .should().dependOnClassesThat().resideInAPackage("..config..")
            .because("only the composition root wires dependencies");

    @ArchTest
    static final ArchRule inboundDoesNotUseOutbound = noClasses()
            .that().resideInAPackage("..adapter.in..")
            .should().dependOnClassesThat().resideInAPackage("..adapter.out..")
            .because("inbound adapters reach outbound adapters only through use cases and ports");

    @ArchTest
    static final ArchRule outboundDoesNotUseInbound = noClasses()
            .that().resideInAPackage("..adapter.out..")
            .should().dependOnClassesThat().resideInAPackage("..adapter.in..");
}
