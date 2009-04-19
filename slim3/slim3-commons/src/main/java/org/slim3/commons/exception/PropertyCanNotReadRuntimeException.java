/*
 * Copyright 2004-2009 the Seasar Foundation and the Others.
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
package org.slim3.commons.exception;

/**
 * This exception is thrown when an exception is encountered while trying to
 * read the property.
 * 
 * @author higa
 * @since 3.0
 */
public class PropertyCanNotReadRuntimeException extends SRuntimeException {

    static final long serialVersionUID = 1L;

    private Class<?> beanClass;

    private String propertyName;

    /**
     * Constructor.
     * 
     * @param beanClass
     *            the bean class
     * @param propertyName
     *            the property name
     * @param cause
     *            the cause
     */
    public PropertyCanNotReadRuntimeException(Class<?> beanClass,
            String propertyName, Throwable cause) {
        super(cause, "S3Commons-E0009", beanClass.getName(), propertyName,
                convertCauseMessage(cause));
        this.beanClass = beanClass;
        this.propertyName = propertyName;
    }

    /**
     * Returns the bean class.
     * 
     * @return the bean class
     */
    public Class<?> getBeanClass() {
        return beanClass;
    }

    /**
     * Returns the property name.
     * 
     * @return the property name
     */
    public String getPropertyName() {
        return propertyName;
    }
}