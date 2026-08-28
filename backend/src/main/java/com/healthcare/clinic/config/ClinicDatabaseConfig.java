package com.healthcare.clinic.config;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.context.annotation.DependsOn;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages = {
                "com.healthcare.clinic.appointment.repository",
                "com.healthcare.clinic.audit.repository",
                "com.healthcare.clinic.backoffice.inventory.repository",
                "com.healthcare.clinic.billing.repository",
                "com.healthcare.clinic.branch.repository",
                "com.healthcare.clinic.clinicaldecision.repository",
                "com.healthcare.clinic.department.repository",
                "com.healthcare.clinic.doctor.medicine.repository",
                "com.healthcare.clinic.doctor.repository",
                "com.healthcare.clinic.document.repository",
                "com.healthcare.clinic.emergency.repository",
                "com.healthcare.clinic.emr.repository",
                "com.healthcare.clinic.finance.repository",
                "com.healthcare.clinic.homevisit.repository",
                "com.healthcare.clinic.identity.repository",
                "com.healthcare.clinic.inpatient.repository",
                "com.healthcare.clinic.insurance.repository",
                "com.healthcare.clinic.laboratory.repository",
                "com.healthcare.clinic.medicalrecord.repository",
                "com.healthcare.clinic.notification.repository",
                "com.healthcare.clinic.nursing.repository",
                "com.healthcare.clinic.patient.repository",
                "com.healthcare.clinic.radiology.repository",
                "com.healthcare.clinic.reception.repository",
                "com.healthcare.clinic.superadmin.repository",
                "com.healthcare.clinic.support.repository",
                "com.healthcare.clinic.surgery.repository",
                "com.healthcare.clinic.tenant.repository"
        },
        entityManagerFactoryRef = "clinicEntityManagerFactory",
        transactionManagerRef = "clinicTransactionManager",
        nameGenerator = org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator.class
)
public class ClinicDatabaseConfig {

    @Autowired
    private org.springframework.core.env.Environment environment;

    @Primary
    @Bean(name = "clinicDataSource")
    @ConfigurationProperties(prefix = "app.datasource.clinic")
    public DataSource dataSource() {
        String url = environment.getProperty("app.datasource.clinic.url");
        if (url == null || url.trim().isEmpty()) {
            url = environment.getProperty("SPRING_DATASOURCE_CLINIC_URL");
        }
        
        String username = environment.getProperty("app.datasource.clinic.username");
        if (username == null || username.trim().isEmpty()) {
            username = environment.getProperty("SPRING_DATASOURCE_CLINIC_USERNAME");
        }
        
        String password = environment.getProperty("app.datasource.clinic.password");
        if (password == null || password.trim().isEmpty()) {
            password = environment.getProperty("SPRING_DATASOURCE_CLINIC_PASSWORD");
        }
        
        String driver = environment.getProperty("app.datasource.clinic.driver-class-name");
        if (driver == null || driver.trim().isEmpty()) {
            driver = environment.getProperty("SPRING_DATASOURCE_CLINIC_DRIVER_CLASS_NAME");
        }

        boolean isRender = java.util.Arrays.asList(environment.getActiveProfiles()).contains("render");
        boolean isH2Fallback = url == null || url.trim().isEmpty() || url.contains("jdbc:h2");
        
        if (isRender) {
            if (isH2Fallback) throw new IllegalStateException("FATAL: SPRING_DATASOURCE_CLINIC_URL is missing in production.");
            if (username == null || username.trim().isEmpty()) throw new IllegalStateException("FATAL: Database username is missing in production.");
        }

        if (isH2Fallback) {
            url = "jdbc:h2:mem:clinicdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;NON_KEYWORDS=VALUE";
            driver = "org.h2.Driver"; // Force driver to match URL
        } else {
            driver = (driver != null && !driver.trim().isEmpty()) ? driver : (url.startsWith("jdbc:postgresql") ? "org.postgresql.Driver" : (url.startsWith("jdbc:tc:postgresql") ? "org.testcontainers.jdbc.ContainerDatabaseDriver" : "org.postgresql.Driver"));
        }

        username = (username != null && !username.trim().isEmpty()) ? username : "sa";
        password = (password != null) ? password : "";

        com.zaxxer.hikari.HikariDataSource dataSource = new com.zaxxer.hikari.HikariDataSource();
        dataSource.setJdbcUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setDriverClassName(driver);
        dataSource.setKeepaliveTime(environment.getProperty("app.datasource.clinic.keepalive-time", Long.class, 120000L));
        dataSource.setConnectionTestQuery("SELECT 1");
        dataSource.setMaximumPoolSize(environment.getProperty("app.datasource.clinic.maximum-pool-size", Integer.class, 5));
        dataSource.setMinimumIdle(environment.getProperty("app.datasource.clinic.minimum-idle", Integer.class, 1));
        dataSource.setConnectionTimeout(environment.getProperty("app.datasource.clinic.connection-timeout", Long.class, 30000L));
        dataSource.setIdleTimeout(environment.getProperty("app.datasource.clinic.idle-timeout", Long.class, 600000L));
        dataSource.setMaxLifetime(environment.getProperty("app.datasource.clinic.max-lifetime", Long.class, 1800000L));

        try (java.sql.Connection testConn = dataSource.getConnection()) {
            System.out.println("[Clinic DB] Test connection succeeded: "
                + testConn.getMetaData().getURL());
        } catch (java.sql.SQLException e) {
            if (!isRender) {
                System.err.println("[Clinic DB] Local DB connection failed (" + e.getMessage() + "). Falling back to H2 in-memory database.");
                url = "jdbc:h2:mem:clinicdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;NON_KEYWORDS=VALUE";
                driver = "org.h2.Driver";
                username = "sa";
                password = "";
                dataSource.setJdbcUrl(url);
                dataSource.setDriverClassName(driver);
                dataSource.setUsername(username);
                dataSource.setPassword(password);
            } else {
                throw new IllegalStateException("Clinic datasource is unreachable at startup: " + e.getMessage(), e);
            }
        }

        System.out.println("Configured Clinic DataSource URL: " + url);
        return dataSource;
    }

