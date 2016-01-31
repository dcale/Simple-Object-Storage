package ch.papers.objectstorage.models;

import java.io.Serializable;
import java.util.UUID;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 31/01/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public interface UuidObject extends Serializable {
    public UUID getUuid();
}
