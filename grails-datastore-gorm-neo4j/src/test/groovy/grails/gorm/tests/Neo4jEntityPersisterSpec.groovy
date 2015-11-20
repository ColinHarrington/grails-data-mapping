package grails.gorm.tests

import org.grails.datastore.gorm.neo4j.engine.Neo4jEntityPersister
import org.grails.datastore.mapping.model.MappingContext
import spock.lang.Specification

/**
 * @author Stefan Armbruster
 */
class Neo4jEntityPersisterSpec extends Specification {

    def "verify singular detection"() {
        setup:
        def cut = new Neo4jEntityPersister(Mock(MappingContext), null, null, null)

        expect:
        cut.isSingular(term) == result

        where:
        term       | result
        "word"     | true
        "words"    | false
        "friend"   | true
        "friends"  | false
        "buddy"    | true
        "buddies"  | false
        "child"    | true
        "children" | false
        "person"   | true
        //"people"   | false
    }
}
