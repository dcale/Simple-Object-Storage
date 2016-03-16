package ch.papers.objectstorage;

import ch.papers.objectstorage.models.AbstractUuidObject;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.UUID;


/**
 * Created by Alessandro De Carli (@a_d_c_) on 05/12/15.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class UuidObjectMapType implements ParameterizedType {
    private Class<? extends AbstractUuidObject> wrapped;

    public UuidObjectMapType(Class<? extends AbstractUuidObject> wrapper) {
        this.wrapped = wrapper;
    }

    @Override
    public Type[] getActualTypeArguments() {
        return new Type[]{UUID.class,wrapped};
    }

    @Override
    public Type getRawType() {
        return Map.class;
    }

    @Override
    public Type getOwnerType() {
        return null;
    }

}
