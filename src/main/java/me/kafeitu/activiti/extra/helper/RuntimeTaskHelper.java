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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
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
    SqlSession sqlSession = openSession();
    PreparedStatement ps = null;

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

      Connection connection = sqlSession.getConnection();

      for (String userId : userIds) {

        // 处理重复加签
        long count = taskService.createTaskQuery().taskDefinitionKey(taskDefinitionKey).processInstanceId(processInstanceId).taskAssignee(userId).count();
        if (count > 0) {
          logger.warn("忽略重复加签，用户：{}, 任务：{}", userId, taskDefinitionKey);
          continue;
        }

        // 插入一条新的运行时实例
        Long newExecutionId = ++nextId;

        int counter = 1;
        ps = connection.prepareStatement("insert into ACT_RU_EXECUTION values(?, 1, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
        ps.setString(counter++, newExecutionId.toString()); // ID
        ps.setString(counter++, processInstanceId); // PROC_INST_ID_
        ps.setString(counter++, executionEntityOfMany.getBusinessKey()); // BUSINESS_KEY_
        ps.setString(counter++, executionEntityOfMany.getParentId()); // PARENT_ID_
        ps.setString(counter++, processDefinitionId); // PROC_DEF_ID_
        ps.setString(counter++, executionEntityOfMany.getSuperExecutionId()); // SUPER_EXEC_
        ps.setString(counter++, executionEntityOfMany.getActivityId()); // ACT_ID_
        ps.setString(counter++, "TRUE"); // IS_ACTIVE_
        ps.setString(counter++, "TRUE"); // IS_CONCURRENT_
        ps.setString(counter++, "FALSE"); // IS_SCOPE_
        ps.setString(counter++, "FALSE"); // IS_EVENT_SCOPE_
        ps.setInt(counter++, 1); // SUSPENSION_STATE_
        ps.setInt(counter++, executionEntityOfMany.getCachedEntityState()); // CACHED_ENT_STATE_
        ps.executeUpdate();

        // 创建任务
        // runtime task
        Long newTaskId = ++nextId;
        counter = 1;
        ps = connection.prepareStatement("insert into act_ru_task values(?, 1, ?, ?, ?, ?, null, null, ?, null, ?, null, ?, ?, null, '1')");
        ps.setString(counter++, newTaskId.toString());
        ps.setString(counter++, newExecutionId.toString());
        ps.setString(counter++, processInstanceId);
        ps.setString(counter++, processDefinitionId);
        ps.setString(counter++, task.getName());
        ps.setString(counter++, taskDefinitionKey);
        ps.setString(counter++, userId);
        ps.setInt(counter++, task.getPriority());
        ps.setTimestamp(counter++, new Timestamp(System.currentTimeMillis()));
        ps.executeUpdate();

        // history task
        counter = 1;
        ps = connection.prepareStatement("insert into act_hi_taskinst values(?, ?, ?, ?, ?, null, ?, null, null, ?, ?, null, null, null, ?, null, null, null)");
        ps.setString(counter++, newTaskId.toString());
        ps.setString(counter++, processDefinitionId + "'");
        ps.setString(counter++, taskDefinitionKey + "'");
        ps.setString(counter++, processInstanceId);
        ps.setString(counter++, newExecutionId.toString());
        ps.setString(counter++, task.getName());
        ps.setString(counter++, userId);
        ps.setTimestamp(counter++, new Timestamp(System.currentTimeMillis()));
        ps.setInt(counter++, task.getPriority());
        ps.executeUpdate();

        // 更新主键ID
        String updateNextId = "update ACT_GE_PROPERTY set VALUE_ = ? where NAME_ = ?";
        ps = connection.prepareStatement(updateNextId);
        ps.setLong(1, nextId);
        ps.setString(2, "next.dbid");

        /*
         * 更新多实例相关变量
         */
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

        // 多实例变量加一
        nrOfInstances++;
        nrOfActiveInstances++;

        String updateVariablesOfMultiinstance = "update ACT_HI_VARINST set LONG_ = ?, TEXT_ = ? where EXECUTION_ID_ = ? and NAME_ = ?";
        ps = connection.prepareStatement(updateVariablesOfMultiinstance);
        ps.setLong(1, nrOfInstances);
        ps.setString(2, String.valueOf(nrOfInstances));
        ps.setString(3, executionEntityOfMany.getParentId());
        ps.setString(4, "nrOfInstances");
        ps.executeUpdate();

        ps.setLong(1, nrOfInstances);
        ps.setString(2, String.valueOf(nrOfActiveInstances));
        ps.setString(3, executionEntityOfMany.getParentId());
        ps.setString(4, "nrOfActiveInstances");
        ps.executeUpdate();

      }

      sqlSession.commit();
    } catch (Exception e) {
      logger.error("failed to add sign for countersign", e);
    } finally {
      if (ps != null) {
        try {
          ps.close();
        } catch (SQLException e) {
          logger.error("failed on execute sql", e);
        }
      }
      sqlSession.close();
    }

  }
}
