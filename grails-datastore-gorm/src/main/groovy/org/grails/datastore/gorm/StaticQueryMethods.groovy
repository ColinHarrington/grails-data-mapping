/*
 * Copyright 2014 the original author or authors.
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
package org.grails.datastore.gorm

import grails.gorm.DetachedCriteria
import org.grails.datastore.gorm.async.GormAsyncStaticApi
import org.grails.datastore.mapping.query.api.BuildableCriteria
import org.grails.datastore.mapping.query.api.Criteria
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.DefaultTransactionDefinition
import org.springframework.validation.Errors

/**
 * 
 * @author Jeff Brown
 * @since 4.0
 */
trait StaticQueryMethods<D> {

    private static GormStaticApi<D> internalStaticApi

    static void initInternalStaticApi(GormStaticApi<D> api) {
        internalStaticApi = api
    }

    /**
     *
     * @param callable Callable closure containing detached criteria definition
     * @return The DetachedCriteria instance
     */
    static DetachedCriteria<D> where(Closure callable) {
        internalStaticApi.where callable
    }

    /**
     *
     * @param callable Callable closure containing detached criteria definition
     * @return The DetachedCriteria instance that is lazily initialized
     */
    static DetachedCriteria<D> whereLazy(Closure callable) {
        internalStaticApi.whereLazy callable
    }
    
    /**
     *
     * @param callable Callable closure containing detached criteria definition
     * @return The DetachedCriteria instance
     */
    static DetachedCriteria<D> whereAny(Closure callable) {
        internalStaticApi.whereAny callable
    }
    
    /**
     * Uses detached criteria to build a query and then execute it returning a list
     *
     * @param callable The callable
     * @return A List of entities
     */
    static List<D> findAll(Closure callable) {
        internalStaticApi.findAll callable
    }

    /**
     * Uses detached criteria to build a query and then execute it returning a list
     *
     * @param args pagination parameters
     * @param callable The callable
     * @return A List of entities
     */
    static List<D> findAll(Map args, Closure callable) {
        internalStaticApi.findAll args, callable
    }

    /**
     * Uses detached criteria to build a query and then execute it returning a list
     *
     * @param callable The callable
     * @return A single entity
     */
    static D find(Closure callable) {
        internalStaticApi.find callable
    }


    /**
     * Saves a list of objects in one go
     * @param objectsToSave The objects to save
     * @return A list of object identifiers
     */
    static List<Serializable> saveAll(Object... objectsToSave) {
        internalStaticApi.saveAll objectsToSave
    }

    /**
     * Saves a list of objects in one go
     * @param objectToSave Collection of objects to save
     * @return A list of object identifiers
     */
    static List<Serializable> saveAll(Iterable<?> objectsToSave) {
        internalStaticApi.saveAll objectsToSave
    }

    /**
     * Deletes a list of objects in one go
     * @param objectsToDelete The objects to delete
     */
    static void deleteAll(Object... objectsToDelete) {
        internalStaticApi.deleteAll objectsToDelete
    }

    /**
     * Deletes a list of objects in one go
     * @param objectsToDelete Collection of objects to delete
     */
    static void deleteAll(Iterable objectToDelete) {
        internalStaticApi.deleteAll objectToDelete
    }

    /**
     * Creates an instance of this class
     * @return The created instance
     */
    static D create() {
        internalStaticApi.create()
    }

    /**
     * Retrieves and object from the datastore. eg. Book.get(1)
     */
    static D get(Serializable id) {
        internalStaticApi.get id
    }

    /**
     * Retrieves and object from the datastore. eg. Book.read(1)
     *
     * Since the datastore abstraction doesn't support dirty checking yet this
     * just delegates to {@link #get(Serializable)}
     */
    static D read(Serializable id) {
        internalStaticApi.read id
    }

    /**
     * Retrieves and object from the datastore as a proxy. eg. Book.load(1)
     */
    static D load(Serializable id) {
        internalStaticApi.load id
    }

    /**
     * Retrieves and object from the datastore as a proxy. eg. Book.proxy(1)
     */
    static D proxy(Serializable id) {
        internalStaticApi.proxy id
    }

    /**
     * Retrieve all the objects for the given identifiers
     * @param ids The identifiers to operate against
     * @return A list of identifiers
     */
    static List<D> getAll(Iterable<Serializable> ids) {
        internalStaticApi.getAll ids
    }

    /**
     * Retrieve all the objects for the given identifiers
     * @param ids The identifiers to operate against
     * @return A list of identifiers
     */
    static List<D> getAll(Serializable... ids) {
        internalStaticApi.getAll ids
    }

