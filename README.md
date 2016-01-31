# Simple Object Storage

This is a Java library, that allows you to store/persist and retrieve objects. 
The library's only dependency is gson, which is used for object serialization. 
The core idea was to have a clean storage API that keeps things as simple as possible.

## API


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
That's the reason why the library is completely asynchronous and works with callbacks.