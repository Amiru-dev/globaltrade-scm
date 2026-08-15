# Technical Implementation Report: GlobalTrade SCM System

## 1. System Architecture Overview
The GlobalTrade Supply Chain Management (SCM) system is an enterprise-grade logistics platform developed using Jakarta EE 10. The system employs a multi-tiered architecture to ensure separation of concerns, scalability, and maintainability.

### Module Structure
The application is structured into a multi-module Maven project packaged as an Enterprise Archive (EAR), consisting of:
- **Common Module (JAR):** Contains shared domain entities, Data Transfer Objects (DTOs), enumerations, and a custom exception hierarchy. This module acts as the foundation, ensuring consistency across tiers.
- **EJB Module (JAR):** The core business tier encapsulating session beans (Stateless, Stateful, Singleton), timer services for scheduled tasks, interceptors for cross-cutting concerns, and container-managed transaction logic.
- **Web Module (WAR):** The presentation and integration tier exposing JAX-RS RESTful web services. It manages HTTP requests, coordinates with the EJB layer, and enforces URL-based security constraints.

### Deployment Environment
The system is targeted for deployment on Payara Server (Community Edition), leveraging its robust implementation of Jakarta EE specifications and embedded EclipseLink JPA provider. Data persistence is managed via MySQL 8+, integrated through a JNDI datasource.

---

## 2. Timer Services Design and Analysis
The logistics domain requires numerous scheduled and reactive automated processes. The system leverages EJB Timer Services to orchestrate these tasks without relying on external scheduling frameworks like Quartz.

### Programmatic vs. Declarative Timers
The system demonstrates both approaches based on business requirements:

#### Programmatic Timers (`ShipmentTrackingTimer`, `RouteOptimizationTimer`)
Programmatic timers are instantiated via the `TimerService` interface. They offer dynamic scheduling capabilities, essential for operations where intervals may change based on external factors.
- **Shipment Tracking:** Created at startup using `createIntervalTimer()`. The programmatic approach allows the interval to be dynamically adjusted based on configuration (e.g., polling every 5 minutes during peak holiday seasons and every 15 minutes otherwise). It is configured as `persistent=true` to ensure tracking continues reliably across server restarts.
- **Route Optimization:** Implemented as a single-action timer (`createSingleActionTimer()`). Route recalculation is computationally expensive and is triggered reactively by cost change events. The programmatic single-action timer allows for "debouncing"—delaying the execution slightly to batch multiple rapid cost changes into a single calculation. It is configured as `persistent=false` because if the server crashes, recalculating obsolete events is unnecessary; the next event will trigger a fresh optimization.

#### Declarative Timers (`InventoryMonitorTimer`, `VendorPerformanceTimer`)
Declarative timers use the `@Schedule` annotation for fixed, predictable rhythms.
- **Vendor Performance:** Scheduled to run daily at 02:00 AM (`@Schedule(hour = "2", minute = "0")`). This is a batch process that calculates ratings based on historical delivery data. The declarative approach reduces boilerplate code and clearly defines the execution intent in the metadata.

### Critical Analysis of Timer Services
- **Clustering and Concurrency:** In a clustered Payara environment, persistent timers are synchronized across nodes to prevent duplicate executions. This is vital to ensure that a single shipment status is not updated concurrently by multiple nodes, which could lead to database deadlocks or conflicting audit logs.
- **Transactionality:** Timer callback methods (`@Timeout`) operate within a transaction by default. Long-running timer tasks (like global vendor performance calculations) should ideally use `TransactionAttributeType.REQUIRES_NEW` or `NOT_SUPPORTED` with chunk-based processing to avoid holding database locks for extended periods and exhausting connection pools.

---

## 3. Interceptor Framework Architecture
The system employs an interceptor chain to handle cross-cutting concerns cleanly, removing infrastructural code from business logic.

