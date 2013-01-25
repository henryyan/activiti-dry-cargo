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
import java.util.Map;
import java.util.Set;

import org.activiti.engine.delegate.Expression;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.history.HistoricVariableInstance;
import org.activiti.engine.impl.RepositoryServiceImpl;
import org.activiti.engine.impl.bpmn.behavior.UserTaskActivityBehavior;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.activiti.engine.impl.pvm.delegate.ActivityBehavior;
import org.activiti.engine.impl.pvm.process.ActivityImpl;
import org.activiti.engine.impl.task.TaskDefinition;
import org.activiti.engine.management.TablePage;
import org.activiti.engine.task.Task;
import org.apache.ibatis.session.SqlSession;

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

  /**
   * 多实例（会签）加签
   * 
   * @param taskId
   *          任务ID，多实例任务的任何一个都可以
   * @param userIds
   *          加签的人员
   */
  public void addSign(String taskId, String... userIds) {

    // open new session
    SqlSession sqlSession = getSqlSession();

    try {
      Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
      String processInstanceId = task.getProcessInstanceId();
      String taskDefinitionKey = task.getTaskDefinitionKey();

      // 插入ACT_RU_EXECUTION
      // 获取针对多实例的运行时流程实例
      ExecutionEntity executionEntityOfMany = (ExecutionEntity) runtimeService
              .createNativeExecutionQuery()
              .sql("select * from ACT_RU_EXECUTION where PARENT_ID_ = (select ID_ from ACT_RU_EXECUTION where PARENT_ID_ = '" + processInstanceId
                      + "') limit 1").singleResult();
      String processDefinitionId = executionEntityOfMany.getProcessDefinitionId();

      // 获取ID
      TablePage listPage = managementService.createTablePageQuery().tableName("ACT_GE_PROPERTY").listPage(2, 2);
      Map<String, Object> idMap = listPage.getRows().get(0);

      // 当前的ID
      Long nextId = Long.parseLong(idMap.get("VALUE_").toString());

      for (String userId : userIds) {

        // 处理重复加签
        long count = taskService.createTaskQuery().taskDefinitionKey(taskDefinitionKey).processInstanceId(processInstanceId).taskAssignee(userId).count();
        if (count > 0) {
          logger.warn("忽略重复加签，用户：{}, 任务：{}", userId, taskDefinitionKey);
          continue;
        }

        // 插入一条新的运行时实例
        Long newExecutionId = ++nextId;
        StringBuilder insertExecution = new StringBuilder();
        insertExecution.append("insert into ACT_RU_EXECUTION values(");
        insertExecution.append(newExecutionId); // ID
        insertExecution.append(", 1"); // REV_
        insertExecution.append(", " + processInstanceId); // PROC_INST_ID_
        insertExecution.append(", " + executionEntityOfMany.getBusinessKey()); // BUSINESS_KEY_
        insertExecution.append(", " + executionEntityOfMany.getParentId()); // PARENT_ID_
        insertExecution.append(", '" + processDefinitionId + "'"); // PROC_DEF_ID_
        insertExecution.append(", " + executionEntityOfMany.getSuperExecutionId()); // SUPER_EXEC_
        insertExecution.append(", '" + executionEntityOfMany.getActivityId() + "'"); // ACT_ID_
        insertExecution.append(", 'TRUE'"); // IS_ACTIVE_
        insertExecution.append(", 'TRUE'"); // IS_CONCURRENT_
        insertExecution.append(", 'FALSE'"); // IS_SCOPE_
        insertExecution.append(", 'FALSE'"); // IS_EVENT_SCOPE_
        insertExecution.append(", '1'"); // SUSPENSION_STATE_
        insertExecution.append(", " + executionEntityOfMany.getCachedEntityState()); // CACHED_ENT_STATE_

        insertExecution.append(")");
        sqlSession.getConnection().createStatement().execute(insertExecution.toString());

        // 创建任务
        // runtime task
        Long newTaskId = ++nextId;
        StringBuilder insertRuntimeTask = new StringBuilder();
        insertRuntimeTask.append("insert into act_ru_task values(");
        insertRuntimeTask.append(newTaskId + ", 1, " + newExecutionId + ", " + processInstanceId);
        insertRuntimeTask.append(", '" + processDefinitionId + "'");
        insertRuntimeTask.append(", '" + task.getName() + "', null, null");
        insertRuntimeTask.append(", '" + taskDefinitionKey + "'");
        insertRuntimeTask.append(", null");
        insertRuntimeTask.append(", '" + userId + "'");
        insertRuntimeTask.append(", null, 50, sysdate, null, '1'");
        insertRuntimeTask.append(")");
        sqlSession.getConnection().createStatement().execute(insertRuntimeTask.toString());

        // history task
        StringBuilder insertHistoryTask = new StringBuilder();
        insertHistoryTask.append("insert into act_hi_taskinst values(");
        insertHistoryTask.append(newTaskId);
        insertHistoryTask.append(",'" + processDefinitionId + "'");
        insertHistoryTask.append(", '" + taskDefinitionKey + "'");
        insertHistoryTask.append(", " + processInstanceId + ", " + newExecutionId);
        insertHistoryTask.append(", null");
        insertHistoryTask.append(",'" + task.getName() + "'");
        insertHistoryTask.append(", null");
        insertHistoryTask.append(", null");
        insertHistoryTask.append(", '" + userId + "'");
        insertHistoryTask.append(", sysdate");
        insertHistoryTask.append(", null");
        insertHistoryTask.append(", null");
        insertHistoryTask.append(", null");
        insertHistoryTask.append(", 50");
        insertHistoryTask.append(", null)");
        sqlSession.getConnection().createStatement().execute(insertHistoryTask.toString());

        // 更新主键ID
        String updateNextId = "update ACT_GE_PROPERTY set VALUE_ = " + nextId + " where NAME_ = 'next.dbid'";
        sqlSession.getConnection().createStatement().execute(updateNextId);

        // 更新多实例相关变量
        List<HistoricVariableInstance> list = historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstanceId)
                .variableNameLike("nrOf%").list();
        Integer nrOfInstances = 0;
        Integer nrOfActiveInstances = 0;
        for (HistoricVariableInstance var : list) {
          if (var.getVariableName().equals("nrOfInstances")) {
            nrOfInstances = (Integer) var.getValue();
          } else if (var.getVariableName().equals("nrOfActiveInstances")) {
            nrOfActiveInstances = (Integer) var.getValue();
          }
        }

        nrOfInstances++;
        nrOfActiveInstances++;

        String updateVariablesOfMultiinstance = "update ACT_HI_VARINST set LONG_ = " + nrOfInstances + ", TEXT_ = " + nrOfInstances + " where EXECUTION_ID_ = "
                + executionEntityOfMany.getParentId() + " and NAME_ = 'nrOfInstances'";
        sqlSession.getConnection().createStatement().execute(updateVariablesOfMultiinstance);

        updateVariablesOfMultiinstance = "update ACT_HI_VARINST set LONG_ = " + nrOfActiveInstances + ", TEXT_ = " + nrOfActiveInstances
                + " where EXECUTION_ID_ = " + executionEntityOfMany.getParentId() + " and NAME_ = 'nrOfActiveInstances'";
        sqlSession.getConnection().createStatement().execute(updateVariablesOfMultiinstance);

      }

      sqlSession.commit();
    } catch (Exception e) {
      logger.error("failed to add sign for countersign", e);
    } finally {
      sqlSession.close();
    }

  }

}