    @Primary
    @Bean(name = "clinicEntityManagerFactory")
    @DependsOn({"clinicFlyway"})
    public LocalContainerEntityManagerFactoryBean clinicEntityManagerFactory(
            @Qualifier("clinicDataSource") DataSource dataSource,
            org.springframework.core.env.Environment env) {
        
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPersistenceUnitName("clinic");
        em.setPackagesToScan(
                "com.healthcare.clinic.appointment.entity",
                "com.healthcare.clinic.audit.entity",
                "com.healthcare.clinic.backoffice.inventory.entity",
                "com.healthcare.clinic.billing.entity",
                "com.healthcare.clinic.branch.entity",
                "com.healthcare.clinic.clinicaldecision.entity",
                "com.healthcare.clinic.department.entity",
                "com.healthcare.clinic.doctor.entity",
                "com.healthcare.clinic.doctor.medicine.entity",
                "com.healthcare.clinic.document.entity",
                "com.healthcare.clinic.emergency.entity",
                "com.healthcare.clinic.emr.entity",
                "com.healthcare.clinic.finance.entity",
                "com.healthcare.clinic.homevisit.entity",
                "com.healthcare.clinic.identity.entity",
                "com.healthcare.clinic.inpatient.entity",
                "com.healthcare.clinic.insurance.entity",
                "com.healthcare.clinic.integration.entity",
                "com.healthcare.clinic.laboratory.entity",
                "com.healthcare.clinic.medicalrecord.entity",
                "com.healthcare.clinic.notification.entity",
                "com.healthcare.clinic.nursing.entity",
                "com.healthcare.clinic.patient.entity",
                "com.healthcare.clinic.radiology.entity",
                "com.healthcare.clinic.reception.entity",
                "com.healthcare.clinic.superadmin.entity",
                "com.healthcare.clinic.support.entity",
                "com.healthcare.clinic.surgery.entity",
                "com.healthcare.clinic.tenant.entity"
        );

        em.setJpaVendorAdapter(new org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter());
        
        java.util.HashMap<String, Object> properties = new java.util.HashMap<>();
        String dbUrl = env.getProperty("app.datasource.clinic.url", "");
        if (dbUrl.contains("h2")) {
            properties.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        } else {
            properties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        }
        String ddlAuto = env.getProperty("spring.jpa.hibernate.ddl-auto", "validate");
        properties.put("hibernate.hbm2ddl.auto", ddlAuto);
        properties.put("hibernate.physical_naming_strategy", "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy");
        properties.put("hibernate.bytecode.use_reflection_optimizer", "true");
        em.setJpaPropertyMap(properties);
        
        return em;
    }

    @Primary
    @Bean(name = "clinicTransactionManager")
    public PlatformTransactionManager clinicTransactionManager(
            @Qualifier("clinicEntityManagerFactory") EntityManagerFactory clinicEntityManagerFactory) {
        return new JpaTransactionManager(clinicEntityManagerFactory);
    }
}
