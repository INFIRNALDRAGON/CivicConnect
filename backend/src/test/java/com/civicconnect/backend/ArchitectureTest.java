package com.civicconnect.backend;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleNameEndingWith;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleNameStartingWith;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.library.dependencies.SliceAssignment;
import com.tngtech.archunit.library.dependencies.SliceIdentifier;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;

class ArchitectureTest {

    private static final String ROOT = "com.civicconnect.backend";

    private static final List<String> MODULES =
            List.of("auth", "request", "assignment", "status", "audit", "notification", "reporting");

    private static final Map<String, List<String>> ALLOWED = Map.of(
            "auth", List.of("exception"),
            "request", List.of("auth", "dto", "exception"),
            "status", List.of("auth", "exception", "request"),
            "audit", List.of("request", "status"),
            "assignment", List.of("auth", "exception", "request", "status"),
            "notification", List.of("auth", "exception", "request", "status"),
            "reporting", List.of("auth", "exception", "request"));

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(ROOT);
    }

    @Test
    void modulesDoNotDependOnEachOtherInACircle() {
        SliceAssignment byModule = new SliceAssignment() {
            @Override
            public SliceIdentifier getIdentifierOf(JavaClass javaClass) {
                for (String module : MODULES) {
                    if (javaClass.getPackageName().equals(ROOT + "." + module)
                            || javaClass.getPackageName().startsWith(ROOT + "." + module + ".")) {
                        return SliceIdentifier.of(module);
                    }
                }
                return SliceIdentifier.ignore();
            }

            @Override
            public String getDescription() {
                return "module";
            }
        };
        SlicesRuleDefinition.slices().assignedFrom(byModule).should().beFreeOfCycles().check(classes);
    }

    @Test
    void eachModuleOnlyUsesTheModulesItIsAllowedToUse() {
        for (String module : MODULES) {
            List<String> forbidden = MODULES.stream()
                    .filter(other -> !other.equals(module) && !ALLOWED.get(module).contains(other))
                    .map(other -> ROOT + "." + other + "..")
                    .toList();
            List<String> forbiddenPackages = new java.util.ArrayList<>(forbidden);
            forbiddenPackages.add(ROOT + ".controller..");
            noClasses().that().resideInAPackage(ROOT + "." + module + "..")
                    .should().dependOnClassesThat().resideInAnyPackage(forbiddenPackages.toArray(new String[0]))
                    .because("module " + module + " may only use " + ALLOWED.get(module))
                    .check(classes);
        }
    }

    @Test
    void nothingOutsideTheControllersDependsOnTheControllers() {
        noClasses().that().resideOutsideOfPackage(ROOT + ".controller..")
                .should().dependOnClassesThat().resideInAPackage(ROOT + ".controller..")
                .check(classes);
    }

    @Test
    void persistenceClassesStayInsideTheirOwnModule() {
        DescribedPredicate<JavaClass> persistence = simpleNameEndingWith("Entity")
                .or(simpleNameEndingWith("Repository"))
                .or(simpleNameStartingWith("Jpa"));
        for (String module : MODULES) {
            noClasses().that().resideOutsideOfPackage(ROOT + "." + module + "..")
                    .should().dependOnClassesThat(resideInAPackage(ROOT + "." + module + "..").and(persistence))
                    .because("only the " + module + " module may touch its tables")
                    .check(classes);
        }
    }
}
