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

package me.kafeitu.activiti.extra.test.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.FileInputStream;
import java.net.URL;
import java.util.Map;

import me.kafeitu.activiti.extra.helper.ProcessDefinitionHelper;

import org.activiti.engine.RepositoryService;
import org.activiti.engine.impl.util.ReflectUtil;
import org.activiti.engine.repository.ProcessDefinition;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

/**
 * @author henryyan
 */
@ContextConfiguration(locations = { "/applicationContext.xml" })
public class ProcessDefinitionHelperTest extends AbstractJUnit4SpringContextTests {

  @Autowired
  ProcessDefinitionHelper processDefinitionHelper;

  @Autowired
  RepositoryService repositoryService;

  @Before
  public void setUp() throws Exception {
    URL resource = ReflectUtil.getResource("diagrams/AutoAssignee.bpmn");
    assertNotNull(resource);

    repositoryService.createDeployment().addInputStream("AutoAssignee.bpmn20.xml", new FileInputStream(resource.getPath())).deploy();
  }

  @Test
  public void testUserTaskKeysByProcessDefinitionKey() throws Exception {
    Map<String, String> keys = processDefinitionHelper.getUserTaskKeysByProcessDefinitionKey("AutoAssignee");
    assertUserTaskKeyAndName(keys);
  }

  @Test
  public void testUserTaskKeysByProcessDefinitionId() throws Exception {
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().latestVersion().singleResult();
    Map<String, String> keys = processDefinitionHelper.getUserTaskKeysByProcessDefinitionId(processDefinition.getId());

    assertUserTaskKeyAndName(keys);
  }

  private void assertUserTaskKeyAndName(Map<String, String> keys) {
    assertEquals(2, keys.size());
    assertEquals(keys.get("usertask1"), "Task One");
    assertEquals(keys.get("usertask2"), "Task Two");
  }

}
