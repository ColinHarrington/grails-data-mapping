/* Copyright (C) 2010 SpringSource
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
package org.grails.datastore.mapping.mongo.engine;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mongodb.*;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.grails.datastore.mapping.core.IdentityGenerationException;
import org.grails.datastore.mapping.core.OptimisticLockingException;
import org.grails.datastore.mapping.core.SessionImplementor;
import org.grails.datastore.mapping.engine.AssociationIndexer;
import org.grails.datastore.mapping.engine.EntityAccess;
import org.grails.datastore.mapping.engine.NativeEntryEntityPersister;
import org.grails.datastore.mapping.engine.Persister;
import org.grails.datastore.mapping.engine.PropertyValueIndexer;
import org.grails.datastore.mapping.engine.internal.MappingUtils;
import org.grails.datastore.mapping.model.EmbeddedPersistentEntity;
import org.grails.datastore.mapping.model.MappingContext;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.model.PersistentProperty;
import org.grails.datastore.mapping.model.PropertyMapping;
import org.grails.datastore.mapping.model.config.GormProperties;
import org.grails.datastore.mapping.model.types.Association;
import org.grails.datastore.mapping.model.types.EmbeddedCollection;
import org.grails.datastore.mapping.model.types.Identity;
import org.grails.datastore.mapping.model.types.ManyToMany;
import org.grails.datastore.mapping.mongo.MongoDatastore;
import org.grails.datastore.mapping.mongo.MongoSession;
import org.grails.datastore.mapping.mongo.config.MongoAttribute;
import org.grails.datastore.mapping.mongo.config.MongoMappingContext;
import org.grails.datastore.mapping.mongo.query.MongoDocumentQuery;
import org.grails.datastore.mapping.mongo.query.MongoQuery;
import org.grails.datastore.mapping.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.mongodb.core.DbCallback;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * A {@link org.grails.datastore.mapping.engine.EntityPersister} implementation for the Mongo document store
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class MongoDocumentEntityPersister extends AbstractMongoObectEntityPersister<Document> {

    public static final ValueRetrievalStrategy<Document> VALUE_RETRIEVAL_STRATEGY = new ValueRetrievalStrategy<Document>() {
        @Override
        public Object getValue(Document document, String name) {
            return document.get(name);
        }

        @Override
        public void setValue(Document document, String name, Object value) {
            document.put(name, value);
        }
    };

    public MongoDocumentEntityPersister(MappingContext mappingContext, PersistentEntity entity,
                                MongoSession mongoSession, ApplicationEventPublisher publisher) {
        super(mappingContext, entity, mongoSession, publisher);
    }


    @Override
    protected void loadEmbeddedCollection(EmbeddedCollection embeddedCollection,
                                          EntityAccess ea, Object embeddedInstances, String propertyKey) {

        if(Map.class.isAssignableFrom(embeddedCollection.getType())) {
            if(embeddedInstances instanceof Document) {
                Map instances = new HashMap();
                Document embedded = (Document)embeddedInstances;
                for (String key : embedded.keySet()) {
                    Object o = embedded.get(key);
                    if(o instanceof Document) {
                        Document nativeEntry = (Document) o;
                        Object instance =
                                createObjectFromEmbeddedNativeEntry(embeddedCollection.getAssociatedEntity(), nativeEntry);
                        SessionImplementor<Document> si = (SessionImplementor<Document>)getSession();
                        si.cacheEntry(embeddedCollection.getAssociatedEntity(), createEmbeddedCacheEntryKey(instance), nativeEntry);
                        instances.put(key, instance);
                    }

                }
                ea.setProperty(embeddedCollection.getName(), instances);
            }
        }
        else {
            Collection<Object> instances = MappingUtils.createConcreteCollection(embeddedCollection.getType());

            if (embeddedInstances instanceof Collection) {
                Collection coll = (Collection)embeddedInstances;
                for (Object dbo : coll) {
                    if (dbo instanceof Document) {
                        Document nativeEntry = (Document)dbo;
                        Object instance =
                                createObjectFromEmbeddedNativeEntry(embeddedCollection.getAssociatedEntity(), nativeEntry);
                        SessionImplementor<Document> si = (SessionImplementor<Document>)getSession();
                        si.cacheEntry(embeddedCollection.getAssociatedEntity(), createEmbeddedCacheEntryKey(instance), nativeEntry);
                        instances.add(instance);
                    }
                }
            }

            ea.setProperty(embeddedCollection.getName(), instances);
        }
    }

    @Override
    protected boolean isEmbeddedEntry(Object entry) {
        return entry instanceof Document;
    }

    public Query createQuery() {
        return new MongoDocumentQuery((MongoSession) getSession(), getPersistentEntity());
    }


    @Override
    protected void deleteEntry(String family, final Object key, final Object entry) {
        final MongoSession mongoSession = getMongoSession();
        final MongoCollection mongoCollection = mongoSession.getCollection(getPersistentEntity());
        WriteConcern writeConcern = mongoSession.getDeclaredWriteConcern(getPersistentEntity());

        Document dbo = createDBObjectWithKey(key);
        mongoCollection
                .withWriteConcern(writeConcern)
                .deleteOne(dbo);

    }


    @Override
    protected Object generateIdentifier(final PersistentEntity persistentEntity, final Document nativeEntry) {

        String collectionName = getCollectionName(persistentEntity, nativeEntry);

        final MongoCollection<Document> dbCollection = getMongoSession().getCollection(persistentEntity);

        // If there is a numeric identifier then we need to rely on optimistic concurrency controls to obtain a unique identifer
        // sequence. If the identifier is not numeric then we assume BSON ObjectIds.
        if (hasNumericalIdentifier) {

            int attempts = 0;
            while (true) {

                final FindOneAndUpdateOptions options = new FindOneAndUpdateOptions();
                options.upsert(true).returnDocument(ReturnDocument.AFTER);
                Document result = dbCollection.findOneAndUpdate(new Document(MONGO_ID_FIELD, collectionName), new Document("$inc", new Document("next_id", 1)), options);
                // result should never be null and we shouldn't come back with an error ,but you never know. We should just retry if this happens...
                if (result != null) {
                    long nextId = getMappingContext().getConversionService().convert(result.get("next_id"), Long.class);
                    nativeEntry.put(MONGO_ID_FIELD, nextId);
                    break;
                } else {
                    attempts++;
                    if (attempts > 3) {
                        throw new IdentityGenerationException("Unable to generate identity for ["+persistentEntity.getName()+"] using findAndModify after 3 attempts");
                    }
                }
            }

            return nativeEntry.get(MONGO_ID_FIELD);
        }

        ObjectId objectId = ObjectId.get();
        if (ObjectId.class.isAssignableFrom(persistentEntity.getIdentity().getType())) {
            nativeEntry.put(MONGO_ID_FIELD, objectId);
            return objectId;
        }

        String stringId = objectId.toString();
        nativeEntry.put(MONGO_ID_FIELD, stringId);
        return stringId;
    }


    @Override
    protected Document createNewEntry(String family, Object instance) {
        SessionImplementor<Document> si = (SessionImplementor<Document>)getSession();

        Document dbo = si.getCachedEntry(getPersistentEntity(), createInstanceCacheEntryKey(instance));
        if(dbo != null) {
            return dbo;
        }
        else {
            return super.createNewEntry(family, instance);
        }
    }


    @Override
    protected Document createNewEntry(String family) {
        Document dbo = new Document();
        PersistentEntity persistentEntity = getPersistentEntity();
        if (!persistentEntity.isRoot()) {
            dbo.put(MONGO_CLASS_FIELD, persistentEntity.getDiscriminator());
        }

        return dbo;
    }


    @Override
    protected void setEntryValue(Document nativeEntry, String key, Object value) {
        MappingContext mappingContext = getMappingContext();
        setDBObjectValue(nativeEntry, key, value, mappingContext);
    }


    public static void setDBObjectValue(Document nativeEntry, String key, Object value, MappingContext mappingContext) {
        Object nativeValue = getSimpleNativePropertyValue(value, mappingContext);
        nativeEntry.put(key, nativeValue);
    }

    /**
     * Convert a value into a type suitable for use in Mongo. Collections and maps are converted recursively. The
     * mapping context is used for the conversion if possible, otherwise toString() is the eventual fallback.
     * @param value The value to convert (or null)
     * @param mappingContext The mapping context.
     * @return The converted value (or null)
     */
    public static Object getSimpleNativePropertyValue(Object value, MappingContext mappingContext) {
        Object nativeValue;

        if (value == null || mappingContext.isPersistentEntity(value)) {
            nativeValue = null;
        } else if (MongoMappingContext.isMongoNativeType(value.getClass())) {
            // easy case, no conversion required.
            // Checked first in case any of these types (such as BasicDBObject) are instances of collections
            // or arrays, etc.!
            nativeValue = value;
        } else if (value.getClass().isArray()) {
            Object[] array = (Object[]) value;
            List<Object> nativeColl = new ArrayList<Object>(array.length);
            for (Object item : array) {
                nativeColl.add(getSimpleNativePropertyValue(item, mappingContext));
            }
            nativeValue = nativeColl;
        } else if (value instanceof Collection) {
            Collection existingColl = (Collection)value;
            List<Object> nativeColl = new ArrayList<Object>(existingColl.size());
            for (Object item : existingColl) {
                nativeColl.add(getSimpleNativePropertyValue(item, mappingContext));
            }
            nativeValue = nativeColl;
        } else if (value instanceof Map) {
            Map<String, Object> existingMap = (Map)value;
            Map<String, Object> newMap = new LinkedHashMap<String, Object>();
            for (Map.Entry<String, Object> entry :existingMap.entrySet()) {
                newMap.put(entry.getKey(), getSimpleNativePropertyValue(entry.getValue(), mappingContext));
            }
            nativeValue = newMap;
        } else {
            nativeValue = convertPrimitiveToNative(value, mappingContext);
        }
        return nativeValue;
    }

    private static Object convertPrimitiveToNative(Object item, MappingContext mappingContext) {
        Object nativeValue;
        if (item != null) {
            ConversionService conversionService = mappingContext.getConversionService();
            // go for toInteger or toString.
            TypeDescriptor itemTypeDescriptor = TypeDescriptor.forObject(item);
            Class<?> itemTypeClass = itemTypeDescriptor.getObjectType();
            if ((itemTypeClass.equals(Integer.class) || itemTypeClass.equals(Short.class)) && conversionService.canConvert(itemTypeDescriptor, TypeDescriptor.valueOf(Integer.class))) {
                nativeValue = conversionService.convert(item, Integer.class);
            } else if (conversionService.canConvert(itemTypeDescriptor, TypeDescriptor.valueOf(String.class))) {
                nativeValue = conversionService.convert(item, String.class);
            } else {
                // fall back if no explicit converter is registered, good for URL, Locale, etc.
                nativeValue = item.toString();
            }
        } else {
            nativeValue = null;
        }
        return nativeValue;
    }


    @Override
    protected Document retrieveEntry(final PersistentEntity persistentEntity,
                                     String family, final Serializable key) {
        final MongoSession mongoSession = getMongoSession();
        final MongoCollection<Document> collection =
                mongoSession
                        .getNativeInterface()
                        .getDatabase( mongoSession.getDatabase(persistentEntity))
                        .getCollection( mongoSession.getCollectionName(persistentEntity ));
        return collection.find(createDBObjectWithKey(key)).limit(1).first();
    }

    private Document removeNullEntries(Document nativeEntry) {
        for (String key : new HashSet<String>(nativeEntry.keySet())) {
            Object o = nativeEntry.get(key);
            if (o == null) {
                nativeEntry.remove(key);
            } else if (o instanceof Object[]) {
                for (Object o2 : (Object[])o) {
                    if (o2 instanceof Document) {
                        removeNullEntries((Document)o2);
                    }
                }
            } else if (o instanceof List) {
                for (Object o2 : (List)o) {
                    if (o2 instanceof Document) {
                        removeNullEntries((Document)o2);
                    }
                }
            } else if (o instanceof Document) {
                removeNullEntries((Document)o);
            }
        }
        return nativeEntry;
    }

    @Override
    protected Object storeEntry(final PersistentEntity persistentEntity, final EntityAccess entityAccess,
                                final Object storeId, final Document nativeEntry) {

        removeNullEntries(nativeEntry);
        nativeEntry.put(MONGO_ID_FIELD, storeId);
        return nativeEntry.get(MONGO_ID_FIELD);
    }

    protected String getCollectionName(PersistentEntity persistentEntity, Document nativeEntry) {
        String collectionName;
        if (persistentEntity.isRoot()) {
            MongoSession mongoSession = (MongoSession) getSession();
            collectionName = mongoSession.getCollectionName(persistentEntity);
        }
        else {
            MongoSession mongoSession = (MongoSession) getSession();
            collectionName = mongoSession.getCollectionName(persistentEntity.getRootEntity());
        }
        return collectionName;
    }

    private Document modifyNullsToUnsets(Document nativeEntry) {
        Document unsets = new Document();
        Document sets = new Document();
        for (String key : nativeEntry.keySet()) {
            Object o = nativeEntry.get(key);
            if (o == null) {
                unsets.put(key, 1);
            } else if ("_id".equals(key)) {
            } else if (o instanceof Object[]) {
                sets.put(key, o);
                for (Object o2 : (Object[])o) {
                    if (o2 instanceof Document) {
                        removeNullEntries((Document)o2);
                    }
                }
            } else if (o instanceof List) {
                sets.put(key, o);
                for (Object o2 : (List)o) {
                    if (o2 instanceof Document) {
                        removeNullEntries((Document)o2);
                    }
                }
            } else if (o instanceof DBObject) {
                sets.put(key, removeNullEntries((Document)o));
            } else {
                sets.put(key, o);
            }
        }
        Document newEntry = new Document();
        newEntry.put("$set", sets);
        if (!unsets.keySet().isEmpty()) {
            newEntry.put("$unset", unsets);
        }
        return newEntry;
    }

    @Override
    public void updateEntry(final PersistentEntity persistentEntity, final EntityAccess ea,
                            final Object key, final Document entry) {
        MongoCollection dbCollection = getMongoSession().getCollection(persistentEntity);
        Document dbo = createDBObjectWithKey(key);

        boolean versioned = isVersioned(ea);
        Object currentVersion = null;
        if (versioned) {
            currentVersion = getCurrentVersion(ea);
            incrementVersion(ea);
            // query for old version to ensure atomicity
            if (currentVersion != null) {
                dbo.put(GormProperties.VERSION, currentVersion);
            }
        }

        Document newEntry = modifyNullsToUnsets(entry);

        MongoSession mongoSession = (MongoSession) session;
        WriteConcern writeConcern = mongoSession.getDeclaredWriteConcern(getPersistentEntity());
        UpdateResult result;
        if (writeConcern != null) {
            result = dbCollection
                    .withWriteConcern(writeConcern)
                    .updateOne(dbo, newEntry, new UpdateOptions().upsert(false));
        }
        else {
            result = dbCollection
                    .updateOne(dbo, newEntry, new UpdateOptions().upsert(false));
        }
        if (versioned && !((SessionImplementor)getSession()).isStateless(persistentEntity)) {
            // ok, we need to check whether the write worked:
            // note that this will use the standard write concern unless it wasn't at least ACKNOWLEDGE:
            // if the document count "n" of the update was 0, the version check must have failed:
            if (result.wasAcknowledged() && result.getModifiedCount() == 0) {
                if(currentVersion != null) {
                    ea.setProperty(GormProperties.VERSION, currentVersion);
                }
                throw new OptimisticLockingException(persistentEntity, key);
            }
        }
    }

    @Override
    ValueRetrievalStrategy<Document> getValueRetrievalStrategy() {
        return VALUE_RETRIEVAL_STRATEGY;
    }

    @Override
    protected void deleteEntries(String family, final List<Object> keys) {
        final MongoCollection dbCollection = getMongoSession().getCollection(getPersistentEntity());

        MongoSession mongoSession = (MongoSession) getSession();
        MongoDocumentQuery query = (MongoDocumentQuery)mongoSession.createQuery(getPersistentEntity().getJavaClass());
        query.in(getPersistentEntity().getIdentity().getName(), keys);

        dbCollection.deleteMany(query.getMongoQuery());

    }

    protected Document createDBObjectWithKey(Object key) {
        Document dbo = new Document();
        if (hasNumericalIdentifier || hasStringIdentifier) {
            dbo.put(MONGO_ID_FIELD, key);
        }
        else {
            if (key instanceof ObjectId) {
                dbo.put(MONGO_ID_FIELD, key);
            }
            else {
                dbo.put(MONGO_ID_FIELD, new ObjectId(key.toString()));
            }
        }
        return dbo;
    }


}
