package org.grails.datastore.gorm.jcr

import org.junit.runner.RunWith
import org.junit.runners.Suite
import org.junit.runners.Suite.SuiteClasses

import grails.gorm.tests.CrudOperationsSpec
import grails.gorm.tests.GormEnhancerSpec
import grails.gorm.tests.ProxyLoadingSpec
import grails.gorm.tests.FindByMethodSpec
import grails.gorm.tests.DomainEventsSpec
import grails.gorm.tests.QueryAfterPropertyChangeSpec
import grails.gorm.tests.GroovyProxySpec
import grails.gorm.tests.CriteriaBuilderSpec
import grails.gorm.tests.CommonTypesPersistenceSpec
import grails.gorm.tests.CircularOneToManySpec
import grails.gorm.tests.InheritanceSpec
import grails.gorm.tests.ListOrderBySpec
import grails.gorm.tests.OrderBySpec
import grails.gorm.tests.ValidationSpec
import grails.gorm.tests.UpdateWithProxyPresentSpec
import grails.gorm.tests.AttachMethodSpec
import grails.gorm.tests.WithTransactionSpec
import grails.gorm.tests.RangeQuerySpec
import grails.gorm.tests.NamedQuerySpec
import grails.gorm.tests.OneToManySpec
import grails.gorm.tests.SaveAllSpec
import grails.gorm.tests.NegationSpec

/**
 * @author Erawat
 */
@RunWith(Suite)
@SuiteClasses([
//  DomainEventsSpec, // not yet passed
//  ProxyLoadingSpec, // passed
//  QueryAfterPropertyChangeSpec,
//  CircularOneToManySpec, // StackOverflow
//  InheritanceSpec, // still doesn't support inheritance
//  FindByMethodSpec, // passed
//  ListOrderBySpec, // passed
//  GroovyProxySpec, // passed
//  CommonTypesPersistenceSpec, // passed
//  GormEnhancerSpec, //passed
//  CriteriaBuilderSpec, //passed
//  NegationSpec, //passes
 NamedQuerySpec,
//  OrderBySpec, //passed
//  RangeQuerySpec,
//  ValidationSpec,
//  UpdateWithProxyPresentSpec,
//  AttachMethodSpec, // passed
//  WithTransactionSpec,
//  CrudOperationsSpec // passed
//  OneToManySpec
//  SaveAllSpec // passed
])
class JcrTestSuite {
}
