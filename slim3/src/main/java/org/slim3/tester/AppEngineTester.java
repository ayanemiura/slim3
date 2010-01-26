/*
 * Copyright 2004-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.slim3.tester;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.slim3.datastore.DatastoreUtil;
import org.slim3.util.AppEngineUtil;
import org.slim3.util.ThrowableUtil;
import org.slim3.util.WrapRuntimeException;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.labs.taskqueue.TaskQueuePb.TaskQueueAddRequest;
import com.google.appengine.api.labs.taskqueue.TaskQueuePb.TaskQueueAddResponse;
import com.google.appengine.api.mail.MailServicePb.MailMessage;
import com.google.appengine.api.urlfetch.URLFetchServicePb.URLFetchRequest;
import com.google.appengine.api.urlfetch.URLFetchServicePb.URLFetchResponse;
import com.google.appengine.repackaged.com.google.protobuf.ByteString;
import com.google.apphosting.api.ApiProxy;
import com.google.apphosting.api.ApiProxy.ApiConfig;
import com.google.apphosting.api.ApiProxy.ApiProxyException;
import com.google.apphosting.api.ApiProxy.Delegate;
import com.google.apphosting.api.ApiProxy.Environment;
import com.google.apphosting.api.ApiProxy.LogRecord;
import com.google.apphosting.api.DatastorePb.PutResponse;
import com.google.storage.onestore.v3.OnestoreEntity.Reference;

/**
 * A tester for local services.
 * 
 * @author higa
 * @since 3.0
 * 
 */
public class AppEngineTester implements Delegate<Environment> {

    /**
     * The name of ApiProxyLocalImpl class.
     */
    protected static final String API_PROXY_LOCAL_IMPL_CLASS_NAME =
        "com.google.appengine.tools.development.ApiProxyLocalImpl";
    /**
     * The name of LocalDatastoreService class.
     */
    protected static final String LOCAL_DATASTORE_SERVICE_CLASS_NAME =
        "com.google.appengine.api.datastore.dev.LocalDatastoreService";

    /**
     * The name of impl directory.
     */
    protected static final String IMPL_DIR_NAME = "impl";

    /**
     * The name of appengine-api-stubs library.
     */
    protected static final String API_STUBS_LIB_NAME =
        "appengine-api-stubs.jar";

    /**
     * The name of appengine-local-runtime library.
     */
    protected static final String LOCAL_RUNTIME_LIB_NAME =
        "appengine-local-runtime.jar";

    /**
     * The name of local datastore service.
     */
    protected static final String LOCAL_DETASTORE_SERVICE_NAME = "datastore_v3";

    /**
     * The no storage property key.
     */
    protected static final String NO_STORAGE_PROPERTY = "datastore.no_storage";

    /**
     * The internal name of datastore service.
     */
    protected static final String DATASTORE_SERVICE = "datastore_v3";

    /**
     * The internal name of mail service.
     */
    protected static final String MAIL_SERVICE = "mail";

    /**
     * The internal name of taskqueue service.
     */
    protected static final String TASKQUEUE_SERVICE = "taskqueue";

    /**
     * The internal name of urlfetch service.
     */
    protected static final String URLFETCH_SERVICE = "urlfetch";

    /**
     * The internal name of put method.
     */
    protected static final String PUT_METHOD = "Put";

    /**
     * The internal name of send method.
     */
    protected static final String SEND_METHOD = "Send";

    /**
     * The internal name of add method.
     */
    protected static final String ADD_METHOD = "Add";

    /**
     * The internal name of fetch method.
     */
    protected static final String FETCH_METHOD = "Fetch";

    /**
     * The ApiProxyLocalImpl instance.
     */
    protected static Delegate<Environment> apiProxyLocalImpl;

    /**
     * The environment for test.
     */
    public TestEnvironment environment;

    /**
     * The list of mail messages.
     */
    public final List<MailMessage> mailMessages = new ArrayList<MailMessage>();

    /**
     * The list of tasks.
     */
    public final List<TaskQueueAddRequest> tasks =
        new ArrayList<TaskQueueAddRequest>();

    /**
     * The original delegate.
     */
    protected Delegate<Environment> originalDelegate;

    /**
     * The parent delegate.
     */
    protected Delegate<Environment> parentDelegate;

    /**
     * The original environment.
     */
    protected Environment originalEnvironment;

