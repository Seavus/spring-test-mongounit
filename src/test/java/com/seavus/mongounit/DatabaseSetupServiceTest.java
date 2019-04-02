/*
 * Copyright (c) 2018 Seavus
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package com.seavus.mongounit;

import com.mongodb.BasicDBObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationContext;
import org.springframework.data.mongodb.core.MongoTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DatabaseSetupServiceTest {

    private DatabaseSetupService databaseSetupService;
    @Mock
    private ApplicationContext applicationContext;
    @Mock
    private MongoTemplate mongoTemplate;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        databaseSetupService = new DatabaseSetupService();
    }

    @Test
    public void shouldThrowIfMongoTemplateIsNotProvided() {
        when(applicationContext.getBean(MongoTemplate.class)).thenReturn(null);
        Throwable throwable = catchThrowable(() -> databaseSetupService.init(new TestContext(applicationContext,
                null)));

        assertThat(throwable).isInstanceOf(MongoUnitException.class);
        assertThat(throwable.getMessage()).isEqualTo(
                "You must provide a valid bean for org.springframework.data.mongodb.core.MongoTemplate before using " + "@DatabaseSetup");
    }

    @Test
    public void shouldThrowIfProvidedResourceDoesntExists() {
        when(applicationContext.getBean(MongoTemplate.class)).thenReturn(mongoTemplate);

        Throwable throwable = catchThrowable(() -> databaseSetupService.init(new TestContext(applicationContext,
                NonExistingResourceTestClass.class)));

        assertThat(throwable).isInstanceOf(MongoUnitException.class);
        assertThat(throwable.getMessage()).startsWith("The resource you provided doesn't exists on path");
    }

    @Test
    public void shouldThrowIfProvidedResourceIsNotAValidJsonObject() {
        when(applicationContext.getBean(MongoTemplate.class)).thenReturn(mongoTemplate);

        Throwable throwable = catchThrowable(() -> databaseSetupService.init(new TestContext(applicationContext,
                InvalidJsonResourceTestClass.class)));

        assertThat(throwable).isInstanceOf(MongoUnitException.class);
        assertThat(throwable.getMessage()).startsWith("Could not read content from provided resource:");
    }

    @Test
    public void shouldLoadMethodResourcesToDatabase() {
        when(applicationContext.getBean(MongoTemplate.class)).thenReturn(mongoTemplate);

        databaseSetupService.init(new TestContext(applicationContext, MethodResourceTestClass.class));

        verify(mongoTemplate).dropCollection("methodTestResource");
        verify(mongoTemplate).save(new BasicDBObject(), "methodTestResource");
    }

    @Test
    public void shouldLoadArrayOfResourcesToDatabase() {
        when(applicationContext.getBean(MongoTemplate.class)).thenReturn(mongoTemplate);

        databaseSetupService.init(new TestContext(applicationContext, ArrayOfResourceTestClass.class));

        verify(mongoTemplate).dropCollection("methodTestResource");
        verify(mongoTemplate, times(2)).save(new BasicDBObject(), "methodTestResource");
    }

    @Test
    public void shouldLoadClassResourcesToDatabase() {
        when(applicationContext.getBean(MongoTemplate.class)).thenReturn(mongoTemplate);

        databaseSetupService.init(new TestContext(applicationContext, ClassResourceTestClass.class));

        verify(mongoTemplate).dropCollection("classTestResource");
        verify(mongoTemplate).save(new BasicDBObject(), "classTestResource");
    }

    @Test
    public void shouldLoadMultipleResources() {
        when(applicationContext.getBean(MongoTemplate.class)).thenReturn(mongoTemplate);

        databaseSetupService.init(new TestContext(applicationContext, MultipleResourceTestClass.class));

        verify(mongoTemplate).dropCollection("methodTestResource");
        verify(mongoTemplate).save(new BasicDBObject(), "methodTestResource");
        verify(mongoTemplate).dropCollection("classTestResource");
        verify(mongoTemplate).save(new BasicDBObject(), "classTestResource");
    }

    @Test
    public void shouldDropCollectionsOnCleanup() {
        when(applicationContext.getBean(MongoTemplate.class)).thenReturn(mongoTemplate);

        databaseSetupService.cleanUp(new TestContext(applicationContext, MultipleResourceTestClass.class));

        verify(mongoTemplate).dropCollection("methodTestResource");
        verify(mongoTemplate).dropCollection("classTestResource");
    }

    private class MultipleResourceTestClass {

        @DatabaseSetup({"MethodTestResource.json", "ClassTestResource.json"})
        public void testMethod() {

        }
    }

    private class MethodResourceTestClass {

        @DatabaseSetup("MethodTestResource.json")
        public void testMethod() {

        }
    }

    private class ArrayOfResourceTestClass {

        @DatabaseSetup("ArrayOfResource.json")
        public void testMethod() {

        }
    }

    @DatabaseSetup("ClassTestResource.json")
    private class ClassResourceTestClass {

        public void testMethod() {

        }
    }

    private class NonExistingResourceTestClass {

        @DatabaseSetup("NonExistentTestResource.json")
        public void testMethod() {

        }
    }

    private class InvalidJsonResourceTestClass {

        @DatabaseSetup("InvalidJsonTestResource.json")
        public void testMethod() {

        }
    }
}