    /**
     * @return The async version of the GORM static API
     */
    static GormAsyncStaticApi<D> getAsync() {
        internalStaticApi.getAsync()
    }

    /**
     * @return Synonym for {@link #list()}
     */
    static List<D> getAll() {
        list()
    }

    /**
     * Creates a criteria builder instance
     */
    static BuildableCriteria createCriteria() {
        internalStaticApi.createCriteria()
    }

    /**
     * Creates a criteria builder instance
     */
    static withCriteria(@DelegatesTo(Criteria) Closure callable) {
        internalStaticApi.withCriteria callable
    }

    /**
     * Creates a criteria builder instance
     */
    static withCriteria(Map builderArgs, @DelegatesTo(Criteria) Closure callable) {
        internalStaticApi.withCriteria builderArgs, callable
    }

    /**
     * Locks an instance for an update
     * @param id The identifier
     * @return The instance
     */
    static D lock(Serializable id) {
        internalStaticApi.lock id
    }

    /**
     * Merges an instance with the current session
     * @param d The object to merge
     * @return The instance
     */
    static D merge(D d) {
        internalStaticApi.merge d
    }

    /**
     * Counts the number of persisted entities
     * @return The number of persisted entities
     */
    static Integer count() {
        internalStaticApi.count()
    }

    /**
     * Same as {@link #count()} but allows property-style syntax (Foo.count)
     */
    static Integer getCount() {
        internalStaticApi.getCount()
    }

    /**
     * Checks whether an entity exists
     */
    static boolean exists(Serializable id) {
        internalStaticApi.exists id
    }

    /**
     * Lists objects in the datastore. eg. Book.list(max:10)
     *
     * @param params Any parameters such as offset, max etc.
     * @return A list of results
     */
    static List<D> list(Map params) {
        internalStaticApi.list params
    }

    /**
     * List all entities
     *
     * @return The list of all entities
     */
    static List<D> list() {
        internalStaticApi.list()
    }

    /**
     * The same as {@link #list()}
     *
     * @return The list of all entities
     */
    static List<D> findAll(Map params = Collections.emptyMap()) {
        internalStaticApi.findAll params
    }

    /**
     * Finds an object by example
     *
     * @param example The example
     * @return A list of matching results
     */
    static List<D> findAll(D example) {
        internalStaticApi.findAll example
    }

    /**
     * Finds an object by example using the given arguments for pagination
     *
     * @param example The example
     * @param args The arguments
     *
     * @return A list of matching results
     */
    static List<D> findAll(D example, Map args) {
        internalStaticApi.findAll example, args
    }

    /**
     * Finds the first object using the natural sort order
     *
     * @return the first object in the datastore, null if none exist
     */
    static D first() {
        internalStaticApi.first()
    }

    /**
     * Finds the first object sorted by propertyName
     *
     * @param propertyName the name of the property to sort by
     *
     * @return the first object in the datastore sorted by propertyName, null if none exist
     */
    static D first(String propertyName) {
        internalStaticApi.first propertyName
    }

    /**
     * Finds the first object.  If queryParams includes 'sort', that will
     * dictate the sort order, otherwise natural sort order will be used.
     * queryParams may include any of the same parameters that might be passed
     * to the list(Map) method.  This method will ignore 'order' and 'max' as
     * those are always 'asc' and 1, respectively.
     *
     * @return the first object in the datastore, null if none exist
     */
    static D first(Map queryParams) {
        internalStaticApi.first queryParams
    }

    /**
     * Finds the last object using the natural sort order
     *
     * @return the last object in the datastore, null if none exist
     */
    static D last() {
        internalStaticApi.last()
    }

    /**
     * Finds the last object sorted by propertyName
     *
     * @param propertyName the name of the property to sort by
     *
     * @return the last object in the datastore sorted by propertyName, null if none exist
     */
    static D last(String propertyName) {
        internalStaticApi.last propertyName
    }

    /**
     * Finds the last object.  If queryParams includes 'sort', that will
     * dictate the sort order, otherwise natural sort order will be used.
     * queryParams may include any of the same parameters that might be passed
     * to the list(Map) method.  This method will ignore 'order' and 'max' as
     * those are always 'asc' and 1, respectively.
     *
     * @return the last object in the datastore, null if none exist
     */
    static D last(Map queryParams) {
        internalStaticApi.last queryParams
    }

    /**
     * Finds all results matching all of the given conditions. Eg. Book.findAllWhere(author:"Stephen King", title:"The Stand")
     *
     * @param queryMap The map of conditions
     * @return A list of results
     */
    static List<D> findAllWhere(Map queryMap) {
        internalStaticApi.findAllWhere queryMap
    }