    /**
     * The set of put keys.
     */
    protected Set<Key> putKeys = new HashSet<Key>();

    /**
     * The handler to fetch URL.
     */
    protected URLFetchHandler urlFetchHandler;

    static {
        if (!AppEngineUtil.isServer()) {
            ClassLoader loader = loadLibraries();
            prepareLocalServices(loader);
        }
    }

    /**
     * Loads appengine libraries.
     * 
     * @return {@link ClassLoader} to prepare local services
     */
    protected static ClassLoader loadLibraries() {
        ClassLoader loader = AppEngineTester.class.getClassLoader();
        if (loader instanceof URLClassLoader) {
            File libDir = getLibDir();
            File implDir = new File(libDir, IMPL_DIR_NAME);
            List<URL> urls = new ArrayList<URL>();
            try {
                loader.loadClass(API_PROXY_LOCAL_IMPL_CLASS_NAME);
            } catch (ClassNotFoundException e) {
                urls.add(getLibraryURL(implDir, LOCAL_RUNTIME_LIB_NAME));
            }
            try {
                loader.loadClass(LOCAL_DATASTORE_SERVICE_CLASS_NAME);
            } catch (ClassNotFoundException e) {
                urls.add(getLibraryURL(implDir, API_STUBS_LIB_NAME));
            }
            loadLibraries(loader, urls);
        }
        return loader;
    }

    /**
     * Loads the libraries.
     * 
     * @param loader
     *            the {@link ClassLoader}
     * @param urls
     *            the list of {@link URL}s.
     */
    protected static void loadLibraries(ClassLoader loader, List<URL> urls) {
        if (urls.size() > 0) {
            try {
                Method m =
                    URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
                m.setAccessible(true);
                for (URL url : urls) {
                    m.invoke(loader, url);
                }
            } catch (Throwable cause) {
                ThrowableUtil.wrapAndThrow(cause);
            }
        }
    }

    /**
     * Returns a lib directory.
     * 
     * @return a lib directory
     */
    protected static File getLibDir() {
        try {
            return new File(URLDecoder.decode(ApiProxy.class
                .getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .getFile(), "UTF-8")).getParentFile().getParentFile();
        } catch (UnsupportedEncodingException e) {
            throw new WrapRuntimeException(e);
        }

    }

    /**
     * Gets the library {@link URL}.
     * 
     * @param dir
     *            the directory
     * @param libName
     *            the name of the library
     * @return the library {@link URL}
     */
    protected static URL getLibraryURL(File dir, String libName) {
        File file = new File(dir, libName);
        if (!file.exists()) {
            throw new IllegalArgumentException("The library("
                + libName
                + ") is not found in the directory("
                + dir.getAbsolutePath()
                + ").");
        }
        try {
            return file.toURI().toURL();
        } catch (MalformedURLException e) {
            throw new WrapRuntimeException(e);
        }
    }

    /**
     * Prepares local services.
     * 
     * @param loader
     *            the {@link ClassLoader}
     */
    @SuppressWarnings("unchecked")
    protected static void prepareLocalServices(ClassLoader loader) {
        try {
            Class<?> apiProxyLocalImplClass =
                loader.loadClass(API_PROXY_LOCAL_IMPL_CLASS_NAME);
            Constructor<?> con =
                apiProxyLocalImplClass.getDeclaredConstructor(File.class);
            con.setAccessible(true);
            apiProxyLocalImpl =
                (Delegate<Environment>) con.newInstance(new File(
                    "build/test-classes"));
        } catch (Throwable cause) {
            ThrowableUtil.wrapAndThrow(cause);
        }
    }

