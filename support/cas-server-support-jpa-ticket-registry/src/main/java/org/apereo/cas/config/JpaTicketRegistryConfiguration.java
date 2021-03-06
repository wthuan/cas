package org.apereo.cas.config;

import org.apache.commons.lang3.StringUtils;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.configuration.model.core.ticket.registry.TicketRegistryProperties;
import org.apereo.cas.configuration.model.support.jpa.JpaConfigDataHolder;
import org.apereo.cas.configuration.model.support.jpa.ticketregistry.JpaTicketRegistryProperties;
import org.apereo.cas.configuration.support.Beans;
import org.apereo.cas.ticket.TicketMetadataCatalog;
import org.apereo.cas.ticket.registry.JpaTicketRegistry;
import org.apereo.cas.ticket.registry.TicketRegistry;
import org.apereo.cas.ticket.registry.support.JpaLockingStrategy;
import org.apereo.cas.ticket.registry.support.LockingStrategy;
import org.apereo.cas.util.InetAddressUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

/**
 * This this {@link JpaTicketRegistryConfiguration}.
 *
 * @author Misagh Moayyed
 * @since 5.0.0
 */
@Configuration("jpaTicketRegistryConfiguration")
@EnableConfigurationProperties(CasConfigurationProperties.class)
@EnableTransactionManagement(proxyTargetClass = true)
public class JpaTicketRegistryConfiguration {

    @Autowired
    private CasConfigurationProperties casProperties;

    @Bean
    public String[] ticketPackagesToScan() {
        return new String[]{
                "org.apereo.cas.ticket",
                "org.apereo.cas.adaptors.jdbc"
        };
    }
    
    @Lazy
    @Bean
    public LocalContainerEntityManagerFactoryBean ticketEntityManagerFactory() {
        return Beans.newHibernateEntityManagerFactoryBean(
                new JpaConfigDataHolder(
                        Beans.newHibernateJpaVendorAdapter(casProperties.getJdbc()),
                        "jpaTicketRegistryContext",
                        ticketPackagesToScan(),
                        dataSourceTicket()),
                casProperties.getTicket().getRegistry().getJpa());
    }

    @Bean
    public PlatformTransactionManager ticketTransactionManager(@Qualifier("ticketEntityManagerFactory") final EntityManagerFactory emf) {
        final JpaTransactionManager mgmr = new JpaTransactionManager();
        mgmr.setEntityManagerFactory(emf);
        return mgmr;
    }

    @RefreshScope
    @Bean
    public DataSource dataSourceTicket() {
        return Beans.newHickariDataSource(casProperties.getTicket().getRegistry().getJpa());
    }

    @Bean(name = {"jpaTicketRegistry", "ticketRegistry"})
    @RefreshScope
    public TicketRegistry jpaTicketRegistry(@Qualifier("ticketMetadataCatalog")
                                            final TicketMetadataCatalog ticketMetadataCatalog) {
        final JpaTicketRegistryProperties jpa = casProperties.getTicket().getRegistry().getJpa();
        final JpaTicketRegistry bean = new JpaTicketRegistry(jpa.getTicketLockType(), ticketMetadataCatalog);
        bean.setCipherExecutor(Beans.newTicketRegistryCipherExecutor(jpa.getCrypto()));
        return bean;
    }

    @Bean
    public LockingStrategy lockingStrategy() {
        final TicketRegistryProperties registry = casProperties.getTicket().getRegistry();
        final String uniqueId = StringUtils.defaultIfEmpty(casProperties.getHost().getName(), InetAddressUtils.getCasServerHostName());
        return new JpaLockingStrategy(registry.getCleaner().getAppId(), uniqueId, registry.getJpa().getJpaLockingTimeout());
    }
}
