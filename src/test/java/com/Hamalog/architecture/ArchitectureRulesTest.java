package com.Hamalog.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * ArchUnit을 사용한 아키텍처 규칙 테스트
 *
 * 이 테스트는 프로젝트의 아키텍처 규칙을 자동으로 검증합니다:
 * - 계층 간 의존성 규칙
 * - 네이밍 컨벤션
 * - 패키지 구조 규칙
 */
@DisplayName("아키텍처 규칙 테스트")
class ArchitectureRulesTest {

    private static JavaClasses importedClasses;

    @BeforeAll
    static void setUp() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.Hamalog");
    }

    @Nested
    @DisplayName("계층형 아키텍처 규칙")
    class LayeredArchitectureTest {

        @Test
        @DisplayName("Controller는 Repository를 직접 접근하지 않아야 함")
        void controllersShouldNotAccessRepositoriesDirectly() {
            noClasses()
                    .that().resideInAPackage("..controller..")
                    .should().accessClassesThat().resideInAPackage("..repository..")
                    .because("Controller는 Service를 통해서만 데이터에 접근해야 합니다")
                    .check(importedClasses);
        }

        @Test
        @DisplayName("Service는 Controller에 의존하지 않아야 함")
        void servicesShouldNotDependOnControllers() {
            noClasses()
                    .that().resideInAPackage("..service..")
                    .should().dependOnClassesThat().resideInAPackage("..controller..")
                    .because("Service는 Controller에 의존하면 안됩니다 (순환 의존성 방지)")
                    .check(importedClasses);
        }

        @Test
        @DisplayName("Repository는 Service에 의존하지 않아야 함")
        void repositoriesShouldNotDependOnServices() {
            noClasses()
                    .that().resideInAPackage("..repository..")
                    .should().dependOnClassesThat().resideInAPackage("..service..")
                    .because("Repository는 Service에 의존하면 안됩니다")
                    .check(importedClasses);
        }
    }

    @Nested
    @DisplayName("네이밍 컨벤션 규칙")
    class NamingConventionTest {

        @Test
        @DisplayName("Controller 클래스는 'Controller' 접미사를 가져야 함")
        void controllersShouldHaveControllerSuffix() {
            classes()
                    .that().resideInAPackage("..controller..")
                    .and().areAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
                    .should().haveSimpleNameEndingWith("Controller")
                    .because("REST Controller는 'Controller' 접미사를 가져야 합니다")
                    .check(importedClasses);
        }

        @Test
        @DisplayName("Service 클래스는 'Service' 접미사를 가져야 함")
        void servicesShouldHaveServiceSuffix() {
            classes()
                    .that().resideInAPackage("..service..")
                    .and().areAnnotatedWith(org.springframework.stereotype.Service.class)
                    .should().haveSimpleNameEndingWith("Service")
                    .because("Service 클래스는 'Service' 접미사를 가져야 합니다")
                    .check(importedClasses);
        }

        @Test
        @DisplayName("Repository 인터페이스는 'Repository' 접미사를 가져야 함")
        void repositoriesShouldHaveRepositorySuffix() {
            classes()
                    .that().resideInAPackage("..repository..")
                    .and().areInterfaces()
                    .should().haveSimpleNameEndingWith("Repository")
                    .because("Repository 인터페이스는 'Repository' 접미사를 가져야 합니다")
                    .check(importedClasses);
        }
    }

    @Nested
    @DisplayName("어노테이션 규칙")
    class AnnotationRulesTest {

        @Test
        @DisplayName("Controller는 @RestController 어노테이션을 가져야 함")
        void controllersShouldBeAnnotatedWithRestController() {
            classes()
                    .that().resideInAPackage("..controller..")
                    .and().haveSimpleNameEndingWith("Controller")
                    .and().areNotInterfaces()
                    .should().beAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
                    .because("REST API Controller는 @RestController 어노테이션이 필요합니다")
                    .check(importedClasses);
        }

        @Test
        @DisplayName("Service는 @Service 또는 @Component 어노테이션을 가져야 함")
        void servicesShouldBeAnnotatedWithServiceOrComponent() {
            classes()
                    .that().resideInAPackage("..service..")
                    .and().haveSimpleNameEndingWith("Service")
                    .and().areNotInterfaces()
                    .should().beAnnotatedWith(org.springframework.stereotype.Service.class)
                    .orShould().beAnnotatedWith(org.springframework.stereotype.Component.class)
                    .because("Service 클래스는 Spring Bean으로 등록되어야 합니다")
                    .check(importedClasses);
        }
    }

    @Nested
    @DisplayName("패키지 구조 규칙")
    class PackageStructureTest {

        @Test
        @DisplayName("Entity 클래스는 domain 패키지에 존재해야 함")
        void entityClassesShouldResideInDomainPackage() {
            classes()
                    .that().areAnnotatedWith(jakarta.persistence.Entity.class)
                    .should().resideInAPackage("..domain..")
                    .because("JPA Entity는 domain 패키지에 있어야 합니다")
                    .check(importedClasses);
        }
    }
}
