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
package org.slim3.commons.util;

import junit.framework.TestCase;

/**
 * @author higa
 * 
 */
public class ByteArrayUtilTest extends TestCase {

    /**
     * @throws Exception
     */
    public void testToString() throws Exception {
        assertEquals("null", ByteArrayUtil.toString(null));
        System.out.println(ByteArrayUtil.toString(new byte[1]));
    }

    /**
     * @throws Exception
     */
    public void testToByteArray() throws Exception {
        assertNull(ByteArrayUtil.toByteArray(null));
        assertNotNull(ByteArrayUtil.toByteArray("aaa"));
    }

    /**
     * @throws Exception
     */
    public void testToObject() throws Exception {
        assertNull(ByteArrayUtil.toObject(null));
        assertEquals("aaa", ByteArrayUtil.toObject(ByteArrayUtil
                .toByteArray("aaa")));
    }
}