### Interceptor Chain Design
The interceptors are orchestrated using `@Priority` to ensure a strict execution order:
1. **SecurityAuditInterceptor:** Logs security-related events (authentication, authorization checks). Must execute first to detect unauthorized access attempts before any business logic is processed.
2. **ComplianceCheckInterceptor:** Validates regulatory compliance (e.g., verifying customs documentation). Executes early to short-circuit invalid requests.
3. **AuditLogInterceptor:** Captures a comprehensive audit trail of business operations (user, action, parameters, timestamps).
4. **PerformanceMonitor:** Measures method execution times to identify bottlenecks.
5. **VendorDataValidator:** A method-level interceptor that performs deep validation on specific entities before persistence.

### Implementation Strategies
- **XML vs. Annotations:** The system demonstrates both approaches. Method-level interceptors (`@Interceptors(VendorDataValidator.class)`) are used for targeted validation. Class-level default interceptors (defined in `ejb-jar.xml`) are used for universal concerns like `PerformanceMonitor` and `AuditLogInterceptor`. Using XML allows administrators to enable or disable global monitoring without recompiling the application.
- **Contextual Data:** Interceptors leverage the `InvocationContext` to inspect method parameters, modify arguments, or halt execution by throwing exceptions.

### Critical Analysis of Interceptors
- **Performance Overhead:** Each interceptor in the chain adds a small overhead due to reflection and context management. While negligible for individual calls (~0.1ms), deep chains on highly trafficked methods (e.g., `ShipmentService.findById`) can compound. The `PerformanceMonitor` is crucial for detecting when the interceptor chain itself becomes a bottleneck.
- **Transaction Boundaries:** Interceptors execute within the same transaction context as the target method unless configured otherwise. If the `AuditLogInterceptor` writes to the database and the target method fails, the audit log will be rolled back. To ensure audit logs persist even upon failure, the audit write operation is delegated to a separate bean method annotated with `REQUIRES_NEW`.

---

## 4. Transaction Management Strategy
Robust transaction management is critical in SCM to prevent data corruption (e.g., lost inventory, orphaned orders).

### Container-Managed Transactions (CMT)
The system primarily relies on CMT, utilizing specific attributes tailored to the operation:
- **`REQUIRED`:** The default and most common attribute. Used in `InventoryService.transferInventory` to guarantee that stock deduction from the source warehouse and addition to the target warehouse execute atomically.
- **`REQUIRES_NEW`:** Used in `AuditLogInterceptor` (via a delegated service) to guarantee that audit records are committed independently of the main business transaction's success or failure.
- **`SUPPORTS`:** Used extensively for read-only operations (e.g., `ShipmentService.findActiveShipments`). It joins an existing transaction if present but does not incur the overhead of starting a new one, improving read throughput.

### Bean-Managed Transactions (BMT)
While CMT handles 95% of use cases, the `BeanManagedTxService` demonstrates BMT using `UserTransaction`. BMT is justified in complex, long-running workflows that span multiple external systems (e.g., orchestrating an order with an external carrier API and a legacy customs system). It allows for manual commit/rollback control, enabling "compensating transactions" if an external API call fails after a local database commit.

### Critical Analysis of Transaction Management
- **Isolation Levels:** By default, the system uses the database's default isolation level (usually `READ_COMMITTED`). However, for critical inventory deductions during high-demand periods, `SERIALIZABLE` isolation might be required to prevent lost updates, though this significantly impacts concurrency. Optimistic locking (via JPA `@Version`) is a better, more scalable alternative for inventory management in this scenario.
- **Rollback Rules:** The custom exception hierarchy dictates rollback behaviour. System exceptions (e.g., `EJBException`) and `CustomsComplianceException(rollback=true)` trigger automatic rollbacks to protect data integrity. Conversely, `InventoryShortageException(rollback=false)` allows the transaction to commit, enabling the system to process a partial order fulfilment rather than failing the entire transaction.

---

## 5. Security Architecture
Global logistics systems handle highly sensitive data, requiring a multi-layered security approach.

### Layered Defense
1. **Web Layer (Declarative):** The `web.xml` defines URL-based constraints (e.g., `/api/admin/*` restricted to `SYSTEM_ADMIN`). This prevents unauthorized access at the network boundary.
2. **EJB Layer (Declarative):** The `@RolesAllowed` annotation on session beans ensures that even if the web tier is bypassed (e.g., via internal RMI/IIOP calls), the container enforces authorization.
3. **Programmatic Security:** Implemented in `SecurityService.java` using `SessionContext.isCallerInRole()`.

