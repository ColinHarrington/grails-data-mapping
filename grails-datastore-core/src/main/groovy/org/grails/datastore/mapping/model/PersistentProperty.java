package org.grails.datastore.mapping.model;

import org.grails.datastore.mapping.config.Property;

/**
 * @author Graeme Rocher
 * @since 1.0
 */
public interface PersistentProperty<T extends Property> {

    /**
     * The name of the property
     * @return The property name
     */
    String getName();

    /**
     * The name with the first letter in upper case as per Java bean conventions
     * @return The capitilized name
     */
    String getCapitilizedName();

    /**
     * The type of the property
     * @return The property type
     */
    Class<?> getType();

    /**
    * Specifies the mapping between this property and an external form
    * such as a column, key/value pair etc.
    *
    * @return The PropertyMapping instance
    */
    PropertyMapping<T> getMapping();

    /**
     * Obtains the owner of this persistent property
     *
     * @return The owner
     */
    PersistentEntity getOwner();

    /**
     * Whether the property can be set to null
     *
     * @return True if it can
     */
    boolean isNullable();

    /**
     * @return Whether this property is inherited
     */
    boolean isInherited();
}
