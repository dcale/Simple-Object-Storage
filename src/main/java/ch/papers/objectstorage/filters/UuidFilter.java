package ch.papers.objectstorage.filters;

import java.util.UUID;

import ch.papers.objectstorage.models.UuidObject;

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
    public boolean matches(UuidObject object) {
        return object.getUuid().equals(this.matchingUUID);
    }
}