### Custom JAAS Login Module
The `GlobalTradeLoginModule` implements Java Authentication and Authorization Service (JAAS).
- **Authentication:** Validates credentials against a MySQL database using a constant-time comparison to mitigate timing attacks.
- **Password Hashing:** Uses `SHA-256` for password hashing to maintain compatibility with the default Payara `JDBCRealm` configuration without requiring external dependencies or complex data structures for salting.

### Critical Analysis of Security
- **Declarative vs. Programmatic Limitation:** Declarative security (`@RolesAllowed`) is static and insufficient for multi-tenant or contextual authorization. For example, a `VENDOR_REPRESENTATIVE` must only be allowed to view their *own* shipments. The programmatic `SecurityService` bridges this gap by combining role checks with data-driven logic (verifying the requested vendor ID matches the authenticated principal's associated vendor ID).
- **Emergency Overrides:** The programmatic model enables "break-glass" scenarios. During a logistics crisis (e.g., port strike), a `SYSTEM_ADMIN` can use the `authorizeEmergencyAccess` method to bypass normal role restrictions, with the action heavily audited.

---

## 6. Exception Handling and Resilience
The exception hierarchy differentiates between recoverable business errors and unrecoverable infrastructure failures.

### Hierarchy Design
- **Base Application Exception:** `SupplyChainException` annotated with `@ApplicationException`. It serves as the root for all business logic errors.
- **Rollback Semantics:**
    - `TransactionRollbackException (rollback=true)`: Used when data integrity is compromised.
    - `InventoryShortageException (rollback=false)`: Allows the client to handle the shortage (e.g., by initiating a backorder) without discarding the current transaction context.
- **System Exceptions:** `CarrierSystemException` extends `EJBException` to signal severe infrastructure failures, triggering immediate rollback and bean instance destruction.

### Critical Analysis of Exception Handling
- **Performance Impact:** Constructing exceptions in Java is expensive due to the native `fillInStackTrace()` method. For high-frequency, expected business scenarios (e.g., item not found), returning `null` or an `Optional` is more performant than throwing an exception. The exception hierarchy is strictly reserved for genuine anomalous events.
- **Client Propagation:** Application exceptions are serialized and propagated to the client (or mapped to HTTP 4xx/5xx status codes in the JAX-RS layer). System exceptions are wrapped in `EJBException`, masking underlying infrastructure details (like SQL syntax errors) from the client to prevent information disclosure vulnerabilities.

---

## 7. Deployment Architecture
The application is packaged as an Enterprise Archive (EAR) utilizing a split-directory structure.

### Packaging Strategy
- **`globaltrade-scm-common.jar`:** Placed in the EAR's `/lib` directory. Classes here (Entities, DTOs) are loaded by the EAR classloader and are visible to all other modules.
- **`globaltrade-scm-ejb.jar`:** Contains the business logic. Loaded by the EJB classloader.
- **`globaltrade-scm-web.war`:** Contains REST endpoints. Loaded by the Web classloader.

### Critical Analysis of Deployment
- **Classloader Isolation:** The hierarchical classloader model ensures that the Web module can see EJB and Common classes, but the EJB module cannot see Web classes. This architectural constraint prevents developers from accidentally coupling business logic to HTTP-specific APIs (like `HttpServletRequest`).
- **Module Independence:** The EAR structure allows for independent module redeployment. In a production environment, the frontend UI or REST API (WAR) can be updated to fix a presentation bug without interrupting the core EJB services or long-running transactions, enhancing system availability.

---

## References
1. Jakarta EE Specification Process (2022). *Jakarta Enterprise Beans 4.0 Specification*. Eclipse Foundation.
2. Oracle (2021). *Java Authentication and Authorization Service (JAAS) Reference Guide*.
3. Payara Services Ltd (2023). *Payara Server Documentation: Security Realms and JACC*.
4. Fowler, M. (2002). *Patterns of Enterprise Application Architecture*. Addison-Wesley Professional.
5. Evans, E. (2003). *Domain-Driven Design: Tackling Complexity in the Heart of Software*. Addison-Wesley Professional.
