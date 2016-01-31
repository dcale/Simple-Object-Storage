package ch.papers.objectstorage;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 23/11/15.
 * Papers.ch
 * a.decarli@papers.ch
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import ch.papers.objectstorage.filters.Filter;
import ch.papers.objectstorage.filters.MatchAllFilter;
import ch.papers.objectstorage.filters.UuidFilter;
import ch.papers.objectstorage.listeners.OnResultListener;
import ch.papers.objectstorage.listeners.OnStorageChangeListener;
import ch.papers.objectstorage.models.AbstractUuidObject;

public class UuidObjectStorage {

    private static UuidObjectStorage INSTANCE;

    public static UuidObjectStorage getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new UuidObjectStorage();
        }
        return INSTANCE;
    }

    private UuidObjectStorage() {
    }

    private File rootPath;

    private final Map<Class<? extends AbstractUuidObject>, Map<UUID, ? extends AbstractUuidObject>> uuidObjectCache = new LinkedHashMap<Class<? extends AbstractUuidObject>, Map<UUID, ? extends AbstractUuidObject>>();
    private final Map<Class<? extends AbstractUuidObject>, List<OnStorageChangeListener>> listeners = new HashMap<Class<? extends AbstractUuidObject>, List<OnStorageChangeListener>>();

    public synchronized void init(File rootPath) {
        this.rootPath = rootPath;
    }

    public <T extends AbstractUuidObject> void addEntries(final Map<UUID, T> entries, final OnResultListener<Map<UUID, T>> resultCallback, final Class<T> clazz) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    UuidObjectStorage.this.<T>getOrCreateClassCache(clazz).putAll(entries);
                    UuidObjectStorage.this.notifyListeners(clazz);
                    resultCallback.onSuccess(entries);
                } catch (Throwable e) {
                    resultCallback.onError(e.getMessage());
                }
            }
        }).start();
    }

    public <T extends AbstractUuidObject> void addEntriesAsList(final List<T> entries, final OnResultListener<List<T>> resultCallback, final Class<T> clazz) {
        final Map<UUID, T> entriesToAdd = new HashMap<UUID, T>();
        for (T entry:entries) {
            entriesToAdd.put(entry.getUuid(),entry);
        }

        this.addEntries(entriesToAdd, new OnResultListener<Map<UUID, T>>() {
            @Override
            public void onSuccess(Map<UUID, T> result) {
                resultCallback.onSuccess(new ArrayList<T>(result.values()));
            }

            @Override
            public void onError(String message) {
                resultCallback.onError(message);
            }
        },clazz);
    }

    public <T extends AbstractUuidObject> void addEntry(final T entry, final OnResultListener<T> resultCallback, final Class<T> clazz) {
        final Map<UUID, T> entriesToAdd = new HashMap<UUID, T>();
        entriesToAdd.put(entry.getUuid(),entry);
        this.addEntries(entriesToAdd, new OnResultListener<Map<UUID,T>>() {
            @Override
            public void onSuccess(Map<UUID,T> result) {
                resultCallback.onSuccess(result.get(entry.getUuid()));
            }

            @Override
            public void onError(String message) {
                resultCallback.onError(message);
            }
        },clazz);
    }

    public <T extends AbstractUuidObject> void deleteEntry(final T entry, final OnResultListener<T> resultCallback, final Class<T> clazz) {
        this.deleteEntries(new UuidFilter(entry.getUuid()), new OnResultListener<Map<UUID, T>>() {
            @Override
            public void onSuccess(Map<UUID, T> result) {
                resultCallback.onSuccess(result.get(entry.getUuid()));
            }

            @Override
            public void onError(String message) {
                resultCallback.onError(message);
            }
        },clazz);
    }

    public <T extends AbstractUuidObject> void deleteEntries(final Filter<T> filter, final OnResultListener<Map<UUID, T>> resultCallback, final Class<T> clazz) {
        this.getEntries(filter, new OnResultListener<Map<UUID, T>>() {
            @Override
            public void onSuccess(Map<UUID, T> result) {
                for (T uuidObject : result.values()) {
                    UuidObjectStorage.this.uuidObjectCache.get(clazz).remove(uuidObject.getUuid());
                }
                UuidObjectStorage.this.notifyListeners(clazz);
                resultCallback.onSuccess(result);
            }

            @Override
            public void onError(String message) {
                resultCallback.onError(message);
            }
        }, clazz);
    }

    public <T extends AbstractUuidObject> void getEntries(final Filter<T> filter, final OnResultListener<Map<UUID, T>> resultCallback, final Class<T> clazz) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final Map<UUID, T> resultMap = new HashMap<UUID, T>();
                try {
                    for (T uuidObject : UuidObjectStorage.this.<T>getOrCreateClassCache(clazz).values()) {
                        if (filter.matches(uuidObject)) {
                            resultMap.put(uuidObject.getUuid(), uuidObject);
                        }
                    }
                    resultCallback.onSuccess(resultMap);
                } catch (Throwable e) {
                    resultCallback.onError(e.getMessage());
                }
            }
        }).start();
    }

    public <T extends AbstractUuidObject> void getEntries(final OnResultListener<Map<UUID, T>> resultCallback, final Class<T> clazz) {
        this.getEntries(new MatchAllFilter(), resultCallback, clazz);
    }

    public <T extends AbstractUuidObject> void getEntriesAsList(final Filter<T> filter, final OnResultListener<List<T>> resultCallback, final Class<T> clazz) {
        this.getEntries(filter, new OnResultListener<Map<UUID, T>>() {
            @Override
            public void onSuccess(Map<UUID, T> result) {
                resultCallback.onSuccess(new ArrayList<T>(result.values()));
            }

            @Override
            public void onError(String message) {
                resultCallback.onError(message);
            }
        }, clazz);
    }

    public <T extends AbstractUuidObject> void getEntriesAsList(final OnResultListener<List<T>> resultCallback, final Class<T> clazz) {
        this.getEntriesAsList(new MatchAllFilter(), resultCallback, clazz);
    }

    public <T extends AbstractUuidObject> void getFirstMatchEntry(final Filter<T> filter, final OnResultListener<T> resultCallback, final Class<T> clazz) {
        this.getEntries(filter, new OnResultListener<Map<UUID, T>>() {
            @Override
            public void onSuccess(Map<UUID, T> result) {
                if (!result.isEmpty()) {
                    resultCallback.onSuccess(result.values().iterator().next());
                } else {
                    resultCallback.onError("could not find entry for filter " + filter);
                }
            }

            @Override
            public void onError(String message) {
                resultCallback.onError(message);
            }
        },clazz);
    }

    public <T extends AbstractUuidObject> void getEntry(final UUID uuid, final OnResultListener<T> resultCallback, final Class<T> clazz) {
        this.getFirstMatchEntry(new UuidFilter(uuid),resultCallback,clazz);
    }

    public <T extends AbstractUuidObject> void registerOnChangeListener(OnStorageChangeListener onStorageChangeListener, final Class<T> clazz) {
        final List<OnStorageChangeListener> listeners = this.getOrCreateListenerList(clazz);
        listeners.add(onStorageChangeListener);
    }

    public <T extends AbstractUuidObject> void unRegisterOnChangeListener(OnStorageChangeListener onStorageChangeListener, final Class<T> clazz) {
        final List<OnStorageChangeListener> listeners = this.getOrCreateListenerList(clazz);
        listeners.remove(onStorageChangeListener);
    }

    public void commit(final OnResultListener<String> resultCallback) {
        for (Class<? extends AbstractUuidObject> keyClazz : this.uuidObjectCache.keySet()) {
            this.commit(resultCallback, keyClazz);
        }
    }

    public void commit(final OnResultListener<String> resultCallback, final Class<? extends AbstractUuidObject> clazz) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    persistEntries(clazz);
                    resultCallback.onSuccess(Constants.SUCCESS_MESSAGE);
                } catch (Throwable e) {
                    resultCallback.onError(e.getMessage());
                }
            }
        }).start();
    }

    private <T extends AbstractUuidObject> void notifyListeners(final Class<T> clazz) {
        final List<OnStorageChangeListener> listeners = this.getOrCreateListenerList(clazz);
        for (OnStorageChangeListener listener : listeners) {
            listener.onChange();
        }
    }

    private <T extends AbstractUuidObject> List<OnStorageChangeListener> getOrCreateListenerList(final Class<T> clazz) {
        List<OnStorageChangeListener> listeners = this.listeners.get(clazz);
        if (listeners == null) {
            listeners = new ArrayList<OnStorageChangeListener>();
            this.listeners.put(clazz,listeners);
        }
        return listeners;
    }

    private synchronized <T extends AbstractUuidObject> Map<UUID, T> getOrCreateClassCache(final Class<T> clazz) throws IOException {
        try {
            if (!this.uuidObjectCache.containsKey(clazz)) {
                this.<T>loadEntries(clazz);
            }
        } catch (IOException e) {
            // this happens the first time you add an unknown class and it's ok
        }

        Map<UUID, T> entries = (Map<UUID, T>) UuidObjectStorage.this.uuidObjectCache.get(clazz);
        if (entries == null) {
            entries = new HashMap<UUID, T>();
            this.uuidObjectCache.put(clazz, entries);
            this.persistEntries(clazz);
        }

        return entries;
    }

    private synchronized void persistEntries(Class<? extends AbstractUuidObject> clazz) throws IOException {
        final File objectStorageFile = new File(this.rootPath, clazz.getSimpleName() + ".json");
        final FileOutputStream fileOutputStream = new FileOutputStream(objectStorageFile);
        OutputStreamWriter fileOutputStreamWriter = new OutputStreamWriter(fileOutputStream);
        Constants.GSON.toJson(this.uuidObjectCache.get(clazz), fileOutputStreamWriter);
        fileOutputStreamWriter.close();
    }

    private synchronized <T extends AbstractUuidObject> void loadEntries(Class<T> clazz) throws IOException {
        this.uuidObjectCache.remove(clazz);
        final File objectStorageFile = new File(this.rootPath, clazz.getSimpleName() + ".json");
        final FileInputStream fileInputStream = new FileInputStream(objectStorageFile);
        Reader fileInputStreamReader = new InputStreamReader(fileInputStream);
        final Map<UUID, T> entriesMap = Constants.GSON.fromJson(fileInputStreamReader, new UuidObjectMapType(clazz));
        this.uuidObjectCache.put(clazz, entriesMap);
        fileInputStreamReader.close();
    }

}
