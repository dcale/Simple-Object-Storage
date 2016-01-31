package ch.papers.objectstorage.listeners;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 23/01/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class DummyOnResultListener implements OnResultListener {

    private static DummyOnResultListener INSTANCE;

    public static DummyOnResultListener getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new DummyOnResultListener();
        }
        return INSTANCE;
    }

    private DummyOnResultListener() {
    }

    @Override
    public void onSuccess(Object result) {

    }

    @Override
    public void onError(String message) {

    }
}
