/*
 * Copyright 2006 Werner Guttmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.castor.mapping;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Iterator;

import org.castor.core.CoreProperties;
import org.castor.core.util.AbstractProperties;
import org.exolab.castor.mapping.MappingLoader;
import org.junit.Test;

public class TestMappingLoaderRegistry {

  @Test
  public final void testGetInstance() throws Exception {
    AbstractProperties properties = new CoreProperties();
    MappingLoaderRegistry registry = new MappingLoaderRegistry(properties);
    assertNotNull(registry);
  }

  @Test
  public final void testEnlistMappingLoaders() throws Exception {
    AbstractProperties properties = new CoreProperties();
    MappingLoaderRegistry registry = new MappingLoaderRegistry(properties);
    assertNotNull(registry);

    Collection<MappingLoaderFactory> factories = registry.getMappingLoaderFactories();
    assertNotNull(factories);
    assertTrue(factories.size() > 0);
    assertEquals(2, factories.size());

    Iterator<MappingLoaderFactory> iter = factories.iterator();

    MappingLoaderFactory factory = (MappingLoaderFactory) iter.next();
    assertNotNull(factory);
    assertEquals("JDO", factory.getName());
    assertEquals("org.castor.mapping.JDOMappingLoaderFactory", factory.getClass().getName());
    assertEquals("CastorXmlMapping", factory.getSourceType());

    factory = (MappingLoaderFactory) iter.next();
    assertNotNull(factory);
    assertEquals("XML", factory.getName());
    assertEquals("org.castor.mapping.XMLMappingLoaderFactory", factory.getClass().getName());
    assertEquals("CastorXmlMapping", factory.getSourceType());

  }

  @Test
  public final void testGetXMLMappingLoader() throws Exception {
    AbstractProperties properties = new CoreProperties();
    MappingLoaderRegistry registry = new MappingLoaderRegistry(properties);
    assertNotNull(registry);

    MappingLoader mappingLoader = registry.getMappingLoader("CastorXmlMapping", BindingType.XML);
    assertNotNull(mappingLoader);
    assertEquals(mappingLoader.getClass().getName(), "org.exolab.castor.xml.XMLMappingLoader");
  }

}
