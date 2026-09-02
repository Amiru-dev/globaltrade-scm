# GlobalTrade Supply Chain Management System

## Enterprise Java EE 10 Implementation for Global Logistics Operations

A comprehensive supply chain management platform demonstrating advanced EJB features including timer services, interceptor architecture, transaction management, security frameworks, and exception handling designed for global logistics operations requiring 99.9% uptime.

## Table of Contents

- [System Overview](#system-overview)
- [Features](#features)
- [Technology Stack](#technology-stack)
- [Prerequisites](#prerequisites)
- [Installation Guide](#installation-guide)
- [Database Setup](#database-setup)
- [Application Server Configuration](#application-server-configuration)
- [Deployment Instructions](#deployment-instructions)
- [Testing Guide](#testing-guide)
- [User Guide](#user-guide)
- [Architecture Documentation](#architecture-documentation)
- [Performance Monitoring](#performance-monitoring)
- [Security Configuration](#security-configuration)
- [Troubleshooting](#troubleshooting)
- [Development Guide](#development-guide)
- [License](#license)

## System Overview

The GlobalTrade SCM system is a modern Java EE enterprise application designed to manage global supply chain operations for multinational corporations. The system provides automated scheduling, comprehensive security, robust transaction management, and real-time monitoring capabilities for logistics operations across 50+ countries.

### Business Context

GlobalTrade Logistics Corporation manages supply chains for multinational corporations facing critical operational challenges. This system addresses:
- Legacy system modernization with automated shipment tracking
- Inconsistent logistics transaction handling resolution
- Security vulnerabilities in vendor data access systems
- Poor exception handling during peak shipping seasons
- Limited audit trail for customs compliance requirements
- Inadequate real-time tracking of shipments and inventory

### System Capabilities

- **Automated Scheduling**: Timer services for shipment tracking, inventory monitoring, vendor performance evaluation, customs deadline tracking, and route optimization
- **Comprehensive Security**: Multi-layer security with role-based access control, JAAS custom login modules (primary authentication per assignment requirements), Jakarta Security integration layer, JWT for external APIs, and vendor data isolation
- **Transaction Management**: ACID-compliant transaction management with both container-managed and bean-managed transactions
- **Audit Trail**: Comprehensive audit logging for customs compliance and trade regulation requirements
- **Real-time Monitoring**: Performance monitoring, security auditing, and supply chain metrics
- **Exception Handling**: Custom exception hierarchy with recovery mechanisms for supply chain disruptions

## Features

### Timer Services
- **ShipmentTrackingTimer**: Automated shipment status monitoring every 15 minutes
- **InventoryMonitorTimer**: Daily inventory level monitoring with automated alerts
- **VendorPerformanceTimer**: Weekly vendor performance evaluation and scoring
- **CustomsDeadlineTimer**: Hourly customs documentation deadline tracking
- **RouteOptimizationTimer**: On-demand route optimization calculations

### Interceptor Framework
- **AuditLogInterceptor**: Comprehensive audit trail for all business operations (class-level via XML)
- **PerformanceMonitor**: Execution time monitoring and performance metrics (default interceptor via XML)
- **CustomsRegulationsInterceptor**: Trade compliance validation (CDI annotation binding)
- **SecurityAuditInterceptor**: Security event logging and monitoring (class-level via XML)
- **VendorDataValidator**: Vendor data integrity validation (method-level via @Interceptors)
- **ComplianceCheckInterceptor**: Regulatory compliance checking (class-level via XML)

### Transaction Management
- **Container-Managed Transactions**: Standard transactions with appropriate attributes
- **Bean-Managed Transactions**: Complex transaction scenarios requiring manual control
- **Distributed Transactions**: Cross-system transaction coordination
- **Transaction Isolation**: READ_COMMITTED isolation level for optimal performance

### Security Architecture
- **JAAS Authentication**: Custom login module with strong password hashing
- **Role-Based Access Control**: Five user roles with granular permissions
- **Programmatic Authorization**: Complex authorization scenarios
- **Emergency Access**: Time-limited emergency access with audit logging
- **Session Management**: Secure session management with timeout and fixation prevention

### Exception Handling
- **Custom Exception Hierarchy**: Supply chain-specific exceptions under SupplyChainException with @ApplicationException(rollback=false)
- **Exception Classification**: Application exceptions (rollback=false) vs system exceptions (rollback=true)
- **Recovery Service**: LogisticsRecoveryService implements carrier outage, customs failure, and weather disruption recovery strategies
- **Comprehensive Logging**: Exception logging with context for troubleshooting

## Technology Stack

### Core Technologies
- **Java Platform**: Java 17
- **Jakarta EE**: Jakarta EE 10
- **Application Server**: Payara Server 6.2025.11
- **Database**: MySQL 8.0
- **Build Tool**: Maven 3.9+

### Development Tools
- **Testing Framework**: JUnit 5.10.2, Mockito 5.11.0
- **IDE Support**: IntelliJ IDEA
- **Version Control**: Git
- **Documentation**: JavaDoc, Markdown

### Deployment Technologies
- **Packaging**: Maven multi-module with EAR deployment
- **Clustering**: Clustering analysis and timer persistence foundation (single-node deployment with clustering-ready architecture)
- **Monitoring**: Payara monitoring tools, JMeter, OWASP ZAP
- **Logging**: Java Util Logging (JUL)

## Prerequisites

### Software Requirements
- **Java Development Kit (JDK)**: Version 17 or higher
- **Maven**: Version 3.9 or higher
- **MySQL Server**: Version 8.0 or higher
- **Payara Server**: Version 6.2025.11 or higher
- **Git**: For version control (optional)

### Hardware Requirements
- **Minimum**: 4GB RAM, 2 CPU cores, 10GB disk space
- **Recommended**: 8GB RAM, 4 CPU cores, 20GB disk space
- **Production**: 16GB RAM, 8 CPU cores, 50GB disk space

### Operating System
- **Windows**: Windows 10/11, Windows Server 2019+
- **Linux**: Ubuntu 20.04+, CentOS 8+, RHEL 8+
- **macOS**: macOS 11+

## Installation Guide

### Step 1: Go to the Repository

```bash

cd globaltrade-scm
```

### Step 2: Install Java Development Kit

**Windows**:
1. Download JDK 17 from Oracle or OpenJDK
2. Run the installer and follow the prompts
3. Set JAVA_HOME environment variable
4. Add Java bin directory to PATH

**Linux**:
```bash

sudo apt update
sudo apt install openjdk-17-jdk
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH=$PATH:$JAVA_HOME/bin
```

**macOS**:
```bash

brew install openjdk@17
export JAVA_HOME=/usr/local/opt/openjdk@17
export PATH=$PATH:$JAVA_HOME/bin
```

### Step 3: Install Maven

**Windows**:
1. Download Maven from Apache Maven website
2. Extract to desired location
3. Set MAVEN_HOME environment variable
4. Add Maven bin directory to PATH

**Linux**:
```bash

sudo apt install maven
```

**macOS**:
```bash

brew install maven
```

### Step 4: Install MySQL Server

**Windows**:
1. Download MySQL Community Server from MySQL website
2. Run the installer and follow the prompts
3. Set root password during installation
4. Start MySQL service

**Linux**:
```bash

sudo apt install mysql-server
sudo mysql_secure_installation
```

**macOS**:
```bash

brew install mysql
brew services start mysql
```

### Step 5: Install Payara Server

1. Download Payara Server from Payara website
2. Extract to desired location
3. Set PAYARA_HOME environment variable
4. Add Payara bin directory to PATH

**Windows**:
```cmd
set PAYARA_HOME=C:\payara6
set PATH=%PATH%;%PAYARA_HOME%\bin
```

**Linux/macOS**:
```bash

export PAYARA_HOME=/opt/payara6
export PATH=$PATH:$PAYARA_HOME/bin
```

## Database Setup

### Step 1: Create Database

```sql
CREATE DATABASE globaltrade_scm CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'globaltrade'@'localhost' IDENTIFIED BY 'GlobalTrade@2026';
GRANT ALL PRIVILEGES ON globaltrade_scm.* TO 'globaltrade'@'localhost';
FLUSH PRIVILEGES;
```

### Step 2: Initialize Database Schema

Run the database initialization script:

```bash

mysql -u globaltrade -p globaltrade_scm < db/init-schema.sql
```

### Step 3: Verify Database Setup

```sql
USE globaltrade_scm;
SHOW TABLES;
```

Expected tables:
- audit_logs
- carrier_routes
- customs_declarations
- inventory_items
- purchase_orders
- refresh_tokens
- shipments
- users
- user_roles
- vendors
- warehouses

## Application Server Configuration

### Step 1: Start Payara Server

**Windows**:
```cmd
cd C:\payara6\glassfish\bin
asadmin start-domain
```

**Linux/macOS**:
```bash

cd $PAYARA_HOME/glassfish/bin
./asadmin start-domain
```

### Step 2: Configure JDBC Connection Pool

```bash

asadmin create-jdbc-connection-pool \
  --datasourceclassname com.mysql.cj.jdbc.MysqlDataSource \
  --restype javax.sql.ConnectionPoolDataSource \
  --property portNumber=3306:databaseName=globaltrade_scm:serverName=localhost:user=globaltrade:password=GlobalTrade@2026 \
  GlobalTradePool

asadmin create-jdbc-resource \
  --connectionpoolid GlobalTradePool \
  jdbc/GlobalTradeDS
```

### Step 3: Configure JAAS Realm

```bash

asadmin create-auth-realm \
  --classname com.sun.enterprise.security.auth.realm.jdbc.JDBCRealm \
  --property jaas-context=jdbcRealm:datasource-jndi=jdbc/GlobalTradeDS:user-table=users:user-name-column=username:password-column=password:group-table=user_roles:group-name-column=role:digest-algorithm=bcrypt \
  globaltrade-realm
```

### Step 4: Configure Security Roles

Edit `glassfish-web.xml` in the web module:

```xml
<security-role-mapping>
    <role-name>SYSTEM_ADMIN</role-name>
    <group-name>administrators</group-name>
</security-role-mapping>
<security-role-mapping>
    <role-name>LOGISTICS_COORDINATOR</role-name>
    <group-name>logistics</group-name>
</security-role-mapping>
<security-role-mapping>
    <role-name>WAREHOUSE_MANAGER</role-name>
    <group-name>warehouse</group-name>
</security-role-mapping>
<security-role-mapping>
    <role-name>CUSTOMS_AGENT</role-name>
    <group-name>customs</group-name>
</security-role-mapping>
<security-role-mapping>
    <role-name>VENDOR_REPRESENTATIVE</role-name>
    <group-name>vendors</group-name>
</security-role-mapping>
```

## Deployment Instructions

### Step 1: Build the Application

```bash

cd globaltrade-scm
mvn clean install
```

This will:
- Clean previous builds
- Compile all modules
- Run all tests
- Package the EAR file
- Install artifacts to local Maven repository

### Step 2: Deploy the Application

**Option 1: Using asadmin command line**

```bash

cd $PAYARA_HOME/glassfish/bin
./asadmin deploy --force /path/to/globaltrade-scm-ear/target/globaltrade-scm-ear-1.0.ear
```

**Option 2: Using Payara Admin Console**

1. Open browser to `http://localhost:4848`
2. Login with admin credentials
3. Navigate to Applications → Deploy
4. Select the EAR file
5. Click Deploy

### Step 3: Verify Deployment

```bash

asadmin list-applications
```

Expected output:
```
globaltrade-scm-ear-1.0 <ejb, web>
```

### Step 4: Access the Application

- **Application URL**: http://localhost:8080/globaltrade/
- **Admin Console**: http://localhost:4848
- **Note**: Default users must be created in the database via the init-schema.sql script

## Testing Guide

### Run Unit Tests

```bash

cd globaltrade-scm
mvn test
```

### Run Specific Test Classes

```bash

  mvn test -Dtest=TimerServiceTest
  mvn test -Dtest=InterceptorTest
  mvn test -Dtest=TransactionTest
  mvn test -Dtest=SecurityTest
  mvn test -Dtest=ExceptionHandlingTest
  mvn test -Dtest=PerformanceBenchmarkTest
```

### Run Integration Tests

```bash

mvn verify
```

### Generate Test Reports

Test reports are generated in:
- `globaltrade-scm-ejb/target/surefire-reports/`
- `globaltrade-scm-web/target/surefire-reports/`

### Test Coverage

Current test coverage:
- **Timer Services**: 100% (10 tests)
- **Interceptors**: 100% (5 tests)
- **Transaction Management**: 100% (8 tests)
- **Security**: 100% (12 tests)
- **Exception Handling**: 100% (11 tests)
- **Performance**: 100% (9 tests)
- **Integration**: 100% (15 tests)
- **Total**: 70 tests, 100% pass rate

## User Guide

### Default User Accounts

| Username | Password | Role | Access Level |
|----------|----------|------|--------------|
| admin | admin123 | SYSTEM_ADMIN | Full system access |
| logistics | logistics123 | LOGISTICS_COORDINATOR | Shipment and route management |
| warehouse | warehouse123 | WAREHOUSE_MANAGER | Inventory and warehouse operations |
| customs | customs123 | CUSTOMS_AGENT | Customs documentation and compliance |
| vendor | vendor123 | VENDOR_REPRESENTATIVE | Limited vendor data access |

**Note**: All passwords are BCrypt hashed with work factor 12. CHANGE IN PRODUCTION.

### Accessing the Application

1. Open browser to `http://localhost:8080/globaltrade/`
2. Login with appropriate credentials
3. Navigate to desired functionality based on role

### Key Features by Role

**SYSTEM_ADMIN**:
- Full system configuration
- User management
- Security administration
- System monitoring

**LOGISTICS_COORDINATOR**:
- Shipment creation and tracking
- Route optimization
- Carrier management
- Performance monitoring

**WAREHOUSE_MANAGER**:
- Inventory management
- Warehouse operations
- Stock transfer
- Replenishment

**CUSTOMS_AGENT**:
- Customs documentation
- Trade compliance
- Regulatory reporting
- Deadline tracking

**VENDOR_REPRESENTATIVE**:
- Vendor data access (own vendor only)
- Performance monitoring
- Purchase order status
- Shipment tracking

## Architecture Documentation

### Module Structure

```
globaltrade-scm/
├── globaltrade-scm-common/      # Shared domain model and utilities
├── globaltrade-scm-ejb/         # Business logic layer
├── globaltrade-scm-web/         # Presentation layer
└── globaltrade-scm-ear/         # Enterprise archive
```

### Key Components

**Timer Services** (`globaltrade-scm-ejb/src/main/java/com/globaltrade/ejb/timer/`):
- ShipmentTrackingTimer.java
- InventoryMonitorTimer.java
- VendorPerformanceTimer.java
- CustomsDeadlineTimer.java
- RouteOptimizationTimer.java

**Interceptors** (`globaltrade-scm-ejb/src/main/java/com/globaltrade/ejb/interceptor/`):
- AuditLogInterceptor.java
- PerformanceMonitor.java
- CustomsRegulationsInterceptor.java
- SecurityAuditInterceptor.java
- VendorDataValidator.java
- ComplianceCheckInterceptor.java

**Services** (`globaltrade-scm-ejb/src/main/java/com/globaltrade/ejb/service/`):
- ShipmentService.java
- InventoryService.java
- UserService.java
- SecurityService.java
- AuditService.java

**Security** (`globaltrade-scm-ejb/src/main/java/com/globaltrade/ejb/security/`):
- GlobalTradeLoginModule.java
- SecurityService.java
- SessionManager.java

## Performance Monitoring

### Monitoring Tools

**Payara Monitoring Console**:
- URL: http://localhost:4848
- Navigate to Monitoring → Server
- View EJB, Timer, Transaction, and Connection Pool metrics

**Application-Level Monitoring**:
- PerformanceMonitor interceptor provides method-level metrics
- AuditLogInterceptor tracks operation frequency
- Custom metrics available through JMX

### Key Performance Metrics

**PT002 Interceptor Chain Benchmark Results** (tested with Java 17.0.16, 10,000 iterations):

- **Baseline (without interceptors)**: Average 1.82ms, 95th percentile 2.23ms
- **With interceptor chain**: Average 1.84ms, 95th percentile 2.34ms
- **Interceptor overhead**: Average 0.03ms, Overhead percentage 1.38%, 95th percentile overhead 0.11ms
- **Target**: < 2.00% overhead
- **Result**: PASS

**Additional performance monitoring**:
- **PerformanceMonitor Interceptor**: Provides real-time method-level performance monitoring
- **Payara Monitoring Console**: Access at http://localhost:4848 for server-level metrics
- **Performance Dashboard**: Access at /performance-dashboard for application-level metrics

### Performance Tuning

**JVM Configuration**:
```bash

asadmin create-jvm-options -Xmx2g -Xms512m -XX:+UseG1GC
```

**Connection Pool Configuration**:
```bash

asadmin set resources.jdbc-connection-pool.GlobalTradePool.max-pool-size=100
asadmin set resources.jdbc-connection-pool.GlobalTradePool.initial-pool-size=10
```

**EJB Pool Configuration**:
Edit `ejb-jar.xml` to adjust pool sizes for individual EJBs.

## Security Configuration

### Password Security

**Current Configuration**:
- Algorithm: BCrypt with work factor 12
- Adaptive work factor for GPU cracking resistance
- Account Lockout: 5 failed attempts, 30-minute unlock

**Recommended Enhancements**:
- Implement multi-factor authentication
- Implement password expiry policies
- Implement password history checks

### Session Security

**Current Configuration**:
- Session Timeout: 30 minutes
- Session Fixation Prevention: New session on login
- Concurrent Session Prevention: Single session per user
- HttpOnly, Secure, and SameSite flags on cookies

**Recommended Enhancements**:
- Implement HTTPS

- Implement session binding with IP address

### Data Encryption

**Current Configuration**:
- Password hashing: BCrypt with work factor 12
- Data at rest: Not encrypted
- Data in transit: Not encrypted (HTTP)

**Recommended Enhancements**:
- Implement AES-256 encryption for sensitive database fields
- Implement HTTPS for all communications
- Implement TLS 1.3 for secure data transmission

## Troubleshooting

### Common Issues

**Issue: Database Connection Failed**
- Solution: Verify MySQL is running, check connection pool configuration, verify credentials

**Issue: Deployment Failed**
- Solution: Check application server logs, verify database schema, check classpath conflicts

**Issue: Authentication Failed**
- Solution: Verify user credentials exist in database, check JAAS realm configuration, verify password hashing

**Issue: Timer Not Executing**
- Solution: Check timer service logs, verify timer persistence, check thread pool configuration

**Issue: Performance Issues**
- Solution: Check database query performance, review connection pool utilization, verify JVM memory settings

### Log Files

**Application Server Logs**:
- `$PAYARA_HOME/glassfish/domains/domain1/logs/server.log`
- `$PAYARA_HOME/glassfish/domains/domain1/logs/access.log`

**Application Logs**:
- Check server.log for application-specific messages
- Use Java Util Logging configuration to adjust log levels

### Support

For additional support:
- Check technical documentation: `TECHNICAL_DOCUMENTATION.pdf`
- Review critical analysis report: `CRITICAL_ANALYSIS_AND_TEST_REPORT.pdf` 

## Development Guide

### Project Structure

**Source Code Organization**:
- `globaltrade-scm-common/src/main/java/com/globaltrade/common/` - Shared components
- `globaltrade-scm-ejb/src/main/java/com/globaltrade/ejb/` - Business logic
- `globaltrade-scm-web/src/main/java/com/globaltrade/web/` - Web layer
- `globaltrade-scm-ear/src/main/application/` - EAR configuration

### Coding Standards

**Java Code Style**:
- Follow Java naming conventions
- Use meaningful variable and method names
- Add comprehensive JavaDoc comments
- Follow package structure guidelines

**EJB Best Practices**:
- Use appropriate transaction attributes
- Implement proper exception handling
- Use interface-based design for remote access
- Implement proper resource cleanup

### Adding New Features

**Adding a New Timer Service**:
1. Create timer class in `globaltrade-scm-ejb/src/main/java/com/globaltrade/ejb/timer/`
2. Implement appropriate timer configuration
3. Add unit tests in `globaltrade-scm-ejb/src/test/java/com/globaltrade/ejb/test/`
4. Update technical documentation

**Adding a New Interceptor**:
1. Create interceptor class in `globaltrade-scm-ejb/src/main/java/com/globaltrade/ejb/interceptor/`
2. Implement @AroundInvoke method
3. Add unit tests for interceptor behavior
4. Apply interceptor to appropriate EJBs

**Adding a New Security Role**:
1. Add role to security configuration
2. Update role mappings in deployment descriptors
3. Add user groups in database
4. Test role-based access control

### Build and Test Cycle

**Development Workflow**:
```bash
# Make code changes
mvn clean compile
mvn test
mvn package
# Deploy and test
asadmin deploy --force target/globaltrade-scm-ear-1.0.ear
```

## License

This project is developed for educational purposes as part of Enterprise Java Development coursework.

## Acknowledgments

- Jakarta EE Community for excellent enterprise Java specifications
- Payara Community for robust application server implementation
- Maven Community for build tooling
- JUnit and Mockito for testing frameworks

## Contact

For questions or support regarding this implementation, please refer to the accompanying technical documentation and critical analysis reports.

---

**Version**: 1.0
**Last Updated**: 2026-09-01
**Documentation Version**: 1.0