package org.grails.datastore.mapping.jcr.engine;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.springframework.beans.SimpleTypeConverter;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataRetrievalFailureException;
import org.grails.datastore.mapping.core.Session;
import org.grails.datastore.mapping.jcr.JcrSession;
import org.grails.datastore.mapping.jcr.util.JcrConstants;
import org.grails.datastore.mapping.model.MappingContext;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.node.engine.AbstractNodeEntityPersister;
import org.grails.datastore.mapping.query.JcrQuery;
import org.springframework.extensions.jcr.JcrCallback;
import org.springframework.extensions.jcr.JcrTemplate;

/**
 * TODO: write javadoc
 *
 * @author Erawat Chamanont
 * @since 1.0
 */
public class JcrEntityPersister extends AbstractNodeEntityPersister<Node, String> {

    private JcrTemplate jcrTemplate;
    private SimpleTypeConverter typeConverter;

    public JcrEntityPersister(MappingContext context, PersistentEntity entity, Session session,
               JcrTemplate jcrTemplate, ApplicationEventPublisher publisher) {
        super(context, entity, session, publisher);
        typeConverter = new SimpleTypeConverter();
        this.jcrTemplate = jcrTemplate;
        this.jcrTemplate.setAllowCreate(true);
    }

    public JcrEntityPersister(MappingContext mappingContext, PersistentEntity entity,
               Session session, ApplicationEventPublisher publisher) {
        super(mappingContext, entity, session, publisher);
    }

    /**
     * @param persistentEntity The PesistentEntity instnace
     * @param id               The identifer
     * @param timeout          The lock timeout in seconds
     */
    @Override
    protected void lockEntry(PersistentEntity persistentEntity, Serializable id, int timeout) {
        //TODO: Implement  lock timeout
        Node node = jcrTemplate.getNodeByUUID(getString(id));
        try {
            node.lock(true, true);
        } catch (RepositoryException e) {
            throw new DataAccessResourceFailureException("Exception occurred cannot lock entity: " + e.getMessage(), e);
        }
    }

    /**
     * @param o The object
     * @return True if the object is locked
     */
    @Override
    public boolean isLocked(Object o) {
        String uuid = getString(createEntityAccess(getPersistentEntity(), o).getIdentifier());
        if (uuid == null) return false;
        Node node = jcrTemplate.getNodeByUUID(uuid);
        try {
            return node.isLocked();
        } catch (RepositoryException e) {
            throw new DataAccessResourceFailureException("Exception occurred cannot unlock entity: " + e.getMessage(), e);
        }
    }

    /**
     * @param persistentEntity The persistent entity
     * @param id               The identifer
     */
    @Override
    protected void unlockEntry(PersistentEntity persistentEntity, Serializable id) {
        Node node = jcrTemplate.getNodeByUUID(getString(id));
        try {
            node.unlock();
        } catch (RepositoryException e) {
            throw new DataAccessResourceFailureException("Exception occurred cannot unlock entity: " + e.getMessage(), e);
        }
    }

    @Override
    protected String generateIdentifier(PersistentEntity persistentEntity, Node tmp) {
        try {
            return tmp.getUUID();
        } catch (RepositoryException e) {
            throw new DataAccessResourceFailureException("Exception occurred cannot generateIdentifier by getting UUID from Node: " + e.getMessage(), e);
        }
    }

    @Override
    protected void deleteEntry(final String key) {
        Node node = jcrTemplate.getNodeByUUID(key);
        try {
            node.remove();
            jcrTemplate.save();
        } catch (RepositoryException e) {
            throw new DataAccessResourceFailureException("Exception occurred cannot delete Node: " + e.getMessage(), e);
        }
    }

    @Override
    protected Object getEntryValue(Node nativeEntry, String property) {
        try {
            Property prop = nativeEntry.getProperty(property);
            if (prop.getType() == PropertyType.REFERENCE) {
                String nodeUUID = prop.getString();
                return jcrTemplate.getNodeByUUID(nodeUUID);
            }
            switch (prop.getType()) {
                case PropertyType.BINARY:
                    // TODO - add lazyinputstream
                    return prop.getString();
                case PropertyType.BOOLEAN:
                    return prop.getBoolean();
                case PropertyType.DATE:
                    return prop.getDate();
                case PropertyType.DOUBLE:
                    return prop.getDouble();
                case PropertyType.LONG:
                    return prop.getLong();
                case PropertyType.NAME: // fall through
                case PropertyType.PATH: // fall through
                case PropertyType.REFERENCE: // fall through
                case PropertyType.STRING: // fall through
                case PropertyType.UNDEFINED: // not actually expected
                default: // not actually expected
                    return prop.getString();
            }
        } catch(PathNotFoundException e) {
            return null;
        } catch (Exception e) {
            throw new DataRetrievalFailureException("Exception occurred cannot getProperty from Node: " + e.getMessage(), e);
        }
    }

