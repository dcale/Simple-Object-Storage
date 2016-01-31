package ch.papers.objectstorage;

import junit.framework.Assert;

import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import ch.papers.objectstorage.filters.Filter;
import ch.papers.objectstorage.listeners.OnResultListener;
import ch.papers.objectstorage.listeners.OnStorageChangeListener;
import ch.papers.objectstorage.models.UuidObject;

/**
 * To work on unit tests, switch the Test Artifact in the Build Variants view.
 */
public class ObjectStorageUnitTest {
    public final static File STORAGE_ROOT = new File(".");

    public class TestModel extends UuidObject {
        private final String name;
        private final String description;
        private TestModel nestedChild;

        public TestModel(String name, String description) {
            this.name = name;
            this.description = description;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public TestModel getNestedChild() {
            return nestedChild;
        }

        public void setNestedChild(TestModel nestedChild) {
            this.nestedChild = nestedChild;
        }
    }

    @Test
    public void testInit() throws InterruptedException {
        final CountDownLatch syncLatch = new CountDownLatch(2);
        UuidObjectStorage.getInstance().getEntries(new OnResultListener<Map<UUID, TestModel>>() {
            @Override
            public void onSuccess(Map<UUID, TestModel> result) {
                Assert.assertTrue(false);
                syncLatch.countDown();
            }

            @Override
            public void onError(String message) {
                Assert.assertTrue(true);
                syncLatch.countDown();
            }
        }, TestModel.class);

        UuidObjectStorage.getInstance().init(STORAGE_ROOT);

        UuidObjectStorage.getInstance().getEntries(new OnResultListener<Map<UUID, TestModel>>() {
            @Override
            public void onSuccess(Map<UUID, TestModel> result) {
                Assert.assertTrue(true);
                syncLatch.countDown();
            }

            @Override
            public void onError(String message) {
                Assert.assertTrue(false);
                syncLatch.countDown();
            }
        }, TestModel.class);

        syncLatch.await();
    }


    @Test
    public void testListeners() throws InterruptedException {
        TestModel model = new TestModel("bla", "desc");
        final CountDownLatch syncLatch = new CountDownLatch(4); // first add, then onchange, then remove, then onchange, then remove -> no more onchange
        UuidObjectStorage.getInstance().init(STORAGE_ROOT);

        OnStorageChangeListener listener = new OnStorageChangeListener() {
            @Override
            public void onChange() {
                syncLatch.countDown();
            }
        };

        UuidObjectStorage.getInstance().registerOnChangeListener(listener, TestModel.class);

        UuidObjectStorage.getInstance().addEntry(model, new OnResultListener<TestModel>() {
            @Override
            public void onSuccess(TestModel result) {
                Assert.assertTrue(true);
                syncLatch.countDown();
            }

            @Override
            public void onError(String message) {
                Assert.assertTrue(false);
                syncLatch.countDown();
            }
        }, TestModel.class);

        UuidObjectStorage.getInstance().deleteEntry(model, new OnResultListener<TestModel>() {
            @Override
            public void onSuccess(TestModel result) {
                Assert.assertTrue(true);
                syncLatch.countDown();
            }

            @Override
            public void onError(String message) {
                Assert.assertTrue(false);
                syncLatch.countDown();
            }
        }, TestModel.class);
        syncLatch.await();

        UuidObjectStorage.getInstance().unRegisterOnChangeListener(listener, TestModel.class);
    }


    @Test
    public void testSingleAddPerformance() throws InterruptedException {
        final int entryNumber = 1000;
        UuidObjectStorage.getInstance().init(STORAGE_ROOT);
        final CountDownLatch syncLatch = new CountDownLatch(entryNumber);
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < entryNumber; i++) {
            TestModel model = new TestModel("bla" + i, "desc");
            UuidObjectStorage.getInstance().addEntry(model, new OnResultListener<TestModel>() {
                @Override
                public void onSuccess(TestModel result) {
                    Assert.assertTrue(true);
                    syncLatch.countDown();
                }

                @Override
                public void onError(String message) {
                    Assert.assertTrue(false);
                    syncLatch.countDown();
                }
            }, TestModel.class);
        }
        syncLatch.await();
        System.out.println("took me " + (System.currentTimeMillis() - startTime) + "ms for single adding " + entryNumber + " entries");
        startTime = System.currentTimeMillis();

        final CountDownLatch commitLatch = new CountDownLatch(1);
        UuidObjectStorage.getInstance().commit(new OnResultListener<String>() {
            @Override
            public void onSuccess(String result) {
                Assert.assertTrue(true);
                commitLatch.countDown();
            }

            @Override
            public void onError(String message) {
                Assert.assertTrue(false);
                commitLatch.countDown();
            }
        });
        commitLatch.await();
        System.out.println("took me " + (System.currentTimeMillis() - startTime) + "ms to commit " + entryNumber + " entries");
        startTime = System.currentTimeMillis();
    }

