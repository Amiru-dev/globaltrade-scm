# GlobalTrade SCM Deployment Guide

## Prerequisites

- **Payara Server** 6.2023.x or higher
- **MySQL** 8.0 or higher
- **Java** 17 or higher
- **Maven** 3.8 or higher
- **Git** (for version control)

## Database Setup

### 1. Create Database

```bash
mysql -u root -p < db/init-schema.sql
```

This will:
- Create the `globaltrade_scm` database
- Create all required tables (users, user_roles, refresh_tokens, etc.)
- Insert sample data for testing

### 2. Verify Database

```bash
mysql -u root -p -e "USE globaltrade_scm; SHOW TABLES;"
```

Expected output:
```
+------------------------+
| Tables_in_globaltrade_scm |
+------------------------+
| audit_logs             |
| carrier_routes         |
| customs_declarations   |
| inventory_items        |
| purchase_orders        |
| refresh_tokens         |
| shipments              |
| user_roles             |
| users                  |
| vendors                |
| warehouses             |
+------------------------+
```

## Application Deployment

### 1. Build Application

```bash
mvn clean install
```

This will:
- Compile all modules (common, ejb, web, ear)
- Run unit tests
- Package the EAR file

### 2. Configure Payara Datasource

#### Option A: Using payara-resources.xml (Automatic)

The `payara-resources.xml` file is included in the EAR and will automatically configure the datasource:

```xml
<jdbc-connection-pool name="GlobalTradePool"
                      datasource-classname="com.mysql.cj.jdbc.MysqlDataSource"
                      res-type="javax.sql.DataSource">
    <property name="Url" value="jdbc:mysql://localhost:3306/globaltrade_scm"/>
    <property name="User" value="globaltrade_user"/>
    <property name="Password" value="secure_password"/>
    <property name="useSSL" value="false"/>
</jdbc-connection-pool>
```

**Important**: Update the database credentials in `payara-resources.xml` before deployment.

#### Option B: Manual Configuration via Admin Console

1. Access Payara Admin Console: `http://localhost:4848`
2. Navigate to: Resources → JDBC → JDBC Connection Pools
3. Create new pool:
   - **Pool Name**: GlobalTradePool
   - **Datasource Classname**: com.mysql.cj.jdbc.MysqlDataSource
   - **Resource Type**: javax.sql.DataSource
4. Configure properties:
   - **Url**: jdbc:mysql://localhost:3306/globaltrade_scm
   - **User**: globaltrade_user
   - **Password**: secure_password
   - **useSSL**: false
5. Test connection: "Ping" button
6. Create JDBC Resource:
   - **JNDI Name**: jdbc/GlobalTradeDS
   - **Pool Name**: GlobalTradePool

### 3. Deploy Application

#### Using Deployment Script (Linux/Mac)

```bash
chmod +x deploy.sh
./deploy.sh
```

#### Using Deployment Script (Windows)

```cmd
deploy.bat
```

#### Manual Deployment

```bash
# Start Payara Server
$PAYARA_HOME/bin/asadmin start-domain

# Deploy EAR
$PAYARA_HOME/bin/asadmin deploy --contextroot /globaltrade-scm globaltrade-scm-ear/target/globaltrade-scm-ear.ear

# Verify deployment
$PAYARA_HOME/bin/asadmin list-applications
```

### 4. Verify Deployment

```bash
# Check application status
$PAYARA_HOME/bin/asadmin list-applications

# Test datasource connection
$PAYARA_HOME/bin/asadmin ping-connection-pool GlobalTradePool

# Check server logs
$PAYARA_HOME/bin/asadmin log-file-view
```

## Security Configuration

### 1. JAAS Security Realm

#### Configure via Admin Console

1. Navigate to: Configurations → server-config → Security → Realms
2. Create new realm:
   - **Realm Name**: GlobalTradeRealm
   - **Class Name**: com.globaltrade.ejb.security.GlobalTradeLoginModule
3. Configure properties:
   - **Datasource JNDI**: jdbc/GlobalTradeDS
   - **Password Hash Algorithm**: BCrypt

#### Configure via login.conf

Create `$PAYARA_HOME/domains/domain1/config/login.conf`:

```properties
GlobalTradeRealm {
    com.globaltrade.ejb.security.GlobalTradeLoginModule required;
};
```

### 2. JWT Secret Key

Set environment variable for JWT secret key:

```bash
# Linux/Mac
export JWT_SECRET_KEY="your-256-bit-secret-key-here"

# Windows
set JWT_SECRET_KEY=your-256-bit-secret-key-here
```

**Critical**: Use a strong, randomly generated secret key in production. Consider using a key management service (AWS KMS, HashiCorp Vault) for production deployments.

### 3. SSL/TLS Configuration

For production deployment, configure SSL/TLS:

1. Navigate to: Configurations → server-config → HTTP Service → HTTP Listeners
2. Configure SSL settings:
   - Enable SSL
   - Configure certificate
   - Set appropriate cipher suites

## Monitoring and Logging

### 1. Enable Application Logging

```bash
$PAYARA_HOME/bin/asadmin set-log-level com.globaltrade:INFO
```

### 2. Configure Monitoring

```bash
# Enable monitoring
$PAYARA_HOME/bin/asadmin enable-monitoring

# Configure specific monitoring levels
$PAYARA_HOME/bin/asadmin set monitoring-level=HIGH
```