    @Override
    protected Node retrieveEntry(final PersistentEntity persistentEntity, final Serializable key) {
        if (key == null) {
            return null;
        }

        return (Node) jcrTemplate.execute(new JcrCallback() {
            public Object doInJcr(javax.jcr.Session s) throws IOException, RepositoryException {
                try {
                    return s.getNodeByUUID(getString(key));
                } catch (ItemNotFoundException ex) {
                    //Force to return null when ItemNotFoundException occurred
                    return null;
                }
            }
        });
    }

    private String getString(Object key) {
        return typeConverter.convertIfNecessary(key, String.class);
    }

    private Long getLong(Object value) {
        return typeConverter.convertIfNecessary(value, Long.class);
    }

    @Override
    protected Node createNewEntry(final PersistentEntity persistentEntity) {
        try {
            Node rootNode = jcrTemplate.getRootNode();
            Node node = rootNode.addNode(persistentEntity.getJavaClass().getSimpleName(), JcrConstants.DEFAULT_JCR_TYPE);
            node.addMixin(JcrConstants.MIXIN_REFERENCEABLE);
            node.addMixin(JcrConstants.MIXIN_VERSIONABLE);
            node.addMixin(JcrConstants.MIXIN_LOCKABLE);
            return node;
        } catch (RepositoryException e) {
            throw new DataAccessResourceFailureException("Exception occurred cannot create Node: " + e.getMessage(), e);
        }
    }

    @Override
    protected void setEntryValue(Node nativeEntry, String propertyName, Object value) {
        //Possible property should be only String, Boolean, Calendar, Double, InputStream and Long
        if (value != null) {
            try {
                if (value instanceof String)
                    nativeEntry.setProperty(propertyName, (String) value);
                else if (value instanceof Boolean)
                    nativeEntry.setProperty(propertyName, (Boolean) value);
                else if (value instanceof Calendar) {
                    nativeEntry.setProperty(propertyName, (Calendar) value);
                }else if (value instanceof Double)
                    nativeEntry.setProperty(propertyName, (Double) value);
                else if (value instanceof InputStream)
                    nativeEntry.setProperty(propertyName, (InputStream) value);
                else if (value instanceof Long)
                    nativeEntry.setProperty(propertyName, getLong(value));
                else if (value instanceof Integer)
                    nativeEntry.setProperty(propertyName, getLong(value));
                else if (value instanceof Date) {
                    //TODO: Solve date type problem.
                    nativeEntry.setProperty(propertyName, ((Date)value).getTime());
                }else{
                    //Marshaling all unsupported data types into String
                    value = value.toString();
                    nativeEntry.setProperty(propertyName, (String)value);
                }
              } catch (RepositoryException e) {
                throw new DataAccessResourceFailureException("Exception occurred set a property value to Node: " + e.getMessage(), e);
            }
        }
    }

    @Override
    protected String storeEntry(PersistentEntity persistentEntity, String id, Node nativeEntry) {
        jcrTemplate.save();
        return id;
    }

    @Override
    protected void updateEntry(final PersistentEntity persistentEntity, final String id, final Node entry) {
        if (id != null) {
            List<String> propNames = persistentEntity.getPersistentPropertyNames();
            Node node = jcrTemplate.getNodeByUUID(id);
            try {
                node.checkout();
                for (String propName : propNames) {
                    if (node.hasProperty(propName)) {
                        node.setProperty(propName, entry.getProperty(propName).getValue());
                    }
                }
                node.save();
                node.checkin();
            } catch (RepositoryException e) {
                throw new DataAccessResourceFailureException("Exception occurred when updating Node: " + e.getMessage(), e);
            }
        }
    }

    public org.grails.datastore.mapping.query.Query createQuery() {
        return new JcrQuery((JcrSession) getSession(), getJcrTemplate(), getPersistentEntity(), this);
    }

    public JcrTemplate getJcrTemplate() {
        return jcrTemplate;
    }
}
