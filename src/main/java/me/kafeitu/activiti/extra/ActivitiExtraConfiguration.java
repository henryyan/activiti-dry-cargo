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

package me.kafeitu.activiti.extra;

import me.kafeitu.activiti.extra.helper.AbstractHelper;
import me.kafeitu.activiti.extra.helper.ProcessDefinitionHelper;
import me.kafeitu.activiti.extra.helper.ProcessInstanceHelper;
import me.kafeitu.activiti.extra.helper.RuntimeTaskHelper;

import org.activiti.engine.FormService;
import org.activiti.engine.HistoryService;
import org.activiti.engine.IdentityService;
import org.activiti.engine.ManagementService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;

/**
 * @author henryyan
 */
public class ActivitiExtraConfiguration {

  // ACTIVITI EXTRAS PROPERTIES //////////////////////////////////////////

  // ACTIVITI SERVICES ///////////////////////////////////////////////////

  protected RepositoryService repositoryService = null;
  protected RuntimeService runtimeService = null;
  protected HistoryService historyService = null;
  protected IdentityService identityService = null;
  protected TaskService taskService = null;
  protected FormService formService = null;
  protected ManagementService managementService = null;

  // ACTIVITI EXTRA SERVICES /////////////////////////////////////////////
  protected RuntimeTaskHelper runtimeTaskHelper = new RuntimeTaskHelper();
  protected ProcessInstanceHelper processInstanceHelper = new ProcessInstanceHelper();
  protected ProcessDefinitionHelper processDefinitionHelper = new ProcessDefinitionHelper();

  public ActivitiExtra buildActivitiExtra() {
    initRuntimeTaskHelper();
    initProcessInstanceHelper();
    initProcessDefinitionHelper();

    return new ActivitiExtra(this);
  }

  private void initProcessDefinitionHelper() {
    initHelper(processDefinitionHelper);
  }

  private void initProcessInstanceHelper() {
    initHelper(processInstanceHelper);
  }

  private void initRuntimeTaskHelper() {
    initHelper(runtimeTaskHelper);
  }

  /**
   * 设置每个Helper的Activiti Services
   */
  private void initHelper(AbstractHelper helper) {
    helper.setFormService(formService);
    helper.setHistoryService(historyService);
    helper.setIdentityService(identityService);
    helper.setManagementService(managementService);
    helper.setRepositoryService(repositoryService);
    helper.setRuntimeService(runtimeService);
    helper.setTaskService(taskService);
  }

  // getters and setters //////////////////////////////////////////////////////

  public RepositoryService getRepositoryService() {
    return repositoryService;
  }

  public void setRepositoryService(RepositoryService repositoryService) {
    this.repositoryService = repositoryService;
  }

  public RuntimeService getRuntimeService() {
    return runtimeService;
  }

  public void setRuntimeService(RuntimeService runtimeService) {
    this.runtimeService = runtimeService;
  }

  public HistoryService getHistoryService() {
    return historyService;
  }

  public void setHistoryService(HistoryService historyService) {
    this.historyService = historyService;
  }

  public IdentityService getIdentityService() {
    return identityService;
  }

  public void setIdentityService(IdentityService identityService) {
    this.identityService = identityService;
  }

  public TaskService getTaskService() {
    return taskService;
  }

  public void setTaskService(TaskService taskService) {
    this.taskService = taskService;
  }

  public FormService getFormService() {
    return formService;
  }

  public void setFormService(FormService formService) {
    this.formService = formService;
  }

  public ManagementService getManagementService() {
    return managementService;
  }

  public void setManagementService(ManagementService managementService) {
    this.managementService = managementService;
  }

  public RuntimeTaskHelper getRuntimeTaskHelper() {
    return runtimeTaskHelper;
  }

  public void setRuntimeTaskHelper(RuntimeTaskHelper runtimeTaskHelper) {
    this.runtimeTaskHelper = runtimeTaskHelper;
  }

  public ProcessInstanceHelper getProcessInstanceHelper() {
    return processInstanceHelper;
  }

  public void setProcessInstanceHelper(ProcessInstanceHelper processInstanceHelper) {
    this.processInstanceHelper = processInstanceHelper;
  }

  public ProcessDefinitionHelper getProcessDefinitionHelper() {
    return processDefinitionHelper;
  }

  public void setProcessDefinitionHelper(ProcessDefinitionHelper processDefinitionHelper) {
    this.processDefinitionHelper = processDefinitionHelper;
  }

}
