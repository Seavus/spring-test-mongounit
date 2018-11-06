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
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.bson.BsonInvalidOperationException;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.io.ClassRelativeResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import org.springframework.test.context.TestContext;

/**
 * Internal class that is used to provide support for {@link DatabaseSetup}.
 *
 * @author Martin Ilievski
 */
@Service
class DatabaseSetupService {

    void loadResourcesToDatabase(TestContext testContext) {
        MongoTemplate mongoTemplate = testContext.getApplicationContext().getBean(MongoTemplate.class);
        if (mongoTemplate == null) {
            throw new MongoUnitException(
                    "You must provide a valid bean for org.springframework.data.mongodb.core.MongoTemplate " +
                            "before using @DatabaseSetup");
        }
        List<DatabaseSetup> annotations = getAnnotations(testContext);
        for (DatabaseSetupAnnotationAttributes attributes : DatabaseSetupAnnotationAttributes.get(annotations)) {
            String[] resources = attributes.getValue();
            for (String resourceName : resources) {
                loadResourceToDatabase(resourceName, testContext, mongoTemplate);
            }
        }
    }

    private void loadResourceToDatabase(String resourceName, TestContext testContext, MongoTemplate mongoTemplate) {
        try {
            Resource resource = loadResource(resourceName, testContext);

            if (!resource.exists()) {
                throw new MongoUnitException("The resource you provided doesn't exists on path " + resource.getDescription());
            }

            BasicDBObject dbObject = parseResourceToDbObject(resource);
            dbObject.keySet().forEach(collection -> {
                executeMongoOperationsForCollection(mongoTemplate, dbObject, collection);
            });
        } catch (IOException | BsonInvalidOperationException e) {
            throw new MongoUnitException("Could not read content from provided resource: " + resourceName, e);
        }
    }

    private void executeMongoOperationsForCollection(MongoTemplate mongoTemplate, BasicDBObject dbObject,
            String collection) {
        Object collectionData = dbObject.get(collection);
        mongoTemplate.dropCollection(collection);
        mongoTemplate.save(collectionData, collection);
    }

    private BasicDBObject parseResourceToDbObject(Resource resource) throws IOException {
        File resourceFile = resource.getFile();
        String resourceFileContent = FileUtils.readFileToString(resourceFile, Charset.defaultCharset());
        return BasicDBObject.parse(resourceFileContent);
    }

    private Resource loadResource(String resourceName, TestContext testContext) {
        ResourceLoader resourceLoader = new ClassRelativeResourceLoader(testContext.getTestClass());
        return resourceLoader.getResource(resourceName);
    }

    private List<DatabaseSetup> getAnnotations(TestContext testContext) {
        List<DatabaseSetup> annotations = new ArrayList<>();
        addAnnotationToList(annotations,
                AnnotationUtils.findAnnotation(testContext.getTestClass(), DatabaseSetup.class));
        addAnnotationToList(annotations,
                AnnotationUtils.findAnnotation(testContext.getTestMethod(), DatabaseSetup.class));

        return annotations;
    }

    private void addAnnotationToList(List<DatabaseSetup> annotations, DatabaseSetup annotation) {
        if (annotation != null) {
            annotations.add(annotation);
        }
    }
}
