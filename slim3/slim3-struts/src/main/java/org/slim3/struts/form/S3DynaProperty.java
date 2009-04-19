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
package org.slim3.struts.form;

import org.apache.commons.beanutils.DynaProperty;
import org.slim3.commons.bean.PropertyDesc;

/**
 * {@link DynaProperty} for Slim3.
 * 
 * @author higa
 * @since 3.0
 * 
 */
public class S3DynaProperty extends DynaProperty {

    private static final long serialVersionUID = 1L;

    /**
     * The property descriptor.
     */
    protected PropertyDesc propertyDesc;

    /**
     * Constructor.
     * 
     * @param propertyDesc
     *            the property descriptor
     */
    public S3DynaProperty(PropertyDesc propertyDesc) {
        super(propertyDesc.getPropertyName(), propertyDesc.getPropertyClass());
        this.propertyDesc = propertyDesc;
    }

    @Override
    public boolean isIndexed() {
        return false;
    }

    @Override
    public boolean isMapped() {
        return false;
    }

    /**
     * Returns the property descriptor.
     * 
     * @return the property descriptor
     */
    public PropertyDesc getPropertyDesc() {
        return propertyDesc;
    }
}