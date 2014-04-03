package org.grails.datastore.gorm.mongo

import grails.gorm.tests.GormDatastoreSpec
import grails.mongodb.geo.Box
import grails.mongodb.geo.Circle
import grails.mongodb.geo.LineString
import grails.mongodb.geo.Metric
import grails.mongodb.geo.Point
import grails.mongodb.geo.Polygon
import grails.mongodb.geo.Shape
import grails.mongodb.geo.Sphere
import grails.persistence.Entity

/**
 * Created by graemerocher on 17/03/14.
 */
class GeoJSONTypePersistenceSpec extends GormDatastoreSpec {


    void "Test persist GeoJSON types"() {
        given:"A domain with GeoJSON types"

            def point = new Point(5, 10)
            def poly = Polygon.valueOf([ [100.0, 0.0], [101.0, 0.0], [101.0, 1.0], [100.0, 1.0], [100.0, 0.0] ])
            def line = LineString.valueOf([ [100.0, 0.0], [101.0, 1.0] ])
            def box = Box.valueOf([[0, 0], [10, 10]])
            def circle = Circle.valueOf([[5,5], 3])

            def p = new Place(point: point,
                              polygon: poly,
                              lineString: line,
                              box: box,
                              circle: circle)

        when:"the entity is persisted and retrieved"
            p.save(flush:true)
            session.clear()
            p = Place.get(p.id)

        then:"The GeoJSON types are correctly loaded"
            p.point == point
            p.polygon == poly
            p.lineString == line
            p.box == box
            p.circle == circle
    }

    void "Test geoWithin dynamic finder"() {
        given:"A domain with GeoJSON types"
            def point = new Point(2, 1)
            // Order: bottom left, top left, top right, bottom right, bottom left
            def poly1 = Polygon.valueOf([ [0.0, 0.0], [3.0, 0.0], [3.0, 3.0], [0.0, 3.0], [0.0, 0.0] ])
            def poly2 = Polygon.valueOf([ [5.0, 5.0], [7.0, 5.0], [7.0, 7.0], [5.0, 7.0], [5.0, 5.0] ])
            def p = new Place(point: point)
            p.save(flush:true)

        expect:"A geoWithin query is executed to find a point within"
            Place.findByPointGeoWithin(poly1)
            !Place.findByPointGeoWithin(poly2)
            Place.findByPointGeoWithin( Box.valueOf( [[0.0d, 0.0d], [5.0d, 5.0d]] ) )
            !Place.findByPointGeoWithin( Box.valueOf( [[5.0d, 5.0d], [10.0d, 10.0d]] ) )
            Place.findByPointGeoWithin( Circle.valueOf( [[1.0d, 1.0d], 3.0d] ) )
            !Place.findByPointGeoWithin( Circle.valueOf( [[10.0d, 10.0d], 3.0d] ) )
            Place.findByPointGeoWithin( Sphere.valueOf( [[1.0d, 1.0d], 0.06]) )
            !Place.findByPointGeoWithin( Sphere.valueOf( [[10.0d, 10.0d], 0.06] ) )
            Place.findByPoint(point)
            Place.findByPointGeoWithin([ '$polygon': [ [0.0d, 0.0d], [3.0d, 0.0d], [3.0d, 3.0d], [0.0d, 3.0d], [0.0d, 0.0d] ] ])

    }

    void "Test persist abstract shape type"() {
        given:"A data model based on https://blog.codecentric.de/en/2013/03/mongodb-geospatial-indexing-search-geojson-point-linestring-polygon/"
            createGeoDataModel()


        expect:
            Loc.count() == 4
            Loc.findByName("P1").shape instanceof Point
            Loc.findByName("P2").shape instanceof Point
            Loc.findByName("Poly1").shape instanceof Polygon
            Loc.findByName("LS1").shape instanceof LineString
            Loc.findByShape(Point.valueOf(2, 2))
            !Loc.findByShape(Point.valueOf(20, 10))
    }


