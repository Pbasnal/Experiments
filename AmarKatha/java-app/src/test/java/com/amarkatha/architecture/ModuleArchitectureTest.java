package com.amarkatha.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ModuleArchitectureTest {

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.amarkatha");
    }

    @Test
    void identityMayOnlyDependOnSharedAndBootstrap() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.amarkatha.identity..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "com.amarkatha.publishing..",
                        "com.amarkatha.catalog..",
                        "com.amarkatha.reader..",
                        "com.amarkatha.admin..",
                        "com.amarkatha.media..",
                        "com.amarkatha.analytics..",
                        "com.amarkatha.scheduling..",
                        "com.amarkatha.engagement..",
                        "com.amarkatha.payments..",
                        "com.amarkatha.outbox..")
                .because("identity depends only on shared per architecture.md");
        rule.check(classes);
    }

    @Test
    void publishingMayOnlyDependOnAllowedModules() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.amarkatha.publishing..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "com.amarkatha.identity..",
                        "com.amarkatha.catalog..",
                        "com.amarkatha.reader..",
                        "com.amarkatha.admin..",
                        "com.amarkatha.analytics..",
                        "com.amarkatha.engagement..",
                        "com.amarkatha.payments..",
                        "com.amarkatha.bootstrap..")
                .because("publishing depends on shared, scheduling, media, outbox — not engagement");
        rule.check(classes);
    }

    @Test
    void outboxMayOnlyDependOnShared() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.amarkatha.outbox..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "com.amarkatha.identity..",
                        "com.amarkatha.publishing..",
                        "com.amarkatha.catalog..",
                        "com.amarkatha.reader..",
                        "com.amarkatha.admin..",
                        "com.amarkatha.media..",
                        "com.amarkatha.analytics..",
                        "com.amarkatha.scheduling..",
                        "com.amarkatha.engagement..",
                        "com.amarkatha.payments..",
                        "com.amarkatha.business..",
                        "com.amarkatha.bootstrap..")
                .because("outbox is infrastructure — shared/Spring only");
        rule.check(classes);
    }

    @Test
    void schedulingMayOnlyDependOnShared() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.amarkatha.scheduling..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "com.amarkatha.identity..",
                        "com.amarkatha.publishing..",
                        "com.amarkatha.catalog..",
                        "com.amarkatha.reader..",
                        "com.amarkatha.admin..",
                        "com.amarkatha.media..",
                        "com.amarkatha.analytics..",
                        "com.amarkatha.engagement..",
                        "com.amarkatha.payments..",
                        "com.amarkatha.business..",
                        "com.amarkatha.bootstrap..",
                        "com.amarkatha.outbox..")
                .because("scheduling is pure domain — shared only");
        rule.check(classes);
    }

    @Test
    void businessMayOnlyDependOnShared() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.amarkatha.business..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "com.amarkatha.identity..",
                        "com.amarkatha.publishing..",
                        "com.amarkatha.catalog..",
                        "com.amarkatha.reader..",
                        "com.amarkatha.admin..",
                        "com.amarkatha.media..",
                        "com.amarkatha.analytics..",
                        "com.amarkatha.scheduling..",
                        "com.amarkatha.engagement..",
                        "com.amarkatha.payments..",
                        "com.amarkatha.bootstrap..",
                        "com.amarkatha.outbox..")
                .because("business is pure domain — shared only");
        rule.check(classes);
    }

    @Test
    void readerMayNotDependOnAdminOrPayments() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.amarkatha.reader..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "com.amarkatha.admin..",
                        "com.amarkatha.payments..",
                        "com.amarkatha.identity..",
                        "com.amarkatha.media..",
                        "com.amarkatha.engagement..",
                        "com.amarkatha.outbox..")
                .because("reader depends on shared, catalog, publishing, scheduling, analytics, business");
        rule.check(classes);
    }

    @Test
    void engagementMayOnlyDependOnAllowedModules() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.amarkatha.engagement..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "com.amarkatha.reader..",
                        "com.amarkatha.admin..",
                        "com.amarkatha.media..",
                        "com.amarkatha.catalog..",
                        "com.amarkatha.payments..",
                        "com.amarkatha.bootstrap..")
                .because("engagement depends on shared, publishing, identity, outbox, analytics, business");
        rule.check(classes);
    }
}