    @Test
    public void testBulkAddPerformance() throws InterruptedException {
        final int entryNumber = 1000;
        UuidObjectStorage.getInstance().init(STORAGE_ROOT);
        final CountDownLatch syncLatch = new CountDownLatch(1);
        List<TestModel> bulkList = new ArrayList();
        for (int i = 0; i < entryNumber; i++) {
            TestModel model = new TestModel("bla" + i, "desc");
            bulkList.add(model);
        }

        long startTime = System.currentTimeMillis();
        UuidObjectStorage.getInstance().addEntriesAsList(bulkList, new OnResultListener<List<TestModel>>() {
            @Override
            public void onSuccess(List<TestModel> result) {
                Assert.assertTrue(true);
                syncLatch.countDown();
            }

            @Override
            public void onError(String message) {
                Assert.assertTrue(false);
                syncLatch.countDown();
            }
        }, TestModel.class);
        syncLatch.await();
        System.out.println("took me " + (System.currentTimeMillis() - startTime) + "ms for bulk adding list " + entryNumber + " entries");
        startTime = System.currentTimeMillis();

        final CountDownLatch commitLatch = new CountDownLatch(1);
        UuidObjectStorage.getInstance().commit(new OnResultListener<String>() {
            @Override
            public void onSuccess(String result) {
                Assert.assertTrue(true);
                commitLatch.countDown();
            }

            @Override
            public void onError(String message) {
                Assert.assertTrue(false);
                commitLatch.countDown();
            }
        });
        commitLatch.await();
        System.out.println("took me " + (System.currentTimeMillis() - startTime) + "ms to commit " + entryNumber + " entries");

        final CountDownLatch mapLatch = new CountDownLatch(1);
        Map<UUID, TestModel> bulkMap = new HashMap<>();
        for (int i = 0; i < entryNumber; i++) {
            TestModel model = new TestModel("bla" + i, "desc");
            bulkMap.put(model.getUuid(), model);
        }
        startTime = System.currentTimeMillis();
        UuidObjectStorage.getInstance().addEntries(bulkMap, new OnResultListener<Map<UUID, TestModel>>() {
            @Override
            public void onSuccess(Map<UUID, TestModel> result) {
                Assert.assertTrue(true);
                mapLatch.countDown();
            }

            @Override
            public void onError(String message) {
                Assert.assertTrue(false);
                mapLatch.countDown();
            }
        }, TestModel.class);
        mapLatch.await();
        System.out.println("took me " + (System.currentTimeMillis() - startTime) + "ms for bulk adding map " + entryNumber + " entries");
        startTime = System.currentTimeMillis();

        final CountDownLatch commitBulkLatch = new CountDownLatch(1);
        UuidObjectStorage.getInstance().commit(new OnResultListener<String>() {
            @Override
            public void onSuccess(String result) {
                Assert.assertTrue(true);
                commitBulkLatch.countDown();
            }

            @Override
            public void onError(String message) {
                Assert.assertTrue(false);
                commitBulkLatch.countDown();
            }
        });
        commitBulkLatch.await();
        System.out.println("took me " + (System.currentTimeMillis() - startTime) + "ms to commit " + entryNumber + " entries");
        startTime = System.currentTimeMillis();
    }

    @Test
    public void testGetAllEntries() throws InterruptedException {
        UuidObjectStorage.getInstance().init(STORAGE_ROOT);

        final CountDownLatch getLatch = new CountDownLatch(1);
        final long startTime = System.currentTimeMillis();
        UuidObjectStorage.getInstance().getEntriesAsList(new OnResultListener<List<TestModel>>() {
            @Override
            public void onSuccess(List<TestModel> result) {
                System.out.println("took me " + (System.currentTimeMillis() - startTime) + "ms to get " + result.size() + " entries as List");
                Assert.assertTrue(true);
                getLatch.countDown();
            }

            @Override
            public void onError(String message) {
                Assert.assertTrue(false);
                getLatch.countDown();
            }
        }, TestModel.class);
        getLatch.await();

        final CountDownLatch getMapLatch = new CountDownLatch(1);
        final long startMapTime = System.currentTimeMillis();
        UuidObjectStorage.getInstance().getEntries(new OnResultListener<Map<UUID, TestModel>>() {
            @Override
            public void onSuccess(Map<UUID, TestModel> result) {
                System.out.println("took me " + (System.currentTimeMillis() - startTime) + "ms to get " + result.size() + " entries as Map");
                Assert.assertTrue(true);
                getMapLatch.countDown();
            }

            @Override
            public void onError(String message) {
                Assert.assertTrue(false);
                getMapLatch.countDown();
            }
        }, TestModel.class);
        getMapLatch.await();
    }

    @Test
    public void testGetFilterEntries() throws InterruptedException {
        UuidObjectStorage.getInstance().init(STORAGE_ROOT);

        final CountDownLatch getLatch = new CountDownLatch(1);
        final long startTime = System.currentTimeMillis();
        UuidObjectStorage.getInstance().getEntriesAsList(new Filter<TestModel>() {
            @Override
            public boolean matches(TestModel object) {
                return object.getName().equals("bla1");
            }
        }, new OnResultListener<List<TestModel>>() {
            @Override
            public void onSuccess(List<TestModel> result) {
                System.out.println("took me " + (System.currentTimeMillis() - startTime) + "ms to get " + result.size() + " entries as filtered List");
                Assert.assertTrue(true);
                getLatch.countDown();
            }

            @Override
            public void onError(String message) {
                Assert.assertTrue(false);
                getLatch.countDown();
            }
        }, TestModel.class);
        getLatch.await();

        final CountDownLatch getMapLatch = new CountDownLatch(1);
        final long startMapTime = System.currentTimeMillis();
        UuidObjectStorage.getInstance().getEntries(new Filter<TestModel>() {
            @Override
            public boolean matches(TestModel object) {
                return object.getName().equals("bla1");
            }
        }, new OnResultListener<Map<UUID, TestModel>>() {
            @Override
            public void onSuccess(Map<UUID, TestModel> result) {
                System.out.println("took me " + (System.currentTimeMillis() - startTime) + "ms to get " + result.size() + " entries as filtered Map");
                Assert.assertTrue(true);
                getMapLatch.countDown();
            }

            @Override
            public void onError(String message) {
                Assert.assertTrue(false);
                getMapLatch.countDown();
            }
        }, TestModel.class);
        getMapLatch.await();
    }
}