### 3. Health Check

Test application health:

```bash
curl http://localhost:8080/globaltrade-scm/health
```

Expected response:
```json
{
  "status": "UP",
  "database": "UP",
  "memory": {
    "max": "1024 MB",
    "total": "512 MB",
    "used": "256 MB",
    "free": "256 MB",
    "usage_percent": 25
  },
  "timestamp": 1692672000000
}
```

### 4. MicroProfile Metrics (STUB)

**IMPORTANT**: The current MicroProfile Metrics implementation is a STUB that demonstrates the integration pattern. Full implementation requires:

1. **Enable MicroProfile Metrics in Payara**: Configure Payara server to enable MicroProfile Metrics extension
2. **Add Implementation Dependency**: Add Payara MicroProfile Metrics implementation to pom.xml
3. **Configure Metrics Endpoint**: Configure automatic /metrics endpoint in server configuration
4. **Test Metrics Endpoint**: Verify metrics are available at /metrics endpoint

**Current Stub Status**:
- `MetricsResource.java` demonstrates @Counted, @Timed, and @Gauge annotation patterns
- Metrics API dependency added to pom.xml but implementation is not configured
- Annotations are commented out to prevent runtime errors
- Manual metrics endpoint available at /metrics (not automatic MicroProfile endpoint)

**Production Implementation**:
- Uncomment @Gauge annotations after implementing MicroProfile Metrics
- Configure Payara server for automatic metrics endpoint
- Integrate with monitoring systems (Prometheus, Grafana)
- Configure custom metrics for business-specific monitoring

## Environment-Specific Configuration

### Development

```properties
# application-dev.properties
db.url=jdbc:mysql://localhost:3306/globaltrade_scm
db.username=globaltrade_user
db.password=dev_password
jwt.secret.key=dev-secret-key-change-in-production
server.port=8080
server.ssl.enabled=false
```

### Production

```properties
# application-prod.properties
db.url=jdbc:mysql://prod-db-server:3306/globaltrade_scm
db.username=prod_user
db.password=${DB_PASSWORD}
jwt.secret.key=${JWT_SECRET_KEY}
server.port=8080
server.ssl.enabled=true
```

## Troubleshooting

### Common Issues

#### 1. Datasource Connection Failed

**Symptoms**: Application fails to start, datasource connection errors

**Solutions**:
- Verify MySQL is running: `mysql -u root -p -e "SELECT 1"`
- Check datasource configuration in Payara admin console
- Test connection pool: `asadmin ping-connection-pool GlobalTradePool`
- Verify MySQL Connector/J driver is in Payara classpath

#### 2. JAAS Authentication Failed

**Symptoms**: Login failures, authentication errors

**Solutions**:
- Verify login module is configured correctly
- Check datasource JNDI name matches configuration
- Verify user table contains valid BCrypt password hashes
- Check Payara server logs for authentication errors

#### 3. JWT Token Validation Failed

**Symptoms**: 401 Unauthorized errors on API calls

**Solutions**:
- Verify JWT_SECRET_KEY environment variable is set
- Check JWT secret key matches between generation and validation
- Verify token expiration time is appropriate
- Check JWTAuthenticationFilter is registered as provider

#### 4. Deployment Failed

**Symptoms**: EAR deployment fails with errors

**Solutions**:
- Check Maven build completed successfully
- Verify EAR file exists: `ls -la globaltrade-scm-ear/target/`
- Check Payara server logs for deployment errors
- Verify Payara version compatibility (Jakarta EE 10)

## Production Deployment Considerations

### 1. High Availability

- Configure clustering for multiple Payara instances
- Use load balancer (nginx, HAProxy) for distribution
- Configure session replication for stateful beans
- Use external session store (Redis) for session management

### 2. Backup and Recovery

- Regular database backups
- Application configuration backups
- Disaster recovery plan testing
- Rollback procedures for failed deployments

### 3. Performance Tuning

- Configure JVM heap size appropriate for workload
- Tune database connection pool size
- Configure thread pool settings
- Enable HTTP/2 for improved performance

### 4. Security Hardening

- Enable SSL/TLS for all connections
- Configure firewall rules
- Regular security updates
- Security audit logging
- Intrusion detection systems

## Verification Checklist

- [ ] Database created and initialized
- [ ] Datasource configured and tested
- [ ] JAAS security realm configured
- [ ] JWT secret key configured
- [ ] Application deployed successfully
- [ ] Health check endpoint responding
- [ ] Authentication endpoints working
- [ ] Security headers present in responses
- [ ] CSRF protection enabled
- [ ] Monitoring and logging configured
- [ ] SSL/TLS configured (production)

## Support and Maintenance

For issues or questions:
- Check Payara Server documentation: https://docs.payara.fish
- Review Jakarta EE 10 specifications: https://jakarta.ee/specifications/
- Consult application logs: `$PAYARA_HOME/domains/domain1/logs/server.log`
- Enable debug logging for troubleshooting

## Next Steps

After successful deployment:
1. Test authentication flow with sample users
2. Verify REST API endpoints are accessible
3. Test JWT token generation and validation
4. Verify security headers are present
5. Test CSRF protection on state-changing operations
6. Monitor application performance and logs
7. Configure backup and monitoring systems