# Simple Object Storage
This is a Java/Android library, that allows you to store/persist and retrieve objects.
The library's only dependency is gson, which is used for object serialization. 
The core idea was to have a clean storage API that keeps things as simple as possible.

## API
The best documentation on how the lib should be used are its Unit Tests. The first thing you want to
do to persist an object is to implement the UuidObject interface of directly extend the AbstractUuidObject.
The reason you have to do this is because the library relies on UUID's for accessing specific objects:

```Java
public class TestModel extends AbstractUuidObject {
        private final String name;
        private final String description;

        public TestModel(String name, String description) {
            this.name = name;
            this.description = description;
        }
}
```

### Storing an Object
```Java
public final static File STORAGE_ROOT = new File(".");

// initialize the singleton
UuidObjectStorage.getInstance().init(STORAGE_ROOT);

// add a new TestModel
TestModel model = new TestModel("my name","my description");
UuidObjectStorage.getInstance().addEntry(model, new OnResultListener<TestModel>() {
            @Override
            public void onSuccess(TestModel result) {
                // TestModel has been added successfully to the storage
            }

            @Override
            public void onError(String message) {
                // Oops something went wrong...
            }
        }, TestModel.class);

// after having added the model, it will be accessible in you app, but it has not been persisted yet.
// you need to commit in order to persist to disk.
UuidObjectStorage.getInstance().commit(new OnResultListener<String>() {
            @Override
            public void onSuccess(String result) {
                // commit successful, everything was persisted
            }

            @Override
            public void onError(String message) {
                // Oops something went wrong...
            }
        });        
```

### Retrieving an Object
```Java
UuidObjectStorage.getInstance().getEntries(new OnResultListener<Map<UUID, TestModel>>() {
            @Override
            public void onSuccess(Map<UUID, TestModel> result) {
                // your entries have been retrieved
            }

            @Override
            public void onError(String message) {
                // Oops something went wrong...
            }
        }, TestModel.class);
```

### Working with Filters
```Java
UuidObjectStorage.getInstance().getEntriesAsList(new Filter<TestModel>() {
            @Override
            public boolean matches(TestModel object) {
                return object.getName().equals("my name");
            }
        }, new OnResultListener<List<TestModel>>() {
            @Override
            public void onSuccess(List<TestModel> result) {
                // this will return a list with all models with the name "my name"
            }

            @Override
            public void onError(String message) {
                // Oops something went wrong...
            }
        }, TestModel.class);
```



## FAQ
- Are there any limitations?
The library lazy loads the object from the file into memory. 
There is no logic to prevent OutOfMemoryExceptions. 
In short: if you try to load a huge file and your machine has not enough memory, this library will crash. 
This lib's core feature is simplicity, if you want a full fledged database that can query objects from 
a large database it might be better to use something like SQLite. 
The lib has been tested and works great for collections of +-500'000 objects.

- Can this library be used on Android?
Yes. The main reason for its creation was the lack of a simple object storage on Android.

- How fast is the storage?
This of course depends on what you are trying to do. 
In general after its first read from disk all objects are stored in memory (lazy load), 
which makes subsequent access blazing fast.

- Is this an ORM?
No, there is no conversion to a relational representation of the object. 
The object is serialized in an object oriented format (JSON). 
But it solves the same problem like an ORM (persisting Java Objects).

- Nothing is synchronous, I want my object now! What can I do?
Reading from disk is expensive and can encounter issues (like no permission to read or no file). 
That's the reason why the library is completely asynchronous and works with callbacks. If you really want
to wait for an object and make the code synchronous, you will need to work with CountDown latches. However I recommend
to keep things asynchronous.