    /**
     * Sets up this tester.
     * 
     * @throws Exception
     *             if an exception has occurred
     * 
     */
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        originalDelegate = ApiProxy.getDelegate();
        originalEnvironment = ApiProxy.getCurrentEnvironment();
        if (AppEngineUtil.isServer()) {
            parentDelegate = originalDelegate;
            environment = new TestEnvironment(originalEnvironment);
        } else {
            parentDelegate = apiProxyLocalImpl;
            environment = new TestEnvironment();
        }
        ApiProxy.setDelegate(this);
        ApiProxy.setEnvironmentForCurrentThread(environment);
    }

    /**
     * Tears down this tester.
     * 
     * @throws Exception
     *             if an exception has occurred
     */
    public void tearDown() throws Exception {
        DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
        for (Transaction tx : ds.getActiveTransactions()) {
            tx.rollback();
        }
        if (!putKeys.isEmpty()) {
            ds.delete(putKeys);
            putKeys.clear();
        }
        mailMessages.clear();
        ApiProxy.setDelegate(originalDelegate);
        ApiProxy.setEnvironmentForCurrentThread(originalEnvironment);
    }

    public byte[] makeSyncCall(Environment env, String service, String method,
            byte[] requestBuf) throws ApiProxyException {
        if (service.equals(URLFETCH_SERVICE)
            && method.equals(FETCH_METHOD)
            && urlFetchHandler != null) {
            try {
                URLFetchRequest requestPb =
                    URLFetchRequest.parseFrom(requestBuf);
                return URLFetchResponse
                    .newBuilder()
                    .setContent(
                        ByteString.copyFrom(urlFetchHandler
                            .getContent(requestPb)))
                    .setStatusCode(urlFetchHandler.getStatusCode(requestPb))
                    .build()
                    .toByteArray();
            } catch (Exception e) {
                ThrowableUtil.wrapAndThrow(e);
            }
        } else if (service.equals(TASKQUEUE_SERVICE)
            && method.startsWith(ADD_METHOD)) {
            TaskQueueAddRequest taskPb = new TaskQueueAddRequest();
            taskPb.mergeFrom(requestBuf);
            tasks.add(taskPb);
            TaskQueueAddResponse responsePb = new TaskQueueAddResponse();
            return responsePb.toByteArray();
        }
        byte[] responseBuf =
            parentDelegate.makeSyncCall(env, service, method, requestBuf);
        if (DATASTORE_SERVICE.equals(service) && PUT_METHOD.equals(method)) {
            PutResponse response = new PutResponse();
            response.mergeFrom(responseBuf);
            for (Reference r : response.keys()) {
                putKeys.add(DatastoreUtil.referenceToKey(r));
            }
        } else if (service.equals(MAIL_SERVICE)
            && method.startsWith(SEND_METHOD)) { // Send[ToAdmins]
            MailMessage messagePb = new MailMessage();
            messagePb.mergeFrom(requestBuf);
            mailMessages.add(messagePb);
        }
        return responseBuf;
    }

    public Future<byte[]> makeAsyncCall(Environment env, String service,
            String method, byte[] requestBuf, ApiConfig config) {
        Future<byte[]> future =
            parentDelegate.makeAsyncCall(
                env,
                service,
                method,
                requestBuf,
                config);
        byte[] responseBuf = null;
        try {
            responseBuf = future.get();
        } catch (InterruptedException e) {
            throw ThrowableUtil.wrap(e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw ThrowableUtil.wrap(cause);
        }
        if (DATASTORE_SERVICE.equals(service) && PUT_METHOD.equals(method)) {
            PutResponse response = new PutResponse();
            response.mergeFrom(responseBuf);
            for (Reference r : response.keys()) {
                putKeys.add(DatastoreUtil.referenceToKey(r));
            }
        }
        return future;
    }

    public void log(Environment env, LogRecord rec) {
        parentDelegate.log(env, rec);
    }

    /**
     * Sets {@link URLFetchHandler}.
     * 
     * @param urlFetchHandler
     *            the {@link URLFetchHandler}
     */
    public void setUrlFetchHandler(URLFetchHandler urlFetchHandler) {
        this.urlFetchHandler = urlFetchHandler;
    }

    /**
     * Counts the number of the model.
     * 
     * @param modelClass
     *            the model class
     * @return the number of the model
     * @throws NullPointerException
     *             if the modelClass parameter is null
     */
    public int count(Class<?> modelClass) throws NullPointerException {
        if (modelClass == null) {
            throw new NullPointerException("The modelClass parameter is null.");
        }
        return count(modelClass.getSimpleName());
    }

    /**
     * Counts the number of the entity.
     * 
     * @param kind
     *            the kind
     * @return the number of the model
     * @throws NullPointerException
     *             if the kind parameter is null
     */
    public int count(String kind) throws NullPointerException {
        if (kind == null) {
            throw new NullPointerException("The kind parameter is null.");
        }
        DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
        return ds.prepare(new Query(kind)).countEntities();
    }
}