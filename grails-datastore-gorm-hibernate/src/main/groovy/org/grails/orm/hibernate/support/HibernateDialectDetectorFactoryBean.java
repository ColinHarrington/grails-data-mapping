/*
 * Copyright 2004-2005 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grails.orm.hibernate.support;


import java.sql.Connection;
import java.util.Properties;

import javax.sql.DataSource;

import org.grails.orm.hibernate.exceptions.CouldNotDetermineHibernateDialectException;
import org.hibernate.HibernateException;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.resolver.DialectFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.MetaDataAccessException;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * @author Steven Devijver
 */
public class HibernateDialectDetectorFactoryBean implements FactoryBean<String>, InitializingBean {

    private DataSource dataSource;
    private Properties vendorNameDialectMappings;
    private String hibernateDialectClassName;
    private Dialect hibernateDialect;
    private Properties hibernateProperties = new Properties();

    public void setHibernateProperties(Properties hibernateProperties) {
        this.hibernateProperties = hibernateProperties;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void setVendorNameDialectMappings(Properties mappings) {
        vendorNameDialectMappings = mappings;
    }

    public String getObject() {
        return hibernateDialectClassName;
    }

    public Class<String> getObjectType() {
        return String.class;
    }

    public boolean isSingleton() {
        return true;
    }

    public void afterPropertiesSet() throws MetaDataAccessException {
        Assert.notNull(dataSource, "Data source is not set!");
        Assert.notNull(vendorNameDialectMappings, "Vendor name/dialect mappings are not set!");

        Connection connection = null;

        String dbName = (String)JdbcUtils.extractDatabaseMetaData(dataSource, "getDatabaseProductName");

        try {
            connection = DataSourceUtils.getConnection(dataSource);

            try {
                hibernateDialect = DialectFactory.buildDialect(hibernateProperties, connection);
                hibernateDialectClassName = hibernateDialect.getClass().getName();
            } catch (HibernateException e) {
                hibernateDialectClassName = vendorNameDialectMappings.getProperty(dbName);
            }

           if (!StringUtils.hasText(hibernateDialectClassName)) {
                throw new CouldNotDetermineHibernateDialectException(
                        "Could not determine Hibernate dialect for database name [" + dbName + "]!");
           }
        } finally {
            DataSourceUtils.releaseConnection(connection,dataSource);
        }

    }
}