    void "Test geoIntersects dynamic finder"() {
        given:"A geo data model"
            createGeoDataModel()

        expect:
            Loc.findByShapeGeoIntersects( Polygon.valueOf( [[ [0,0], [3,0], [3,3], [0,3], [0,0] ]] ) )
            Loc.findByShapeGeoIntersects( LineString.valueOf( [[1,4], [8,4]] ) )
            !Loc.findByShapeGeoIntersects( LineString.valueOf( [[1,7], [8,7]] ) )
            Loc.findByShapeGeoIntersects( ['$geometry':[type:'Polygon', coordinates: [[ [0,0], [3,0], [3,3], [0,3], [0,0] ]]]] )
            !Loc.findByShapeGeoIntersects( ['$geometry':[type:'LineString', coordinates: [[1,7], [8,7]] ]] )
    }

    void "Test near queries with GeoJSON types"() {
        given:"A geo data model"
            createGeoDataModel()

        when:"We find points near a given point"
            def results = Loc.findAllByShapeNear( Point.valueOf(1,7) )

        then:"The results are correct"
            results.size() == 4
            results*.name == ['P2', 'Poly1', 'P1', 'LS1']

        when:"We find points near a given point"
            results = Loc.findAllByShapeNear( [$geometry: [type:'Point', coordinates: [1,7]]] )

        then:"The results are correct"
            results.size() == 4
            results*.name == ['P2', 'Poly1', 'P1', 'LS1']

        when:"We find points near a given point"
            results = Loc.withCriteria {
                near 'shape', Point.valueOf(1,7), 300000
            }

        then:"The results are correct"
            results.size() == 1
            results*.name == ['P2']

        when:"We look for points near a given point that are too far away"
        results = Loc.withCriteria {
            near 'shape', Point.valueOf(1,7), 1
        }

        then:"The results are correct"
        results.size() == 0
    }


    void "Test nearSphere queries with GeoJSON types"() {
        given:"A geo data model"
            createGeoDataModel()

        when:"We find points near a given point"
            def results = Loc.findAllByShapeNearSphere( Point.valueOf(1,7) )

        then:"The results are correct"
            results.size() == 4
            results*.name == ['P2', 'Poly1', 'P1', 'LS1']

        when:"We find points near a given point"
            results = Loc.findAllByShapeNearSphere( [$geometry: [type:'Point', coordinates: [1,7]]] )

        then:"The results are correct"
            results.size() == 4
            results*.name == ['P2', 'Poly1', 'P1', 'LS1']

        when:"We find points near a given point"
            results = Loc.withCriteria {
                nearSphere 'shape', Point.valueOf(1,7), 300000
            }

        then:"The results are correct"
            results.size() == 1
            results*.name == ['P2']
    }


    /**
     * Creates a data model based on A data model based on
     * https://blog.codecentric.de/en/2013/03/mongodb-geospatial-indexing-search-geojson-point-linestring-polygon
     */
    protected void createGeoDataModel() {
        def p1 = Point.valueOf(2, 2)
        def p2 = Point.valueOf(3, 6)
        def poly1 = Polygon.valueOf([[[3, 1], [1, 2], [5, 6], [9, 2], [4, 3], [3, 1]]])
        def line1 = LineString.valueOf([[5, 2], [7, 3], [7, 5], [9, 4]])

        new Loc(name: 'P1', shape: p1).save(flush: true)
        new Loc(name: 'P2', shape: p2).save(flush: true)
        new Loc(name: 'Poly1', shape: poly1).save(flush: true)
        new Loc(name: 'LS1', shape: line1).save(flush: true)
        session.clear()
    }

    @Override
    List getDomainClasses() {
        [Place, Loc]
    }
}

@Entity
class Place {
    Long id
    String name
    Point point
    Polygon polygon
    LineString lineString
    Box box
    Circle circle
    Sphere sphere

    static mapping = {
        point geoIndex:'2dsphere'
    }
}

@Entity
class Loc {
    Long id
    String name
    Shape shape

    static mapping = {
        shape geoIndex:'2dsphere'
    }

    @Override
    String toString() {
        name
    }
}