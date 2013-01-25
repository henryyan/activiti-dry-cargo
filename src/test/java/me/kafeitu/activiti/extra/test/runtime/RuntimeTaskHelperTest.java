package me.kafeitu.activiti.extra.test.runtime;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.kafeitu.activiti.extra.helper.RuntimeTaskHelper;

import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.activiti.engine.test.Deployment;
import org.activiti.spring.impl.test.SpringActivitiTestCase;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration("classpath:applicationContext-test.xml")
public class RuntimeTaskHelperTest extends SpringActivitiTestCase {

  @Autowired
  RuntimeTaskHelper runtimeTaskHelper;

  /**
   * 自动签收测试
   */
  @Test
  @Deployment(resources = "me/kafeitu/activiti/extra/test/runtime/AutoAssignee.bpmn")
  public void testAutoClaim() throws Exception {
    RuntimeService runtimeService = runtimeTaskHelper.getRuntimeService();
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("AutoAssignee");
    assertNotNull(processInstance.getId());
    System.out.println("id " + processInstance.getId() + " " + processInstance.getProcessDefinitionId());

    TaskService taskService = runtimeTaskHelper.getTaskService();
    Map<String, Object> vars = new HashMap<String, Object>();

    // 签收并完成第一个任务
    Task task = taskService.createTaskQuery().singleResult();
    taskService.claim(task.getId(), "user1");
    vars.put("taskTwoAssignee", "user2");
    taskService.complete(task.getId(), vars);

    // 完成第二个任务
    Task task2 = taskService.createTaskQuery().taskAssignee("user2").singleResult();
    vars = new HashMap<String, Object>();
    vars.put("pass", false);
    taskService.complete(task2.getId(), vars);

    // 验证任务回到第一个节点
    Task taskOne = taskService.createTaskQuery().taskName("Task One").singleResult();
    assertNotNull(taskOne);

    // 自动签收
    runtimeTaskHelper.autoClaim(processInstance.getId());

    // 验证是否已自动签收
    taskOne = taskService.createTaskQuery().taskName("Task One").taskAssignee("user1").singleResult();
    assertNotNull(taskOne);
  }

  /**
   * 加签测试
   * @throws SQLException 
   */
  @Test
  @Deployment(resources = "me/kafeitu/activiti/extra/test/runtime/dispatch.bpmn")
  public void testAddSign() throws SQLException {
    // 启动流程
    Map<String, String> variableMap = new HashMap<String, String>();

    variableMap.put("countersignUsers", "user1,user2,user3,user4");
    variableMap.put("rate", "100");
    variableMap.put("incept", "国务院");
    variableMap.put("content", "民主制");

    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionKey("dispatch").singleResult();
    assertNotNull(processDefinition);
    ProcessInstance processInstance = formService.submitStartFormData(processDefinition.getId(), variableMap);
    assertNotNull(processInstance.getId());

    // 验证任务实例
    List<Task> list = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
    assertEquals(4, list.size());
    Task originTask = list.get(0);

    // 验证历史任务数量
    long count = historyService.createHistoricTaskInstanceQuery().count();
    assertEquals(4, count);

    // 加签
    runtimeTaskHelper.addSign(originTask.getId(), "user5");

    // 加签收验证
    count = historyService.createHistoricTaskInstanceQuery().count();
    assertEquals(5, count);
  }

}
