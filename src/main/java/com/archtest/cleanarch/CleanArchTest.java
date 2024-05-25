package com.archtest.cleanarch;

import java.util.AbstractMap;
import java.util.LinkedList;
import java.util.List;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

public class CleanArchTest {
  private final JavaClasses classes;
  private final String pathEnterpriseBusiness;
  private final String pathApplicationBusiness;
  private final String pathInterfaceAdaptersController;
  private final String pathInterfaceAdaptersPresenter;
  private final String pathInterfaceAdaptersInfra;
  private final String pathCommunicationCoreWithAdapters;
  private final List<String> acceptedEntityDependencies;
  private static final String ENTERPRISE_BUSINESS_LAYER = "enterpriseBusinessLayer";
  private static final String APPLICATION_BUSINESS_LAYER = "applicationBusinessLayer";
  private static final String INTERFACE_ADAPTERS_LAYER = "interfaceAdaptersLayer";

  public CleanArchTest(final Paths paths, final List<String> acceptedEntityDependencies) {
    this.classes = new ClassFileImporter().importPackages(paths.mainProject());
    this.pathEnterpriseBusiness = paths.enterpriseBusiness();
    this.pathApplicationBusiness = paths.applicationBusiness();
    this.pathInterfaceAdaptersController = paths.interfaceAdaptersController();
    this.pathInterfaceAdaptersPresenter = paths.interfaceAdaptersPresenter();
    this.pathInterfaceAdaptersInfra = paths.interfaceAdaptersInfra();
    this.pathCommunicationCoreWithAdapters = paths.communicationCoreWithAdapters();
    this.acceptedEntityDependencies = acceptedEntityDependencies;
  }

  public void check() {
    this.layersAreRespected().check(classes);
    this.runEntityRules();
    this.runUseCaseRules();
    this.runOtherRules();
  }

  private void runEntityRules() {
    this.entityDoesNotDependOnAnyone().check(classes);
    this.privateEntityConstructor().check(classes);
  }

  private void runUseCaseRules() {
    this.useCasesNotCallOtherUseCases().check(classes);
    this.requestAppBusinessUsedByOnlyOneUseCase().check(classes);
    this.responseAppBusinessUsedByOnlyOneUseCase().check(classes);
    this.communicationWithExternalThroughInterface().check(classes);
  }

  private void runOtherRules() {
    this.requestObjectsWithCorrectName().check(classes);
    this.responseObjectsWithCorrectName().check(classes);
    this.requestsAndResponseIsRecords().check(classes);
  }

  private ArchRule layersAreRespected() {

    return layeredArchitecture()
        .consideringOnlyDependenciesInLayers()
        .layer(ENTERPRISE_BUSINESS_LAYER)
        .definedBy(this.pathEnterpriseBusiness)
        .layer(APPLICATION_BUSINESS_LAYER)
        .definedBy(this.pathApplicationBusiness)
        .layer(INTERFACE_ADAPTERS_LAYER)
        .definedBy(
            this.pathInterfaceAdaptersController,
            this.pathInterfaceAdaptersInfra,
            this.pathInterfaceAdaptersPresenter)
        .whereLayer(INTERFACE_ADAPTERS_LAYER)
        .mayNotBeAccessedByAnyLayer()
        .whereLayer(INTERFACE_ADAPTERS_LAYER)
        .mayOnlyAccessLayers(APPLICATION_BUSINESS_LAYER)
        .whereLayer(APPLICATION_BUSINESS_LAYER)
        .mayOnlyBeAccessedByLayers(INTERFACE_ADAPTERS_LAYER)
        .whereLayer(APPLICATION_BUSINESS_LAYER)
        .mayOnlyAccessLayers(ENTERPRISE_BUSINESS_LAYER)
        .whereLayer(ENTERPRISE_BUSINESS_LAYER)
        .mayOnlyBeAccessedByLayers(APPLICATION_BUSINESS_LAYER)
        .whereLayer(ENTERPRISE_BUSINESS_LAYER)
        .mayNotAccessAnyLayer()
        .as("The layers of Clean Architecture should be respected.")
        .allowEmptyShould(true);
  }

  private ArchRule entityDoesNotDependOnAnyone() {
    final var acceptedPackages = new LinkedList<>(List.of("java..", this.pathEnterpriseBusiness));
    acceptedPackages.addAll(this.acceptedEntityDependencies);

    return classes()
        .that()
        .resideInAPackage(this.pathEnterpriseBusiness)
        .should()
        .onlyDependOnClassesThat()
        .resideInAnyPackage(acceptedPackages.toArray(new String[0]))
        .as("The entity must not depend on any lib or framework.")
        .allowEmptyShould(true);
  }

