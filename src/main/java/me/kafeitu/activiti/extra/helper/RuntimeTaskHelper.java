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

import java.util.List;
import java.util.Set;

import org.activiti.engine.delegate.Expression;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.impl.RepositoryServiceImpl;
import org.activiti.engine.impl.bpmn.behavior.UserTaskActivityBehavior;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.activiti.engine.impl.pvm.delegate.ActivityBehavior;
import org.activiti.engine.impl.pvm.process.ActivityImpl;
import org.activiti.engine.impl.task.TaskDefinition;
import org.activiti.engine.task.Task;

/**
 * Task Helper
 * 
 * @author henryyan
 */
public class RuntimeTaskHelper extends AbstractHelper {

  /**
   * 自动签收分配到候选角色、候选组的任务
   * 
   * @param executionId
   *          执行ID
   */
  public void autoClaim(String executionId) {

    // 查询最新的任务
    List<Task> newTasks = taskService.createTaskQuery().executionId(executionId).list();
    for (Task task : newTasks) {

      String taskId = task.getId();

      // 查询并验证任务是否已经分配到具体人
      if (task.getAssignee() != null) {
        logger.info("task [id={}] has assignee: {}", taskId, task.getAssignee());
        return;
      }

      logger.info("task [id={}] has not assignee", taskId);

      // 验证任务是分配到组
      ProcessDefinitionEntity processDefinition = (ProcessDefinitionEntity) ((RepositoryServiceImpl) repositoryService).getDeployedProcessDefinition(task
              .getProcessDefinitionId());

      // 获得当前任务的所有节点
      List<ActivityImpl> activitiList = processDefinition.getActivities();

      for (ActivityImpl activity : activitiList) {
        // 当前节点
        if (task.getTaskDefinitionKey().equals(activity.getId())) {
          ActivityBehavior activityBehavior = activity.getActivityBehavior();

          // 匹配用户任务
          if (activityBehavior instanceof UserTaskActivityBehavior) {
            UserTaskActivityBehavior userTaskActivityBehavior = (UserTaskActivityBehavior) activityBehavior;
            TaskDefinition taskDefinition = userTaskActivityBehavior.getTaskDefinition();
            Set<Expression> candidateUserIdExpressions = taskDefinition.getCandidateUserIdExpressions();
            Set<Expression> candidateGroupIdExpressions = taskDefinition.getCandidateGroupIdExpressions();
            if (!candidateGroupIdExpressions.isEmpty() || !candidateUserIdExpressions.isEmpty()) {

              // 查询历史任务中最新的一条同名记录
              List<HistoricTaskInstance> historicTaskInstances = historyService.createHistoricTaskInstanceQuery()
                      .taskDefinitionKey(task.getTaskDefinitionKey()).executionId(executionId).orderByHistoricTaskInstanceEndTime().desc().list();

              // 没有历史任务时继续下一个循环
              if (historicTaskInstances.isEmpty()) {
                continue;
              }

              HistoricTaskInstance historySameKeyTask = null;
              for (HistoricTaskInstance historicTaskInstance : historicTaskInstances) {
                if (historicTaskInstance.getAssignee() != null) {
                  historySameKeyTask = historicTaskInstance;
                  break;
                }
              }

              // 如果这条任务已经办理过由系统自动签收分配给上一次办理的人
              if (historySameKeyTask != null && historySameKeyTask.getAssignee() != null) {
                String lastAssignee = historySameKeyTask.getAssignee();
                taskService.claim(task.getId(), lastAssignee);
                logger.info("task [id={}] automatic assignee to : {}", taskId, lastAssignee);

              }
            }
          }
        }
      }
    }

  }

}
