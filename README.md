Introduction
============
Spring Test MongoUnit is a library that provides a custom spring `TestExecutionListener` that can be added to your test class.
This library offers a `@DatabaseSetup` annotation that can be used on class and method level.
Based on the values provided in `@DatabaseSetup` before each test execution the resources you provided will loaded inside of your MongoDb

Configuration
=============
To have Spring process `@DatabaseSetup` annotation you must first configure your tests to use the `DatabaseSetupTestExecutionListener`
class. To do this you need to use the Spring `@TestExecutionListeners` annotation. 

Example typical JUnit 4 test:

    @RunWith(SpringJUnit4ClassRunner.class)
    @ContextConfiguration
    @TestExecutionListeners({ DatabaseSetupTestExecutionListener.class })


See the Spring JavaDocs for details of the standard listeners.

In order for this library to access the Mongo database it requires that you provide a `MongoTemplate` bean, if you use 
Spring Boot package `spring-boot-starter-data-mongodb`, it will be provided automatically.


Setup
=====
The `@DatabaseSetup` annotation indicates how Mongo collections should be setup before test methods are run. 
The annotation can be applied to individual test methods or to a whole class. When applied at the class level the setup
occurs before each method in the test. The annotation value references a file that contains the json structure that you
you want to be loaded into the database. This json structure can include setup for N collections.

Example format of this file:

    {
      "myFirstCollection": {
        "_id": "key", // No matter what's the key name that you annotated with @ID you always define it as "_id"
        "someKey" : "someValue"
      },
      "mySecondCollection": {
        "_id": "key",
        "someKey" : "someValue"
      }
    }
`

Here is a typical setup annotation. In this case a file named `sampleData.json` is contained in the same package as the
test class.

    @DatabaseSetup("sampleData.json")

By default setup will perform a `REMOVE_SAVE` operation, this means that all data from the collections referenced in the
json file will be removed before saving new objects.
