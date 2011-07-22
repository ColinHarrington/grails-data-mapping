package grails.gorm.tests

class ClassWithListArgBeforeValidate implements Serializable {
    Long id
    Long version
    def listArgCounter = 0
    def propertiesPassedToBeforeValidate
    String name

    def beforeValidate(List properties) {
        ++listArgCounter
        propertiesPassedToBeforeValidate = properties
    }

    static constraints = {
        name blank: false
    }
}