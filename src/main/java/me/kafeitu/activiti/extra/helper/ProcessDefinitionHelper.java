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

package me.kafeitu.activiti.extra.helper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.activiti.engine.impl.RepositoryServiceImpl;
import org.activiti.engine.impl.bpmn.behavior.UserTaskActivityBehavior;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.activiti.engine.impl.pvm.delegate.ActivityBehavior;
import org.activiti.engine.impl.pvm.process.ActivityImpl;
import org.activiti.engine.impl.task.TaskDefinition;
import org.activiti.engine.repository.ProcessDefinition;
import org.apache.commons.io.FileUtils;

/**
 * 流程定义相关的工具
 * 
 * @author henryyan
 */
public class ProcessDefinitionHelper extends AbstractHelper {

  /**
   * 读取流程中任务名称
   * 
   * @param processDefinitionKey
   *          流程定义的Key
   * @return Map<英文名称, 中文名称>
   */
  public Map<String, String> getUserTaskKeysByProcessDefinitionKey(String processDefinitionKey) {
    Map<String, String> keys = new HashMap<String, String>();
    List<ProcessDefinition> definitions = repositoryService.createProcessDefinitionQuery().processDefinitionKey(processDefinitionKey).list();
    for (ProcessDefinition processDefinition : definitions) {
      ProcessDefinitionEntity processDefinitionEntity = (ProcessDefinitionEntity) ((RepositoryServiceImpl) repositoryService)
              .getDeployedProcessDefinition(processDefinition.getId());
      List<ActivityImpl> activitiList = processDefinitionEntity.getActivities();

      setSingleUserTaskActivity(keys, activitiList);
    }
    return keys;
  }

  /**
   * 读取流程中任务名称
   * 
   * @param processDefinitionId
   *          流程定义的ID
   * @return Map<英文名称, 中文名称>
   */
  public Map<String, String> getUserTaskKeysByProcessDefinitionId(String processDefinitionId) {
    Map<String, String> keys = new HashMap<String, String>();
    ProcessDefinitionEntity processDefinitionEntity = (ProcessDefinitionEntity) ((RepositoryServiceImpl) repositoryService)
            .getDeployedProcessDefinition(processDefinitionId);
    List<ActivityImpl> activitiList = processDefinitionEntity.getActivities();

    setSingleUserTaskActivity(keys, activitiList);
    return keys;
  }

  private void setSingleUserTaskActivity(Map<String, String> keys, List<ActivityImpl> activitiList) {
    for (ActivityImpl activity : activitiList) {
      ActivityBehavior activityBehavior = activity.getActivityBehavior();
      if (activityBehavior instanceof UserTaskActivityBehavior) {
        UserTaskActivityBehavior userTaskActivityBehavior = (UserTaskActivityBehavior) activityBehavior;
        TaskDefinition taskDefinition = userTaskActivityBehavior.getTaskDefinition();
        keys.put(taskDefinition.getKey(), taskDefinition.getNameExpression().toString());
      }
    }
  }

  /**
   * 导出图片文件到硬盘
   * 
   * @return 文件的全路径
   */
  public String exportDiagramToFile(ProcessDefinition processDefinition, String exportDir) throws IOException {
    String diagramResourceName = processDefinition.getDiagramResourceName();
    String key = processDefinition.getKey();
    int version = processDefinition.getVersion();
    String diagramPath = "";

    InputStream resourceAsStream = repositoryService.getResourceAsStream(processDefinition.getDeploymentId(), diagramResourceName);
    byte[] b = new byte[resourceAsStream.available()];

    @SuppressWarnings("unused")
    int len = -1;
    resourceAsStream.read(b, 0, b.length);

    // create file if not exist
    String diagramDir = exportDir + "/" + key + "/" + version;
    File diagramDirFile = new File(diagramDir);
    if (!diagramDirFile.exists()) {
      diagramDirFile.mkdirs();
    }
    if (diagramResourceName.contains("/")) {
      diagramResourceName = diagramResourceName.replaceAll("/", ".");
    }
    diagramPath = diagramDir + "/" + diagramResourceName;
    File file = new File(diagramPath);

    // 文件存在退出
    if (file.exists()) {
      // 文件大小相同时直接返回否则重新创建文件(可能损坏)
      logger.debug("diagram exist, ignore... : {}", diagramPath);
      return diagramPath;
    } else {
      file.createNewFile();
    }

    logger.debug("export diagram to : {}", diagramPath);

    // wirte bytes to file
    FileUtils.writeByteArrayToFile(file, b, true);
    return diagramPath;
  }

}
