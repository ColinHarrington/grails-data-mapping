package grails.gorm.tests

import grails.gorm.JpaEntity

@JpaEntity
class Location implements Serializable {
    String name
    String code

    def namedAndCode() {
        "$name - $code"
    }

    static mapping = {
        name index:true
        code index:true
    }
}
