package ch.papers.objectstorage.filters;

import ch.papers.objectstorage.models.UuidObject;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 23/01/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class MatchAllFilter implements Filter  {
    @Override
    public boolean matches(UuidObject object) {
        return true;
    }
}
