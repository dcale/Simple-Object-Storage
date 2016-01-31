package ch.papers.objectstorage.filters;

import ch.papers.objectstorage.models.UuidObject;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 23/11/15.
 * Papers.ch
 * a.decarli@papers.ch
 */
public interface Filter<T extends UuidObject> {
    public boolean matches(T object);
}