  private ArchRule privateEntityConstructor() {
    return classes()
        .that()
        .resideInAPackage(this.pathEnterpriseBusiness)
        .and()
        .resideOutsideOfPackage("..exception..")
        .should()
        .haveOnlyPrivateConstructors()
        .because("Entity should not have public constructor.")
        .allowEmptyShould(true);
  }

  private ArchRule useCasesNotCallOtherUseCases() {
    return classes()
        .that()
        .resideInAPackage(this.pathApplicationBusiness)
        .should(this.buildUseCaseNotCallOtherUseCasesRule());
  }

  private ArchRule requestAppBusinessUsedByOnlyOneUseCase() {
    return classes()
        .that()
        .resideInAnyPackage(this.pathApplicationBusiness)
        .and()
        .resideInAPackage("..request..")
        .should(this.buildContractRuleBeingUsedOnlyOneUseCase(".request"));
  }

  private ArchRule responseAppBusinessUsedByOnlyOneUseCase() {
    return classes()
        .that()
        .resideInAnyPackage(this.pathApplicationBusiness)
        .and()
        .resideInAPackage("..response..")
        .should(this.buildContractRuleBeingUsedOnlyOneUseCase(".response"));
  }

  private ArchRule requestObjectsWithCorrectName() {
    return classes()
        .that()
        .resideInAPackage("..request..")
        .and()
        .resideOutsideOfPackage("..enums..")
        .should()
        .haveSimpleNameEndingWith("Request")
        .as("Request objects should have a name ending with 'Request'.")
        .allowEmptyShould(true);
  }

  private ArchRule responseObjectsWithCorrectName() {
    return classes()
        .that()
        .resideInAPackage("..response..")
        .and()
        .resideOutsideOfPackage("..enums..")
        .should()
        .haveSimpleNameEndingWith("Response")
        .as("Response objects should have a name ending with 'Response'.")
        .allowEmptyShould(true);
  }

  private ArchCondition<JavaClass> buildUseCaseNotCallOtherUseCasesRule() {
    return new ArchCondition<>("not call other use cases") {
      @Override
      public void check(JavaClass javaClass, ConditionEvents events) {
        javaClass.getDirectDependenciesFromSelf().stream()
            .filter(it -> !it.getTargetClass().getPackageName().equals("java.lang"))
            .filter(CleanArchTest::isNotResponseRequestOrException)
            .map(
                it ->
                    new AbstractMap.SimpleEntry<>(it.getTargetClass(), it.getSourceCodeLocation()))
            .forEach(
                it -> {
                  final var dependency = it.getKey();
                  final var sourceCodeLocation = it.getValue();

                  String message =
                      String.format(
                          "Class %s calls use case %s in %s",
                          javaClass.getName(), dependency.getName(), sourceCodeLocation);
                  events.add(SimpleConditionEvent.violated(dependency, message));
                });
      }
    };
  }

  private ArchCondition<JavaClass> buildContractRuleBeingUsedOnlyOneUseCase(
      final String contractType) {
    return new ArchCondition<>("be used for only one use case") {
      @Override
      public void check(JavaClass javaClass, ConditionEvents events) {
        final var packageOrigin = javaClass.getPackageName();
        final var originWithoutContractType =
            packageOrigin.substring(0, packageOrigin.length() - contractType.length());

        final var usages =
            javaClass.getDirectDependenciesToSelf().stream()
                .filter(
                    it ->
                        it.getOriginClass().getPackageName().startsWith(originWithoutContractType))
                .map(Dependency::getSourceCodeLocation)
                .toList();

        if (usages.size() != 1) {
          String message =
              String.format(
                  "%s %s is used in use cases: %s", contractType, javaClass.getName(), usages);

          events.add(SimpleConditionEvent.violated(javaClass, message));
        }
      }
    };
  }

  private static boolean isNotResponseRequestOrException(final Dependency dependency) {
    return !(dependency.getTargetClass().getPackageName().endsWith("response")
        || dependency.getTargetClass().getPackageName().endsWith("request")
        || dependency.getTargetClass().getPackageName().endsWith("exception"));
  }

  private ArchRule communicationWithExternalThroughInterface() {
    return classes()
        .that()
        .resideInAPackage(this.pathCommunicationCoreWithAdapters)
        .should()
        .beInterfaces()
        .allowEmptyShould(true);
  }

  private ArchRule requestsAndResponseIsRecords() {
    return classes()
        .that()
        .resideInAnyPackage("..request..")
        .or()
        .resideInAnyPackage("..response..")
        .should()
        .beRecords()
        .allowEmptyShould(true);
  }
}
