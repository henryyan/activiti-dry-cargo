package me.kafeitu.activiti.extra.test.runtime;

import static org.junit.Assert.assertNotNull;

import java.io.FileInputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import me.kafeitu.activiti.extra.helper.RuntimeTaskHelper;
import me.kafeitu.activiti.extra.test.base.SpringTransactionalTestCase;

import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.impl.util.ReflectUtil;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(locations = { "/applicationContext.xml" })
public class RuntimeTaskHelperTest extends SpringTransactionalTestCase {

  @Autowired
  RuntimeTaskHelper runtimeTaskHelper;

  /**
   * 自动签收测试
   */
  @Test
  public void testAutoClaim() throws Exception {
    RepositoryService repositoryService = runtimeTaskHelper.getRepositoryService();
    URL resource = ReflectUtil.getResource("diagrams/AutoAssignee.bpmn");
    assertNotNull(resource);

    repositoryService.createDeployment().addInputStream("AutoAssignee.bpmn20.xml", new FileInputStream(resource.getPath())).deploy();
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

}
