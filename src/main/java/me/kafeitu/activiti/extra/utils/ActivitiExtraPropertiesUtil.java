/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.kafeitu.activiti.extra.utils;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.ObjectUtils;


/**
 * @author henryyan
 */
public class ActivitiExtraPropertiesUtil {

  private static Map<String, Object> PROPERTIES = new HashMap<String, Object>();
  
  public static void put(String key, Object value) {
    PROPERTIES.put(key, value);
  }
  
  public static Object get(String key) {
    return PROPERTIES.get(key);
  }
  
  public static String getAsString(String key) {
    return ObjectUtils.toString(PROPERTIES.get(key));
  }
  
}