    /**
     * Finds all results matching all of the given conditions. Eg. Book.findAllWhere(author:"Stephen King", title:"The Stand")
     *
     * @param queryMap The map of conditions
     * @param args The Query arguments
     *
     * @return A list of results
     */
    static List<D> findAllWhere(Map queryMap, Map args) {
        internalStaticApi.findAllWhere queryMap, args
    }

    /**
     * Finds an object by example
     *
     * @param example The example
     * @return A list of matching results
     */
    static D find(D example) {
        internalStaticApi.find example
    }

    /**
     * Finds an object by example using the given arguments for pagination
     *
     * @param example The example
     * @param args The arguments
     *
     * @return A list of matching results
     */
    static D find(D example, Map args) {
        internalStaticApi.find example, args
    }

    /**
     * Finds a single result matching all of the given conditions. Eg. Book.findWhere(author:"Stephen King", title:"The Stand")
     *
     * @param queryMap The map of conditions
     * @return A single result
     */
    static D findWhere(Map queryMap) {
        internalStaticApi.findWhere queryMap
    }

    /**
     * Finds a single result matching all of the given conditions. Eg. Book.findWhere(author:"Stephen King", title:"The Stand")
     *
     * @param queryMap The map of conditions
     * @param args The Query arguments
     *
     * @return A single result
     */
    static D findWhere(Map queryMap, Map args) {
        internalStaticApi.findWhere queryMap, args
    }

    /**
     * Finds a single result matching all of the given conditions. Eg. Book.findWhere(author:"Stephen King", title:"The Stand").  If
     * a matching persistent entity is not found a new entity is created and returned.
     *
     * @param queryMap The map of conditions
     * @return A single result
     */
    static D findOrCreateWhere(Map queryMap) {
        internalStaticApi.findOrCreateWhere queryMap
    }

    /**
     * Finds a single result matching all of the given conditions. Eg. Book.findWhere(author:"Stephen King", title:"The Stand").  If
     * a matching persistent entity is not found a new entity is created, saved and returned.
     *
     * @param queryMap The map of conditions
     * @return A single result
     */
    static D findOrSaveWhere(Map queryMap) {
        internalStaticApi.findOrSaveWhere queryMap
    }

    /**
     * Execute a closure whose first argument is a reference to the current session.
     *
     * @param callable the closure
     * @return The result of the closure
     */
    static withSession(Closure callable) {
        internalStaticApi.withSession callable
    }

    /**
     * Same as withSession, but present for the case where withSession is overridden to use the Hibernate session
     *
     * @param callable the closure
     * @return The result of the closure
     */
    static withDatastoreSession(Closure callable) {
        internalStaticApi.withDatastoreSession callable
    }

    /**
     * Executes the closure within the context of a transaction, creating one if none is present or joining
     * an existing transaction if one is already present.
     *
     * @param callable The closure to call
     * @return The result of the closure execution
     * @see #withTransaction(Map, Closure)
     * @see #withNewTransaction(Closure)
     * @see #withNewTransaction(Map, Closure)
     */
    static withTransaction(Closure callable) {
        internalStaticApi.withTransaction callable
    }

    /**
     * Executes the closure within the context of a new transaction
     *
     * @param callable The closure to call
     * @return The result of the closure execution
     * @see #withTransaction(Closure)
     * @see #withTransaction(Map, Closure)
     * @see #withNewTransaction(Map, Closure)
     */
    static withNewTransaction(Closure callable) {
        internalStaticApi.withNewTransaction callable
    }

    /**
     * Executes the closure within the context of a transaction which is
     * configured with the properties contained in transactionProperties.
     * transactionProperties may contain any properties supported by
     * {@link DefaultTransactionDefinition}.
     *
     * <blockquote>
     * <pre>
     * SomeEntity.withTransaction([propagationBehavior: TransactionDefinition.PROPAGATION_REQUIRES_NEW,
     *                             isolationLevel: TransactionDefinition.ISOLATION_REPEATABLE_READ]) {
     *     // ...
     * }
     * </pre>
     * </blockquote>
     *
     * @param transactionProperties properties to configure the transaction properties
     * @param callable The closure to call
     * @return The result of the closure execution
     * @see DefaultTransactionDefinition
     * @see #withNewTransaction(Closure)
     * @see #withNewTransaction(Map, Closure)
     * @see #withTransaction(Closure)
     */
    static withTransaction(Map transactionProperties, Closure callable) {
        internalStaticApi.withTransaction transactionProperties, callable
    }

