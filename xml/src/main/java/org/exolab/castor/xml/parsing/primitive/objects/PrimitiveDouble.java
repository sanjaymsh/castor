/*
 * Copyright 2005 Philipp Erlacher
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
package org.exolab.castor.xml.parsing.primitive.objects;

import org.apache.commons.lang3.StringUtils;

/**
 * This class is part of the command pattern implementation to instantiate an object. It is used as
 * a command by the command invoker {@link PrimitiveObject}.
 * 
 * @author <a href="mailto:philipp DOT erlacher AT gmail DOT com">Philipp Erlacher</a>
 * 
 */
class PrimitiveDouble extends PrimitiveObject {

  @Override
  public Object getObject(Class<?> type, String value) {
    if (StringUtils.isEmpty(value)) {
      return Double.valueOf(0.0);
    }

    if (StringUtils.equals(value, "INF") || StringUtils.equals(value, "Infinity")) {
      return Double.POSITIVE_INFINITY;
    }

    if (StringUtils.equals(value, "-INF") || StringUtils.equals(value, "-Infinity")) {
      return Double.NEGATIVE_INFINITY;
    }

    return Double.valueOf(Double.parseDouble(value));
  }

}
