# GlobalTrade SCM - Deployment Guide

This guide provides step-by-step instructions for deploying the GlobalTrade Supply Chain Management application to a new machine.

## Table of Contents
1. [Prerequisites](#prerequisites)
2. [Environment Setup](#environment-setup)
3. [Database Configuration](#database-configuration)
4. [Application Build](#application-build)
5. [Payara Server Installation](#payara-server-installation)
6. [Payara JDBC Configuration](#payara-jdbc-configuration)
7. [JAAS Security Configuration](#jaas-security-configuration)
8. [Custom LoginModule Setup](#custom-loginmodule-setup)
9. [Application Deployment](#application-deployment)
10. [Testing and Verification](#testing-and-verification)
11. [Troubleshooting](#troubleshooting)

---

## Prerequisites

### Required Software
- **Java Development Kit (JDK) 17 or higher**
- **Apache Maven 3.6+**
- **MySQL Server 8.0+**
- **Payara Server 6.x**
- **Git** (for cloning the repository)

### System Requirements
- **RAM**: Minimum 4GB (8GB recommended)
- **Disk Space**: Minimum 10GB free space
- **Operating System**: Windows 10/11, Linux, or macOS

---

## Environment Setup

### 1. Install Java Development Kit (JDK)

#### Windows:
```bash
# Download JDK 17 from Oracle or Adoptium
# Set JAVA_HOME environment variable
set JAVA_HOME=C:\Program Files\Java\jdk-17
# Add to PATH
set PATH=%JAVA_HOME%\bin;%PATH%
```

#### Linux:
```bash
sudo apt update
sudo apt install openjdk-17-jdk
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH
```

### 2. Install Apache Maven

#### Windows:
```bash
# Download Maven from https://maven.apache.org/download.cgi
# Extract to C:\Program Files\Apache\maven
set MAVEN_HOME=C:\Program Files\Apache\maven
set PATH=%MAVEN_HOME%\bin;%PATH%
```

#### Linux:
```bash
sudo apt install maven
# Verify installation
mvn --version
```

### 3. Install MySQL Server

#### Windows:
```bash
# Download MySQL Installer from https://dev.mysql.com/downloads/installer/
# Install MySQL Server 8.0+
# Set root password during installation
```

#### Linux:
```bash
sudo apt update
sudo apt install mysql-server
sudo mysql_secure_installation
```

### 4. Install Payara Server

#### Windows:
```bash
# Download Payara Server 6.x from https://www.payara.fish/downloads
# Extract to C:\Program Files\Payara\payara6
cd C:\Program Files\Payara\payara6\bin
asadmin start-domain
```

#### Linux:
```bash
# Download Payara Server 6.x
wget https://www.payara.fish/downloads/payara-6.2025.11.zip
unzip payara-6.2025.11.zip -d /opt/payara6
cd /opt/payara6/bin
./asadmin start-domain
```

---

## Database Configuration

### 1. Create Database and User

```sql
-- Connect to MySQL as root
mysql -u root -p

-- Create database
CREATE DATABASE globaltrade_scm CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Create database user
CREATE USER 'globaltrade_user'@'localhost' IDENTIFIED BY 'secure_password';

-- Grant privileges
GRANT ALL PRIVILEGES ON globaltrade_scm.* TO 'globaltrade_user'@'localhost';

-- Flush privileges
FLUSH PRIVILEGES;

-- Exit
EXIT;
```

### 2. Verify Database Connection

```bash
# Test connection
mysql -u globaltrade_user -p globaltrade_scm
# Should connect successfully
```

---

## Application Build

### 1. Clone the Repository

```bash
git clone <repository-url> globaltrade-scm
cd globaltrade-scm
```

### 2. Build the Application

```bash
# Clean and build without tests
mvn clean package -DskipTests

# The following artifacts will be created:
# - globaltrade-scm-common/target/globaltrade-scm-common-1.0.jar
# - globaltrade-scm-ejb/target/globaltrade-scm-ejb-1.0.jar
# - globaltrade-scm-web/target/globaltrade-scm-web-1.0.war
# - globaltrade-scm-ear/target/globaltrade-scm-ear-1.0.ear
```

### 3. Verify Build Output

```bash
# Check that EAR file was created
ls -la globaltrade-scm-ear/target/globaltrade-scm-ear-1.0.ear
```

---

## Payara JDBC Configuration

### Option 1: Payara Admin Console (GUI)

#### Step 1: Access Admin Console
1. Open browser: `http://localhost:4848`
2. Login with default credentials (admin/adminadmin)

#### Step 2: Create JDBC Connection Pool
1. Navigate to: Resources → JDBC → JDBC Connection Pools
2. Click "New"
3. Configure:
   - **Name**: `GlobalTradePool`
   - **Resource Type**: `javax.sql.DataSource`
   - **Database Vendor**: `MySQL`
4. Click "Next"
5. Configure pool settings:
   - **Datasource Classname**: `com.mysql.cj.jdbc.MysqlDataSource`
   - **Additional Properties**:
     - `Url`: `jdbc:mysql://localhost:3306/globaltrade_scm`
     - `User`: `globaltrade_user`
     - `Password`: `secure_password`
     - `DatabaseName`: `globaltrade_scm`
     - `ServerName`: `localhost`
     - `PortNumber`: `3306`
     - `useSSL`: `false`
6. Click "Finish"

#### Step 3: Create JDBC Resource
1. Navigate to: Resources → JDBC → JDBC Resources
2. Click "New"
3. Configure:
   - **JNDI Name**: `jdbc/GlobalTradeDS`
   - **Pool Name**: `GlobalTradePool`
4. Click "OK"

#### Step 4: Test Connection
1. Navigate to: Resources → JDBC → JDBC Connection Pools
2. Select `GlobalTradePool`
3. Click "Ping"
4. Should return "Ping Succeeded"

### Option 2: Command Line

```bash
cd <payara-installation>/bin
asadmin

# Create JDBC connection pool
create-jdbc-connection-pool \
  --datasourceclassname com.mysql.cj.jdbc.MysqlDataSource \
  --restype javax.sql.DataSource \
  --property Url=jdbc\:mysql\://localhost\:3306/globaltrade_scm:User=globaltrade_user:Password=secure_password:DatabaseName=globaltrade_scm:ServerName=localhost:PortNumber=3306:useSSL=false \
  GlobalTradePool

# Create JDBC resource
create-jdbc-resource --poolname GlobalTradePool jdbc/GlobalTradeDS

# Test connection
ping-connection-pool GlobalTradePool

# Exit asadmin
exit
```

---

## JAAS Security Configuration

### 1. Configure JDBC Realm in Admin Console

#### Step 1: Create Security Realm
1. Navigate to: Configurations → server-config → Security → Realms
2. Click "New"
3. Configure:
   - **Realm Name**: `GlobalTradeRealm`
   - **Class Name**: `com.sun.enterprise.security.auth.realm.jdbc.JDBCRealm`
4. Click "OK"

#### Step 2: Configure Realm Properties
1. Select `GlobalTradeRealm`
2. Configure properties:
   - **JAAS Context**: `jdbcRealm`
   - **JNDI**: `jdbc/GlobalTradeDS`
   - **User Table**: `users`
   - **User Name Column**: `username`
   - **Password Column**: `password_hash`
   - **Group Table**: `user_roles`
   - **Group Table User Name Column**: `user_id`
   - **Group Name Column**: `role_name`
   - **Digest Algorithm**: (leave blank)
3. Click "Save"

### 2. Update login.conf File

```bash
# Navigate to Payara config directory
cd <payara-installation>/glassfish/domains/domain1/config

# Edit login.conf file
# Add the following at the end:

GlobalTradeRealm {
    com.globaltrade.ejb.security.GlobalTradeLoginModule required;
};
```

---

## Custom LoginModule Setup

### 1. Copy Custom LoginModule JARs to Payara

```bash
# Copy the common module (contains LoginModule and dependencies)
copy globaltrade-scm-common\target\globaltrade-scm-common-1.0.jar <payara-installation>\glassfish\lib\

# Copy BCrypt library
copy globaltrade-scm-ear\target\globaltrade-scm-ear-1.0\lib\org.mindrot-jbcrypt-0.4.jar <payara-installation>\glassfish\lib\jbcrypt-0.4.jar
```

### 2. Linux/Mac Commands:
```bash
cp globaltrade-scm-common/target/globaltrade-scm-common-1.0.jar /opt/payara6/glassfish/lib/
cp globaltrade-scm-ear/target/globaltrade-scm-ear-1.0/lib/org.mindrot-jbcrypt-0.4.jar /opt/payara6/glassfish/lib/jbcrypt-0.4.jar
```

### 3. Restart Payara Server

```bash
cd <payara-installation>/bin
asadmin restart-domain
```

---

## Application Deployment

### Option 1: Admin Console Deployment

#### Step 1: Deploy EAR File
1. Navigate to: Applications
2. Click "Deploy"
3. Upload the EAR file: `globaltrade-scm-ear/target/globaltrade-scm-ear-1.0.ear`
4. Configure:
   - **Application Name**: `globaltrade-scm-ear-1.0`
   - **Context Root**: `/globaltrade`
5. Click "OK"

#### Step 2: Verify Deployment
1. Check that application appears in Applications list
2. Status should be "Enabled"

### Option 2: Command Line Deployment

```bash
cd <payara-installation>/bin
asadmin

# Deploy the application
deploy --name globaltrade-scm-ear-1.0 --contextroot /globaltrade <path-to-ear>/globaltrade-scm-ear-1.0.ear

# Verify deployment
list-applications

# Exit asadmin
exit
```

---

## Testing and Verification

### 1. Test Application Access

```bash
# Access the application
http://localhost:8080/globaltrade
```

### 2. Test Authentication

#### Default Test Users (created by DatabaseSeeder):
- **Username**: `admin`
- **Password**: `admin123`
- **Role**: `SYSTEM_ADMIN`

Other test users:
- `logistics_user` / `logistics123` (LOGISTICS role)
- `warehouse_user` / `warehouse123` (WAREHOUSE role)
- `customs_user` / `customs123` (CUSTOMS role)
- `vendor_user` / `vendor123` (VENDOR role)

### 3. Verify Database Operations

```sql
-- Connect to database
mysql -u globaltrade_user -p globaltrade_scm

-- Check if tables were created
SHOW TABLES;

-- Check if users were seeded
SELECT username, full_name, email FROM users;

-- Check if roles were assigned
SELECT u.username, r.role_name FROM users u
JOIN user_roles r ON u.id = r.user_id;
```

### 4. Check Server Logs

```bash
# View server logs
cd <payara-installation>/glassfish/domains/domain1/logs
tail -f server.log

# Look for successful authentication messages:
# "SECURITY: Authentication SUCCEEDED for user: admin"
# "SECURITY: Committed 2 principals for user: admin"
```

### 5. Test Business Functions

1. **Dashboard**: Verify dashboard loads with statistics
2. **Shipment Management**: Create/view shipments
3. **Inventory**: Check inventory levels
4. **Audit Logs**: Verify audit logging is working
5. **Performance**: Check response times

---

## Troubleshooting

### Common Issues and Solutions

#### 1. EntityManagerFactory Error
**Error**: `Unable to retrieve EntityManagerFactory for unitName GlobalTradePU`

**Solution**:
- Verify JDBC connection pool is configured correctly
- Test connection pool ping in admin console
- Ensure database user has proper privileges
- Restart Payara server after JDBC configuration

#### 2. Authentication Failure
**Error**: Login fails with "Invalid username or password"

**Solution**:
- Verify custom LoginModule JARs are in Payara lib directory
- Check login.conf configuration
- Ensure database users table is populated
- Verify BCrypt library is accessible
- Check server logs for detailed error messages

#### 3. Database Connection Error
**Error**: `Access denied for user 'globaltrade_user'@'localhost'`

**Solution**:
- Verify database user exists and has correct password
- Check MySQL user privileges
- Ensure MySQL service is running
- Test connection with MySQL client: `mysql -u globaltrade_user -p globaltrade_scm`

#### 4. Port Conflicts
**Error**: Payara fails to start due to port conflicts

**Solution**:
```bash
# Check which ports are in use
netstat -ano | findstr :4848  # Admin console
netstat -ano | findstr :8080  # HTTP
netstat -ano | findstr :3306  # MySQL

# Change Payara ports if needed
cd <payara-installation>/bin
asadmin
set configs.config.server-config.network-config.network-listeners.network-listener.http-listener-1.port=8081
set configs.config.server-config.admin-service.das-config.admin-listener.port=4849
restart-domain
```

#### 5. Class Loading Issues
**Error**: `ClassNotFoundException: com.globaltrade.ejb.security.GlobalTradeLoginModule`

**Solution**:
- Verify JAR files are copied to correct location: `<payara>/glassfish/lib/`
- Ensure BCrypt JAR is also in the lib directory
- Restart Payara server
- Check JAR file permissions on Linux/Mac

#### 6. Application Deployment Failures
**Error**: Deployment fails with missing dependencies

**Solution**:
- Verify build completed successfully: `mvn clean package -DskipTests`
- Check EAR file contents
- Ensure all dependency JARs are included in EAR/lib
- Review Payara server logs for specific dependency errors

---

## Production Deployment Considerations

### Security Hardening

1. **Change Default Admin Passwords**:
```bash
cd <payara-installation>/bin
asadmin
change-admin-password
```

2. **Enable SSL/HTTPS**:
```bash
asadmin
create-ssl --type http-listener --listenername http-listener-2
set configs.config.server-config.network-config.protocols.protocol.http-listener-2.security-enabled=true
```

3. **Database Security**:
- Use strong passwords for database users
- Restrict database access to localhost only
- Enable MySQL SSL for remote connections
- Regular security updates

4. **Application Security**:
- Review and update security policies
- Enable rate limiting
- Configure firewall rules
- Regular security audits

### Performance Optimization

1. **JVM Tuning**:
```bash
# Edit <payara>/glassfish/domains/domain1/config/domain.xml
# Increase heap size
<jvm-options>-Xmx2048m</jvm-options>
<jvm-options>-Xms512m</jvm-options>
```

2. **Connection Pool Tuning**:
- Increase pool size based on expected load
- Configure connection validation
- Set appropriate timeout values

3. **Database Optimization**:
- Add indexes to frequently queried columns
- Configure MySQL buffer pool size
- Enable query cache if appropriate

### Monitoring and Logging

1. **Enable Application Logging**:
```bash
asadmin
set-log-level com.globaltrade.ejb.security.GlobalTradeLoginModule=INFO
set-log-level com.globaltrade.web.servlet.LoginServlet=INFO
```

2. **Configure Log Rotation**:
- Set up automatic log file rotation
- Configure log retention policy
- Enable error log monitoring

3. **Performance Monitoring**:
- Enable Payara monitoring
- Set up JVM monitoring
- Configure database query logging

### Backup and Recovery

1. **Database Backups**:
```bash
# Regular database backups
mysqldump -u globaltrade_user -p globaltrade_scm > backup_$(date +%Y%m%d).sql
```

2. **Application Backups**:
- Backup EAR file
- Backup Payara configuration
- Document any custom configurations

3. **Disaster Recovery**:
- Test restore procedures regularly
- Maintain offsite backups
- Document recovery procedures

---

## Maintenance

### Regular Maintenance Tasks

1. **Weekly**:
- Review server logs for errors
- Check disk space usage
- Monitor application performance

2. **Monthly**:
- Apply security updates
- Review and optimize database
- Test backup and recovery procedures

3. **Quarterly**:
- Security audit
- Performance review
- Capacity planning

### Updates and Upgrades

1. **Application Updates**:
```bash
# Build new version
mvn clean package -DskipTests

# Undeploy old version
asadmin undeploy globaltrade-scm-ear-1.0

# Deploy new version
asadmin deploy --name globaltrade-scm-ear-1.0 --contextroot /globaltrade target/globaltrade-scm-ear-1.0.ear
```

2. **Payara Updates**:
- Follow Payara upgrade documentation
- Test in staging environment first
- Backup configuration before upgrade

3. **MySQL Updates**:
- Schedule maintenance windows
- Backup database before updates
- Test compatibility with application

---

## Contact and Support

For issues or questions regarding deployment:
- Review server logs: `<payara>/glassfish/domains/domain1/logs/server.log`
- Check Payara documentation: https://docs.payara.fish
- Review application documentation in the project repository

---

## Appendix: Quick Reference

### Essential Commands

```bash
# Payara Server
asadmin start-domain
asadmin stop-domain
asadmin restart-domain
asadmin list-applications
asadmin deploy <ear-file>
asadmin undeploy <app-name>

# Database
mysql -u globaltrade_user -p globaltrade_scm
mysqldump -u globaltrade_user -p globaltrade_scm > backup.sql

# Application Build
mvn clean package -DskipTests
mvn clean install -DskipTests
```

### Important File Locations

- **Payara Config**: `<payara>/glassfish/domains/domain1/config/`
- **Payara Logs**: `<payara>/glassfish/domains/domain1/logs/`
- **Application EAR**: `globaltrade-scm-ear/target/globaltrade-scm-ear-1.0.ear`
- **Login Config**: `<payara>/glassfish/domains/domain1/config/login.conf`
- **Database Config**: `<payara>/glassfish/domains/domain1/config/payara-resources.xml`

### Default Credentials

- **Payara Admin**: admin / adminadmin (change immediately)
- **Database User**: globaltrade_user / secure_password (change in production)
- **Application Admin**: admin / admin123 (change in production)

---

**Deployment Guide Version**: 1.0
**Last Updated**: 2025-08-25
**Application Version**: 1.0
**Compatible Payara Version**: 6.x
**Compatible MySQL Version**: 8.0+