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

package me.kafeitu.activiti.extra.test.base;

import static org.junit.Assert.assertNotNull;
import me.kafeitu.activiti.extra.ActivitiExtraConfiguration;
import me.kafeitu.activiti.extra.helper.AbstractHelper;
import me.kafeitu.activiti.extra.helper.ProcessInstanceHelper;
import me.kafeitu.activiti.extra.helper.RuntimeTaskHelper;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

/**
 * ActivitiExtraConfiguration测试
 * 
 * @author henryyan
 */
@ContextConfiguration(locations = { "/applicationContext.xml" })
public class ActivitiExtraConfigurationTest extends SpringTransactionalTestCase {

  @Autowired
  ActivitiExtraConfiguration activitiExtraConfiguration;
  
  @Autowired
  RuntimeTaskHelper runtimeTaskHelper;
  
  @Autowired
  ProcessInstanceHelper processInstanceHelper;

  @Test
  public void activitiExtraConfigurationTest() {
    
    // base test
    assertNotNull(activitiExtraConfiguration);
    
    // activiti services assert
    assertNotNull(activitiExtraConfiguration.getFormService());
    assertNotNull(activitiExtraConfiguration.getHistoryService());
    assertNotNull(activitiExtraConfiguration.getIdentityService());
    assertNotNull(activitiExtraConfiguration.getManagementService());
    assertNotNull(activitiExtraConfiguration.getRepositoryService());
    assertNotNull(activitiExtraConfiguration.getRuntimeService());
    assertNotNull(activitiExtraConfiguration.getTaskService());
    
    // helper assert
    assertNotNull(runtimeTaskHelper);
    assertActivitiServiceInHelper(runtimeTaskHelper);
    
    assertNotNull(processInstanceHelper);
    assertActivitiServiceInHelper(processInstanceHelper);
  }
  
  /**
   * 验证每个Helper的Activiti Service是否已经注入
   */
  private void assertActivitiServiceInHelper(AbstractHelper helper) {
    assertNotNull(helper.getFormService());
    assertNotNull(helper.getHistoryService());
    assertNotNull(helper.getIdentityService());
    assertNotNull(helper.getManagementService());
    assertNotNull(helper.getRepositoryService());
    assertNotNull(helper.getRuntimeService());
    assertNotNull(helper.getTaskService());
  }

}
