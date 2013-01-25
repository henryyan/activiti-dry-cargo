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

import me.kafeitu.activiti.extra.helper.ProcessDefinitionHelper;
import me.kafeitu.activiti.extra.helper.ProcessInstanceHelper;
import me.kafeitu.activiti.extra.helper.RuntimeTaskHelper;

/**
 * @author henryyan
 */
public class ActivitiExtra {

  // HELPERS ///////////////////////////////////////////////////////////////////
  protected RuntimeTaskHelper runtimeTaskHelper;
  protected ProcessInstanceHelper processInstanceHelper;
  protected ProcessDefinitionHelper processDefinitionHelper;

  protected ActivitiExtraConfiguration activitiExtraConfiguration;

  public ActivitiExtra(ActivitiExtraConfiguration activitiExtraConfiguration) {
    this.activitiExtraConfiguration = activitiExtraConfiguration;
    runtimeTaskHelper = activitiExtraConfiguration.getRuntimeTaskHelper();
    processInstanceHelper = activitiExtraConfiguration.getProcessInstanceHelper();
    processDefinitionHelper = activitiExtraConfiguration.getProcessDefinitionHelper();
  }

  // getters and setters //////////////////////////////////////////////////////

  public RuntimeTaskHelper getRuntimeTaskHelper() {
    return runtimeTaskHelper;
  }

  public ProcessInstanceHelper getProcessInstanceHelper() {
    return processInstanceHelper;
  }

  public ProcessDefinitionHelper getProcessDefinitionHelper() {
    return processDefinitionHelper;
  }

}