    /**
     * Executes the closure within the context of a new transaction which is
     * configured with the properties contained in transactionProperties.
     * transactionProperties may contain any properties supported by
     * {@link DefaultTransactionDefinition}.  Note that if transactionProperties
     * includes entries for propagationBehavior or propagationName, those values
     * will be ignored.  This method always sets the propagation level to
     * TransactionDefinition.REQUIRES_NEW.
     *
     * <blockquote>
     * <pre>
     * SomeEntity.withNewTransaction([isolationLevel: TransactionDefinition.ISOLATION_REPEATABLE_READ]) {
     *     // ...
     * }
     * </pre>
     * </blockquote>
     *
     * @param transactionProperties properties to configure the transaction properties
     * @param callable The closure to call
     * @return The result of the closure execution
     * @see DefaultTransactionDefinition
     * @see #withNewTransaction(Closure)
     * @see #withTransaction(Closure)
     * @see #withTransaction(Map, Closure)
     */
    static withNewTransaction(Map transactionProperties, Closure callable) {
        internalStaticApi.withNewTransaction transactionProperties, callable
    }

    /**
     * Executes the closure within the context of a transaction for the given {@link TransactionDefinition}
     *
     * @param callable The closure to call
     * @return The result of the closure execution
     */
    static withTransaction(TransactionDefinition definition, Closure callable) {
        internalStaticApi.withTransaction definition, callable
    }

    /**
     * Creates and binds a new session for the scope of the given closure
     */
    static withNewSession(Closure callable) {
        internalStaticApi.withNewSession callable
    }

    /**
     * Creates and binds a new session for the scope of the given closure
     */
    static withStatelessSession(Closure callable) {
        internalStaticApi.withStatelessSession callable
    }

    /**
     * Get the thread-local map used to store Errors when validating.
     * @return the map
     */
    static Map<D, Errors> getValidationErrorsMap() {
        internalStaticApi.getValidationErrorsMap()
    }

    /**
     * Get the thread-local map used to store whether to skip validation.
     * @return the map
     */
    static Map<D, Boolean> getValidationSkipMap() {
        internalStaticApi.getValidationSkipMap()
    }

    static List<D> executeQuery(String query) {
        internalStaticApi.executeQuery query
    }

    static List<D> executeQuery(String query, Map args) {
        internalStaticApi.executeQuery query, args
    }

    static List<D> executeQuery(String query, Map params, Map args) {
        internalStaticApi.executeQuery query, params, args
    }

    static List<D> executeQuery(String query, Collection params) {
        internalStaticApi.executeQuery query, params
    }

    static List<D> executeQuery(String query, Object...params) {
        internalStaticApi.executeQuery query, params
    }

    static List<D> executeQuery(String query, Collection params, Map args) {
        internalStaticApi.executeQuery query, params, args
    }

    static Integer executeUpdate(String query) {
        internalStaticApi.executeUpdate query
    }

    static Integer executeUpdate(String query, Map args) {
        internalStaticApi.executeUpdate query, args
    }

    static Integer executeUpdate(String query, Map params, Map args) {
        internalStaticApi.executeUpdate query, params, args
    }

    static Integer executeUpdate(String query, Collection params) {
        internalStaticApi.executeUpdate query, params
    }

    static Integer executeUpdate(String query, Object...params) {
        internalStaticApi.executeUpdate query, params
    }

    static Integer executeUpdate(String query, Collection params, Map args) {
        internalStaticApi.executeUpdate query, params, args
    }

    static D find(String query) {
        internalStaticApi.find query
    }

    static D find(String query, Map args) {
        internalStaticApi.find query, args
    }

    static D find(String query, Map params, Map args) {
        internalStaticApi.find query, params, args
    }

    static D find(String query, Collection params) {
        internalStaticApi.find query, params
    }

    static D find(String query, Object[] params) {
        internalStaticApi.find query, params
    }

    static D find(String query, Collection params, Map args) {
        internalStaticApi.find query, params, args
    }

    static List<D> findAll(String query) {
        internalStaticApi.findAll query
    }

    static List<D> findAll(String query, Map params) {
        internalStaticApi.findAll query, params
    }

    static List<D> findAll(String query, Map params, Map args) {
        internalStaticApi.findAll query, params, args
    }

    static List<D> findAll(String query, Collection params) {
        internalStaticApi.findAll query, params
    }

    static List<D> findAll(String query, Object[] params) {
        internalStaticApi.findAll query, params
    }

    static List<D> findAll(String query, Collection params, Map args) {
        internalStaticApi.findAll query, params, args
    }
}
