package org.grails.datastore.gorm.plugin.support

import grails.persistence.Entity
import grails.validation.ValidationException
import grails.validation.ConstrainedProperty
import org.codehaus.groovy.grails.commons.GrailsDomainConfigurationUtil
import org.grails.datastore.mapping.core.Datastore
import org.grails.datastore.mapping.simple.SimpleMapDatastore
import org.grails.datastore.mapping.transactions.DatastoreTransactionManager
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.validation.Errors
import org.springframework.validation.Validator

import spock.lang.Specification
import spock.lang.Ignore

/**
 *
 */
class DynamicMethodsConfigurerSpec extends Specification{

      void cleanup() {
          GroovySystem.metaClassRegistry.removeMetaClass(Simple)
      }

      void "Test dynamic methods are configured correctly with no existing datastore"() {
          given:"A dynamic methods configurer with a single domain class"
            final configurer = systemUnderTest()
            configurer.datastore.mappingContext.addPersistentEntity(Simple)

          when:"Dynamic methods are configured"
            configurer.configure()

          then:"Dynamic methods can be called on the domain"
            Simple.count() == 0
      }

      @Ignore
      void "Test dynamic methods are configured correctly with an existing datastore"() {
          given:"A dynamic methods configurer with a single domain class"
            final configurer = systemUnderTest()
            configurer.hasExistingDatastore = true
            configurer.datastore.mappingContext.addPersistentEntity(Simple)

          when:"Dynamic methods are configured"
            configurer.configure()

          then:"Dynamic methods are scoped to the datastore"
            Simple.simple.findAll().size() == 0

          when:"And are not included at the top level"
            Simple.findAll()

          then:"A missing method exception is thrown"
             thrown(MissingMethodException)
      }

      void "Test that failOnError can be activated"() {
          given:"A dynamic methods configurer with a single domain class and failOnError activated"
              final configurer = systemUnderTest()
              configurer.failOnError = true
              def entity = configurer.datastore.mappingContext.addPersistentEntity(Simple)
              configurer.datastore.mappingContext.addEntityValidator(entity, [
                      supports:{java.lang.Class c -> true},
                      validate:{target, Errors errors->
                          def constrainedProperties = GrailsDomainConfigurationUtil.evaluateConstraints(Simple)
                          for (ConstrainedProperty cp in constrainedProperties.values()) {
                              cp.validate(target, target[cp.propertyName], errors)
                          }
                      }
              ] as Validator)

          when:"Dynamic methods are configured"
              configurer.configure()
              new Simple(name: "").save()

          then:"Dynamic methods can be called on the domain"
              thrown(ValidationException)
      }
      DynamicMethodsConfigurer systemUnderTest() {
          return new SimpleDynamicMethodsConfigurer(new SimpleMapDatastore(), new DatastoreTransactionManager())
      }
}
class SimpleDynamicMethodsConfigurer extends DynamicMethodsConfigurer {

    SimpleDynamicMethodsConfigurer(Datastore datastore, PlatformTransactionManager transactionManager) {
        super(datastore, transactionManager)
    }

    @Override
    String getDatastoreType() {
        return "Simple"
    }

}

@Entity
class Simple {
    Long id
    String name
    static constraints = {
        name blank:false
    }
}