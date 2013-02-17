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

import java.io.File;
import java.util.Map;

import me.kafeitu.activiti.extra.helper.ProcessDefinitionHelper;
import me.kafeitu.activiti.extra.utils.OSValidator;

import org.activiti.engine.RepositoryService;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.test.Deployment;
import org.activiti.spring.impl.test.SpringActivitiTestCase;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author henryyan
 */
@ContextConfiguration("classpath:applicationContext-test.xml")
public class ProcessDefinitionHelperTest extends SpringActivitiTestCase {

  @Autowired
  ProcessDefinitionHelper processDefinitionHelper;

  @Autowired
  RepositoryService repositoryService;

  @Test
  @Deployment(resources = "me/kafeitu/activiti/extra/test/runtime/AutoAssignee.bpmn")
  public void testUserTaskKeysByProcessDefinitionKey() throws Exception {
    Map<String, String> keys = processDefinitionHelper.getUserTaskKeysByProcessDefinitionKey("AutoAssignee");
    assertUserTaskKeyAndName(keys);
  }

  @Test
  @Deployment(resources = "me/kafeitu/activiti/extra/test/runtime/AutoAssignee.bpmn")
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

  @Test
  @Deployment(resources = "me/kafeitu/activiti/extra/test/runtime/AutoAssignee.bpmn")
  public void testExportDiagramToFile() throws Exception {
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().latestVersion().singleResult();
    String dir = "/tmp";
    if (OSValidator.isWindows()) {
      dir = "c:\\";
    }
    String exportDiagramToFile = processDefinitionHelper.exportDiagramToFile(processDefinition, dir);
    assertNotNull(exportDiagramToFile);
    File file = new File(exportDiagramToFile);
    assertTrue(file.exists());

    file.deleteOnExit();
  }

}
