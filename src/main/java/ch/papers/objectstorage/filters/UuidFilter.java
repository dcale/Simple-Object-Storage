package ch.papers.objectstorage.filters;

import ch.papers.objectstorage.models.AbstractUuidObject;

import java.util.UUID;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 23/01/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class UuidFilter implements Filter {

    private final UUID matchingUUID ;

    public UuidFilter(UUID matchingUUID) {
        this.matchingUUID = matchingUUID;
    }

    @Override
    public boolean matches(AbstractUuidObject object) {
        return object.getUuid().equals(this.matchingUUID);
    }
}
