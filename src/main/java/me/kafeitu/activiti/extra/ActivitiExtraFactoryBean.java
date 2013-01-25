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

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * @author henryyan
 */
public class ActivitiExtraFactoryBean implements FactoryBean<ActivitiExtra>, DisposableBean, ApplicationContextAware {

  protected ApplicationContext applicationContext;
  protected ActivitiExtraConfiguration activitiExtraConfiguration;
  protected ActivitiExtra activitiExtra;

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
  }

  @Override
  public void destroy() throws Exception {
  }

  @Override
  public ActivitiExtra getObject() throws Exception {
    activitiExtra = activitiExtraConfiguration.buildActivitiExtra();
    return activitiExtra;
  }

  @Override
  public Class< ? > getObjectType() {
    return ActivitiExtra.class;
  }

  @Override
  public boolean isSingleton() {
    return true;
  }

  public ActivitiExtraConfiguration getActivitiExtraConfiguration() {
    return activitiExtraConfiguration;
  }

  public void setActivitiExtraConfiguration(ActivitiExtraConfiguration activitiExtraConfiguration) {
    this.activitiExtraConfiguration = activitiExtraConfiguration;
  }

}
