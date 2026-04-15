# Copilot instructions for `idpay-payment`

Use this file as the repository source of truth for everyday coding work. Keep changes repo-specific, follow existing patterns before inventing new ones, and prefer updating the smallest surface that fully solves the task.

## Stack and validation commands

- The service runs on Spring Boot `4.0.2` and Java `25`.
- Build without tests: `mvn clean package -DskipTests`
- Run the full test suite: `mvn test`
- Run one test class: `mvn -Dtest=QRCodePaymentControllerTest test`
- Run one test method: `mvn -Dtest=QRCodePaymentControllerTest#testMethodName test`
- CI quality flow is defined in `.github/workflows/code-review.yml` and uses:
  - `mvn clean org.jacoco:jacoco-maven-plugin:0.8.14:prepare-agent verify org.jacoco:jacoco-maven-plugin:0.8.14:report org.jacoco:jacoco-maven-plugin:0.8.14:report-aggregate -B`
  - `mvn org.jacoco:jacoco-maven-plugin:0.8.14:report -Djacoco.reportFormat=xml -B`
  - `org.sonarsource.scanner.maven:sonar-maven-plugin:sonar`
- No separate lint or format target is configured in `pom.xml`; validate changes with the relevant Maven test or verify command.

## Deployment and chart commands

- Render or test the Helm chart: `helm dep build && helm template . -f values-dev.yaml --debug`
- Deploy the Helm chart: `helm dep build && helm upgrade --namespace idpay --install --values values-dev.yaml --wait --timeout 5m0s idpay-payment .`

## Architecture map

- `src/main/java/it/gov/pagopa/payment/PaymentApplication.java` boots the service and scans `it.gov.pagopa`.
- The HTTP layer is split between interfaces and implementation classes:
  - request mappings live in `controller/**/*Controller.java`
  - executable Spring MVC classes live in `controller/**/*ControllerImpl.java`
- Payment flows are organized by channel under `src/main/java/it/gov/pagopa/payment/service/payment/`:
  - `qrcode`
  - `barcode`
  - `idpaycode`
  - `common`
  - `expired`
- Channel services are orchestration layers that delegate to smaller, single-purpose services. Reuse the existing split before adding new coordination logic.
- Persistence centers on MongoDB `TransactionInProgress` documents. Custom Mongo behavior lives alongside Spring Data repositories, including `TransactionInProgressRepositoryExtImpl`.
- Messaging is function-based Spring Cloud Stream:
  - inbound consumers live in `payment/event/consumer/`
  - outbound transaction events are published by `connector/event/trx/TransactionNotifierServiceImpl`
  - timeout scheduling uses Azure Service Bus via `MessageSchedulerConfig` and `service/messagescheduler/MessageSchedulerServiceImpl`
- External integrations are split by concern:
  - Feign clients in `connector/rest/`
  - Azure Blob storage in `configuration/FileStorageConfig.java`
  - PDF generation in `service/pdf/PdfServiceImpl.java`
- Async contracts are documented in `specs/asyncapi.yml`.

## Cross-cutting repository conventions

- Follow the repository preference for interface plus `Impl` pairs across controllers, services, connectors, and repositories.
- When changing an endpoint, check both the controller interface and its `*Impl` class. The mapping is usually declared on the interface, while collaborators, annotations, and runtime behavior live in the implementation.
- `@PerformanceLog` is a core convention on public flows. Reuse the payload builders under `service/performancelogger/` before adding new logging payload logic.
- Sanitize logging-sensitive identifiers with `Utilities.sanitizeString(...)` before writing transaction IDs, merchant IDs, point-of-sale IDs, and similar values to logs.
- Mongo retry behavior is implemented with AOP:
  - repository calls are wrapped by `MongoRequestRateTooLargeAutomaticRetryAspect`
  - selected APIs opt in with `@MongoRequestRateTooLargeApiRetryable` or `@MongoRequestRateTooLargeRetryable`
- Prefer Lombok-generated `Fields` constants from `@FieldNameConstants` models, such as `TransactionInProgress.Fields`, over raw Mongo field names.
- Stream bindings are not defined only in Java. If you add or rename a producer or consumer, update both the code and `src/main/resources/application.yml`.

## Change-impact checklist

- Endpoint changes usually require reviewing the controller interface, controller implementation, service orchestration, request or response models, and controller tests.
- Messaging changes usually require reviewing producers or consumers, `application.yml` function and binding configuration, and `specs/asyncapi.yml`.
- Mongo query or state-transition changes usually require reviewing repository extensions, retry annotations, `TransactionInProgress` mappings, and integration tests.
- Connector changes usually require reviewing Feign configuration, boundary error handling, and WireMock-based tests.
- Public-flow changes should be checked for `@PerformanceLog`, event publication, timeout scheduling, and log sanitization.

## Testing and specialized agents

- Prefer the narrowest effective test scope:
  - pure unit tests for orchestration or decision logic
  - `@WebMvcTest(...ControllerImpl.class)` for request mapping, validation, and HTTP error mapping
  - `@MongoTest` for repository and Mongo behavior
  - `BaseWireMockTest` for REST connector behavior
- Reuse existing test fakers and helpers under `src/test/java/it/gov/pagopa/payment/test/fakers/` before creating new fixtures.
- Use the specialized testing agent in `.github/agents/spring-testing-agent.md` when the task is primarily about adding, repairing, or choosing tests.
- Use `.github/agents/tdd-refactor-agent.md` only for small, test-protected refactors after confirming its guidance fits this Java/Spring repository and not a different stack.

## Working norms for coding agents

- Prefer repository truth over generic framework advice.
- Search for nearby examples in the same package and mirror local structure, imports, and naming.
- Make precise, surgical changes; do not refactor unrelated code as part of a feature or bug fix.
- Update documentation or contracts only when the code change directly affects them.
- Preserve observable behavior unless the requested change explicitly alters it.
