# Deployment Guide: GlobalTrade SCM System

This guide outlines the steps required to build, configure, and deploy the GlobalTrade Supply Chain Management system to a Payara Server environment.

## 1. Prerequisites

Ensure the following software is installed and configured on your host machine:
- **Java Development Kit (JDK):** Version 17 (LTS)
- **Apache Maven:** Version 3.9.0 or higher
- **Application Server:** Payara Server 6.2025.11 (Community Edition)
- **Database:** MySQL 8.0+

---

## 2. Database Configuration

The application requires a MySQL database for persistence and a custom JAAS realm for authentication.

### 2.1 Initialize the Schema
1. Log in to your MySQL server as the `root` user or an administrator.
2. Execute the initialization script located in the project repository:
   ```bash
   mysql -u root -p < globaltrade-scm/db/init-schema.sql
   ```
   *(Note: This script creates the `globaltrade_scm` database, creates the necessary tables for JPA, sets up the `users` and `user_roles` tables for JAAS, and inserts initial seed data).*

### 2.2 Create a Database User
Ensure a dedicated user exists for the application to connect to the database:
```sql
CREATE USER 'scm_user'@'localhost' IDENTIFIED BY 'scm_password';
GRANT ALL PRIVILEGES ON globaltrade_scm.* TO 'scm_user'@'localhost';
FLUSH PRIVILEGES;
```

---

## 3. Payara Server Configuration

The Payara Server must be configured with a JDBC Connection Pool, a JDBC Resource (JNDI), and a Security Realm before deployment.

### 3.1 Start Payara Server
Start the default domain (usually `domain1`):
```bash
<PAYARA_HOME>/bin/asadmin start-domain
```

### 3.2 Configure the JDBC Connection Pool
Create a connection pool targeting the MySQL database. You will need the MySQL Connector/J driver placed in `<PAYARA_HOME>/glassfish/domains/domain1/lib/ext/`.

```bash
<PAYARA_HOME>/bin/asadmin create-jdbc-connection-pool \
    --datasourceclassname com.mysql.cj.jdbc.MysqlDataSource \
    --restype javax.sql.DataSource \
    --property user=scm_user:password=scm_password:databaseName=globaltrade_scm:serverName=localhost:portNumber=3306 \
    GlobalTradePool
```

### 3.3 Configure the JDBC Resource (JNDI)
Create the JNDI resource referenced by the application's `persistence.xml`:
```bash
<PAYARA_HOME>/bin/asadmin create-jdbc-resource \
    --connectionpoolid GlobalTradePool \
    jdbc/GlobalTradeDS
```

### 3.4 Configure the Security Realm
Create a custom JDBC realm for the JAAS authentication module to use:
```bash
<PAYARA_HOME>/bin/asadmin create-auth-realm \
    --classname com.sun.enterprise.security.auth.realm.jdbc.JDBCRealm \
    --property jaas-context=jdbcRealm:datasource-jndi=jdbc/GlobalTradeDS:user-table=users:user-name-column=username:password-column=password_hash:group-table=user_roles:group-name-column=role_name:digest-algorithm=SHA-256 \
    GlobalTradeRealm
```

---

## 4. Build the Application

Use Maven to compile the source code, run the unit tests, and package the application into an Enterprise Archive (EAR).

1. Navigate to the root of the project directory (where the parent `pom.xml` resides).
2. Execute the Maven clean and install phase:
   ```bash
   mvn clean install
   ```
3. Verify that the build succeeds. The resulting EAR file will be located at:
   `globaltrade-scm/globaltrade-scm-ear/target/globaltrade-scm-ear-1.0-SNAPSHOT.ear`

---

## 5. Deployment

Deploy the built EAR file to the running Payara Server.

### Option A: Using the Payara Admin Console (GUI)
1. Open a web browser and navigate to `http://localhost:4848`.
2. Go to **Applications**.
3. Click **Deploy**.
4. Browse and select the `globaltrade-scm-ear-1.0-SNAPSHOT.ear` file.
5. Set the Context Root to `/globaltrade` (if not automatically detected).
6. Click **OK**.

### Option B: Using the asadmin CLI
Execute the following command from your terminal:
```bash
<PAYARA_HOME>/bin/asadmin deploy \
    --contextroot /globaltrade \
    globaltrade-scm/globaltrade-scm-ear/target/globaltrade-scm-ear-1.0-SNAPSHOT.ear
```

---

## 6. Verification

Once deployed, verify the system is operational:

### 6.1 Check the Application Health
Open a terminal and ping the unauthenticated health endpoint:
```bash
curl -X GET http://localhost:8080/globaltrade/api/dashboard/health
```
**Expected Output:**
```json
{"status":"UP","application":"GlobalTrade SCM","version":"1.0-SNAPSHOT"}
```

### 6.2 Verify Secured Endpoints
Attempt to access a secured endpoint without credentials (should return `401 Unauthorized`):
```bash
curl -i -X GET http://localhost:8080/globaltrade/api/vendors
```

Attempt to access the same endpoint with valid credentials (e.g., assuming `admin`:`password` is seeded in the database):
```bash
curl -i -u admin:password -X GET http://localhost:8080/globaltrade/api/vendors
```
**Expected Output:** `200 OK` with a JSON payload of vendors.

### 6.3 Monitor Timer Execution
Inspect the Payara Server log file to verify that the EJB Timers are executing as scheduled:
```bash
tail -f <PAYARA_HOME>/glassfish/domains/domain1/logs/server.log
```
Look for log entries prefixed with `[ShipmentTrackingTimer]` or `[InventoryMonitorTimer]`.
