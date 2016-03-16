package ch.papers.objectstorage;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 23/11/15.
 * Papers.ch
 * a.decarli@papers.ch
 */

import ch.papers.objectstorage.filters.Filter;
import ch.papers.objectstorage.filters.MatchAllFilter;
import ch.papers.objectstorage.filters.UuidFilter;
import ch.papers.objectstorage.listeners.BlockingOnResultListener;
import ch.papers.objectstorage.listeners.OnResultListener;
import ch.papers.objectstorage.listeners.OnStorageChangeListener;
import ch.papers.objectstorage.models.AbstractUuidObject;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class UuidObjectStorage {

    private static UuidObjectStorage INSTANCE;

    /**
     * Signleton accessor
     *
     * @return the objectstorage object
     */
    public static UuidObjectStorage getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new UuidObjectStorage();
        }
        return INSTANCE;
    }

    private UuidObjectStorage() {
    }

    private File rootPath;

    private final Map<Class<? extends AbstractUuidObject>, Map<UUID, ? extends AbstractUuidObject>> uuidObjectCache = new ConcurrentHashMap<Class<? extends AbstractUuidObject>, Map<UUID, ? extends AbstractUuidObject>>();
    private final Map<Class<? extends AbstractUuidObject>, List<OnStorageChangeListener>> listeners = new ConcurrentHashMap<Class<? extends AbstractUuidObject>, List<OnStorageChangeListener>>();

    /**
     * Initialises the objectstorage signleton with the filepath, where it should store the objects
     *
     * @param rootPath
     */
    public synchronized void init(File rootPath) {
        this.rootPath = rootPath;
        this.uuidObjectCache.clear();
        this.listeners.clear();
    }

    /**
     * Add entries to the object storage synchronously.
     *
     * @param entries the entries to store
     * @param clazz   dynamic type of objects
     * @param <T>     generic type of objects
     * @throws UuidObjectStorageException if something goes wrong during the synchronous call
     */
    public <T extends AbstractUuidObject> void addEntries(final Map<UUID, T> entries, final Class<T> clazz) throws UuidObjectStorageException {
        final BlockingOnResultListener blockingOnResultListener = new BlockingOnResultListener();
        this.addEntries(entries, blockingOnResultListener, clazz);
        try {
            blockingOnResultListener.getCountDownLatch().await();
        } catch (InterruptedException e) {
            throw new UuidObjectStorageException(e);
        }
        if (!blockingOnResultListener.isSuccess()) {
            throw new UuidObjectStorageException(blockingOnResultListener.getErrorMessage());
        }
    }

    /**
     * Add entries to the object storage asynchronously.
     *
     * @param entries        the entries to store
     * @param resultCallback the asynchronous callback
     * @param clazz          dynamic type of objects
     * @param <T>            generic type of objects
     */
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

    /**
     * Add entries to the object storage synchronously.
     *
     * @param entries the entries to store
     * @param clazz   dynamic type of objects
     * @param <T>     generic type of objects
     * @throws UuidObjectStorageException if something goes wrong during the synchronous call
     */
    public <T extends AbstractUuidObject> void addEntriesAsList(final List<T> entries, final Class<T> clazz) throws UuidObjectStorageException {
        final BlockingOnResultListener blockingOnResultListener = new BlockingOnResultListener();
        this.addEntriesAsList(entries, blockingOnResultListener, clazz);
        try {
            blockingOnResultListener.getCountDownLatch().await();
        } catch (InterruptedException e) {
            throw new UuidObjectStorageException(e);
        }
        if (!blockingOnResultListener.isSuccess()) {
            throw new UuidObjectStorageException(blockingOnResultListener.getErrorMessage());
        }
    }

    /**
     * Add entries to the object storage asynchronously.
     *
     * @param entries        the entries to store
     * @param resultCallback the asynchronous callback
     * @param clazz          dynamic type of objects
     * @param <T>            generic type of objects
     */
    public <T extends AbstractUuidObject> void addEntriesAsList(final List<T> entries, final OnResultListener<List<T>> resultCallback, final Class<T> clazz) {
        final Map<UUID, T> entriesToAdd = new ConcurrentHashMap<UUID, T>();
        for (T entry : entries) {
            entriesToAdd.put(entry.getUuid(), entry);
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
        }, clazz);
    }

    /**
     * Add entry to the object storage synchronously.
     *
     * @param entry the entry to store
     * @param clazz dynamic type of objects
     * @param <T>   generic type of objects
     * @throws UuidObjectStorageException if something goes wrong during the synchronous call
     */
    public <T extends AbstractUuidObject> void addEntry(final T entry, final Class<T> clazz) throws UuidObjectStorageException {
        final BlockingOnResultListener blockingOnResultListener = new BlockingOnResultListener();
        this.addEntry(entry, blockingOnResultListener, clazz);
        try {
            blockingOnResultListener.getCountDownLatch().await();
        } catch (InterruptedException e) {
            throw new UuidObjectStorageException(e);
        }
        if (!blockingOnResultListener.isSuccess()) {
            throw new UuidObjectStorageException(blockingOnResultListener.getErrorMessage());
        }
    }

    /**
     * Add entry to the object storage asynchronously.
     *
     * @param entry          the entry to store
     * @param resultCallback the asynchronous callback
     * @param clazz          dynamic type of objects
     * @param <T>            generic type of objects
     */
    public <T extends AbstractUuidObject> void addEntry(final T entry, final OnResultListener<T> resultCallback, final Class<T> clazz) {
        final Map<UUID, T> entriesToAdd = new ConcurrentHashMap<UUID, T>();
        entriesToAdd.put(entry.getUuid(), entry);
        this.addEntries(entriesToAdd, new OnResultListener<Map<UUID, T>>() {
            @Override
            public void onSuccess(Map<UUID, T> result) {
                resultCallback.onSuccess(result.get(entry.getUuid()));
            }

            @Override
            public void onError(String message) {
                resultCallback.onError(message);
            }
        }, clazz);
    }

    /**
     * Delete entry from the object storage synchronously.
     *
     * @param entry the entry to delete
     * @param clazz dynamic type of objects
     * @param <T>   generic type of objects
     * @throws UuidObjectStorageException if something goes wrong during the synchronous call
     */
    public <T extends AbstractUuidObject> void deleteEntry(final T entry, final Class<T> clazz) throws UuidObjectStorageException {
        final BlockingOnResultListener blockingOnResultListener = new BlockingOnResultListener();
        this.deleteEntry(entry, blockingOnResultListener, clazz);
        try {
            blockingOnResultListener.getCountDownLatch().await();
        } catch (InterruptedException e) {
            throw new UuidObjectStorageException(e);
        }
        if (!blockingOnResultListener.isSuccess()) {
            throw new UuidObjectStorageException(blockingOnResultListener.getErrorMessage());
        }
    }

    /**
     * Delete entry from the object storage asynchronously.
     *
     * @param entry          the entry to delete
     * @param resultCallback the asynchronous callback
     * @param clazz          dynamic type of objects
     * @param <T>            generic type of objects
     */
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
        }, clazz);
    }

    /**
     * Delete entries matching filter from the object storage synchronously.
     *
     * @param filter filter to match entries you want to delete
     * @param clazz  dynamic type of objects
     * @param <T>    generic type of objects
     * @throws UuidObjectStorageException if something goes wrong during the synchronous call
     */
    public <T extends AbstractUuidObject> void deleteEntries(final Filter<T> filter, final Class<T> clazz) throws UuidObjectStorageException {
        final BlockingOnResultListener blockingOnResultListener = new BlockingOnResultListener();
        this.deleteEntries(filter, blockingOnResultListener, clazz);
        try {
            blockingOnResultListener.getCountDownLatch().await();
        } catch (InterruptedException e) {
            throw new UuidObjectStorageException(e);
        }
        if (!blockingOnResultListener.isSuccess()) {
            throw new UuidObjectStorageException(blockingOnResultListener.getErrorMessage());
        }
    }

    /**
     * Delete entries matching filter from the object storage asynchronously.
     *
     * @param filter         filter to match entries you want to delete
     * @param resultCallback the asynchronous callback
     * @param clazz          dynamic type of objects
     * @param <T>            generic type of objects
     */
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

    /**
     * Returns the matching filter entries from the object storage synchronously.
     *
     * @param filter filter to match entries you want to get
     * @param clazz  dynamic type of objects
     * @param <T>    generic type of objects
     * @return matching entries
     * @throws UuidObjectStorageException if something goes wrong during the synchronous call
     */
    public <T extends AbstractUuidObject> Map<UUID, T> getEntries(final Filter<T> filter, final Class<T> clazz) throws UuidObjectStorageException {
        final BlockingOnResultListener<Map<UUID, T>> blockingOnResultListener = new BlockingOnResultListener<Map<UUID, T>>();
        this.getEntries(filter, blockingOnResultListener, clazz);
        try {
            blockingOnResultListener.getCountDownLatch().await();
        } catch (InterruptedException e) {
            throw new UuidObjectStorageException(e);
        }
        if (!blockingOnResultListener.isSuccess()) {
            throw new UuidObjectStorageException(blockingOnResultListener.getErrorMessage());
        }
        return blockingOnResultListener.getResultObject();
    }

    /**
     * Returns the matching filter entries from the object storage asynchronously.
     *
     * @param filter         filter to match entries you want to get
     * @param resultCallback the asynchronous callback
     * @param clazz          dynamic type of objects
     * @param <T>            generic type of objects
     */
    public <T extends AbstractUuidObject> void getEntries(final Filter<T> filter, final OnResultListener<Map<UUID, T>> resultCallback, final Class<T> clazz) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final Map<UUID, T> resultMap = new ConcurrentHashMap<UUID, T>();
                try {
                    for (T uuidObject : UuidObjectStorage.this.<T>getOrCreateClassCache(clazz).values()) {
                        if (filter.matches(uuidObject)) {
                            resultMap.put(uuidObject.getUuid(), uuidObject);
                        }
                    }
                    resultCallback.onSuccess(resultMap);
                } catch (Throwable e) {
                    e.printStackTrace();
                    resultCallback.onError(e.getMessage());
                }
            }
        }).start();
    }

    /**
     * Returns all entries from the object storage synchronously.
     *
     * @param clazz dynamic type of objects
     * @param <T>   generic type of objects
     * @return matching entries
     * @throws UuidObjectStorageException if something goes wrong during the synchronous call
     */
    public <T extends AbstractUuidObject> Map<UUID, T> getEntries(final Class<T> clazz) throws UuidObjectStorageException {
        final BlockingOnResultListener<Map<UUID, T>> blockingOnResultListener = new BlockingOnResultListener<Map<UUID, T>>();
        this.getEntries(blockingOnResultListener, clazz);
        try {
            blockingOnResultListener.getCountDownLatch().await();
        } catch (InterruptedException e) {
            throw new UuidObjectStorageException(e);
        }
        if (!blockingOnResultListener.isSuccess()) {
            throw new UuidObjectStorageException(blockingOnResultListener.getErrorMessage());
        }
        return blockingOnResultListener.getResultObject();
    }

    /**
     * Returns all entries from the object storage asynchronously.
     *
     * @param resultCallback the asynchronous callback
     * @param clazz          dynamic type of objects
     * @param <T>            generic type of objects
     * @return matching entries
     * @throws UuidObjectStorageException if something goes wrong during the synchronous call
     */
    public <T extends AbstractUuidObject> void getEntries(final OnResultListener<Map<UUID, T>> resultCallback, final Class<T> clazz) {
        this.getEntries(new MatchAllFilter(), resultCallback, clazz);
    }

    /**
     * Returns the matching filter entries from the object storage synchronously.
     *
     * @param clazz dynamic type of objects
     * @param <T>   generic type of objects
     * @return matching entries
     * @throws UuidObjectStorageException if something goes wrong during the synchronous call
     */
    public <T extends AbstractUuidObject> List<T> getEntriesAsList(final Filter<T> filter, final Class<T> clazz) throws UuidObjectStorageException {
        final BlockingOnResultListener<List<T>> blockingOnResultListener = new BlockingOnResultListener<List<T>>();
        this.getEntriesAsList(filter, blockingOnResultListener, clazz);
        try {
            blockingOnResultListener.getCountDownLatch().await();
        } catch (InterruptedException e) {
            throw new UuidObjectStorageException(e);
        }
        if (!blockingOnResultListener.isSuccess()) {
            throw new UuidObjectStorageException(blockingOnResultListener.getErrorMessage());
        }
        return blockingOnResultListener.getResultObject();
    }

    /**
     * Returns the matching filter entries from the object storage asynchronously.
     *
     * @param filter         filter to match entries you want to get
     * @param resultCallback the asynchronous callback
     * @param clazz          dynamic type of objects
     * @param <T>            generic type of objects
     */
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

    /**
     * Returns all entries from the object storage synchronously.
     *
     * @param clazz dynamic type of objects
     * @param <T>   generic type of objects
     * @return matching entries
     * @throws UuidObjectStorageException if something goes wrong during the synchronous call
     */
    public <T extends AbstractUuidObject> List<T> getEntriesAsList(final Class<T> clazz) throws UuidObjectStorageException {
        final BlockingOnResultListener<List<T>> blockingOnResultListener = new BlockingOnResultListener<List<T>>();
        this.getEntriesAsList(blockingOnResultListener, clazz);
        try {
            blockingOnResultListener.getCountDownLatch().await();
        } catch (InterruptedException e) {
            throw new UuidObjectStorageException(e);
        }
        if (!blockingOnResultListener.isSuccess()) {
            throw new UuidObjectStorageException(blockingOnResultListener.getErrorMessage());
        }
        return blockingOnResultListener.getResultObject();
    }

    /**
     * Returns all entries from the object storage asynchronously.
     *
     * @param resultCallback the asynchronous callback
     * @param clazz          dynamic type of objects
     * @param <T>            generic type of objects
     * @return matching entries
     * @throws UuidObjectStorageException if something goes wrong during the synchronous call
     */
    public <T extends AbstractUuidObject> void getEntriesAsList(final OnResultListener<List<T>> resultCallback, final Class<T> clazz) {
        this.getEntriesAsList(new MatchAllFilter(), resultCallback, clazz);
    }

    /**
     * Returns the first matching entry from the object storage synchronously.
     *
     * @param clazz  dynamic type of objects
     * @param filter filter to match entry you want to get
     * @param <T>    generic type of objects
     * @return matching entry
     * @throws UuidObjectStorageException if something goes wrong during the synchronous call
     */
    public <T extends AbstractUuidObject> T getFirstMatchEntry(final Filter<T> filter, final Class<T> clazz) throws UuidObjectStorageException {
        final BlockingOnResultListener<T> blockingOnResultListener = new BlockingOnResultListener<T>();
        this.getFirstMatchEntry(filter, blockingOnResultListener, clazz);
        try {
            blockingOnResultListener.getCountDownLatch().await();
        } catch (InterruptedException e) {
            throw new UuidObjectStorageException(e);
        }
        if (!blockingOnResultListener.isSuccess()) {
            throw new UuidObjectStorageException(blockingOnResultListener.getErrorMessage());
        }
        return blockingOnResultListener.getResultObject();
    }

    /**
     * Returns the first matching entry from the object storage asynchronously.
     *
     * @param filter         filter to match entry you want to get
     * @param resultCallback the asynchronous callback
     * @param clazz          dynamic type of objects
     * @param <T>            generic type of objects
     */
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
        }, clazz);
    }

    /**
     * Returns the first matching entry from the object storage synchronously.
     *
     * @param clazz dynamic type of objects
     * @param uuid  the identifier of the object
     * @param <T>   generic type of objects
     * @return matching entry
     * @throws UuidObjectStorageException if something goes wrong during the synchronous call
     */
    public <T extends AbstractUuidObject> T getEntry(final UUID uuid, final Class<T> clazz) throws UuidObjectStorageException {
        final BlockingOnResultListener<T> blockingOnResultListener = new BlockingOnResultListener<T>();
        this.getEntry(uuid, blockingOnResultListener, clazz);
        try {
            blockingOnResultListener.getCountDownLatch().await();
        } catch (InterruptedException e) {
            throw new UuidObjectStorageException(e);
        }
        if (!blockingOnResultListener.isSuccess()) {
            throw new UuidObjectStorageException(blockingOnResultListener.getErrorMessage());
        }
        return blockingOnResultListener.getResultObject();
    }

    /**
     * Returns the first matching entry from the object storage asynchronously.
     *
     * @param uuid           the identifier of the object
     * @param resultCallback the asynchronous callback
     * @param clazz          dynamic type of objects
     * @param <T>            generic type of objects
     */
    public <T extends AbstractUuidObject> void getEntry(final UUID uuid, final OnResultListener<T> resultCallback, final Class<T> clazz) {
        this.getFirstMatchEntry(new UuidFilter(uuid), resultCallback, clazz);
    }

    /**
     * Register a listener that will be called if storage of a given class changes.
     *
     * @param onStorageChangeListener the listener that should be called
     * @param clazz                   dynamic type of objects
     * @param <T>                     generic type of objects
     */
    public <T extends AbstractUuidObject> void registerOnChangeListener(OnStorageChangeListener onStorageChangeListener, final Class<T> clazz) {
        final List<OnStorageChangeListener> listeners = this.getOrCreateListenerList(clazz);
        listeners.add(onStorageChangeListener);
    }

    public <T extends AbstractUuidObject> void unRegisterOnChangeListener(OnStorageChangeListener onStorageChangeListener, final Class<T> clazz) {
        final List<OnStorageChangeListener> listeners = this.getOrCreateListenerList(clazz);
        listeners.remove(onStorageChangeListener);
    }

    /**
     * Commit and persist all entries to disk synchronously
     *
     * @throws UuidObjectStorageException if something goes wrong during the synchronous call
     */
    public void commit() throws UuidObjectStorageException {
        final BlockingOnResultListener<String> blockingOnResultListener = new BlockingOnResultListener<String>();
        this.commit(blockingOnResultListener);
        try {
            blockingOnResultListener.getCountDownLatch().await();
        } catch (InterruptedException e) {
            throw new UuidObjectStorageException(e);
        }
        if (!blockingOnResultListener.isSuccess()) {
            throw new UuidObjectStorageException(blockingOnResultListener.getErrorMessage());
        }
    }

    /**
     * Commit and persist all entries to disk asynchronously
     *
     * @param resultCallback the asynchronous callback
     */
    public void commit(final OnResultListener<String> resultCallback) {
        for (Class<? extends AbstractUuidObject> keyClazz : this.uuidObjectCache.keySet()) {
            this.commit(resultCallback, keyClazz);
        }
    }

    /**
     * Commit and persist the entries of a class to disk synchronously
     *
     * @param clazz dynamic type of objects
     * @throws UuidObjectStorageException
     */
    public void commit(final Class<? extends AbstractUuidObject> clazz) throws UuidObjectStorageException {
        final BlockingOnResultListener<String> blockingOnResultListener = new BlockingOnResultListener<String>();
        this.commit(blockingOnResultListener, clazz);
        try {
            blockingOnResultListener.getCountDownLatch().await();
        } catch (InterruptedException e) {
            throw new UuidObjectStorageException(e);
        }
        if (!blockingOnResultListener.isSuccess()) {
            throw new UuidObjectStorageException(blockingOnResultListener.getErrorMessage());
        }
    }

    /**
     * Commit and persist the entries of a class to disk asynchronously
     *
     * @param resultCallback the asynchronous callback
     * @param clazz          dynamic type of objects
     */
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
            this.listeners.put(clazz, listeners);
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
            entries = new ConcurrentHashMap<UUID, T>();
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
        final Map<UUID, T> entriesMap = new ConcurrentHashMap<UUID, T>();
        final Map<UUID, T> deserializedMap = Constants.GSON.fromJson(fileInputStreamReader, new UuidObjectMapType(clazz));
        entriesMap.putAll(deserializedMap);
        this.uuidObjectCache.put(clazz, entriesMap);
        fileInputStreamReader.close();
    }

}
