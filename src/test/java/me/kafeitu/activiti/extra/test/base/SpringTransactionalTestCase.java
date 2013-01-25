package me.kafeitu.activiti.extra.test.base;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;

/**
 * 基于Spring事务管理的测试基类
 * @author henryyan
 */
public abstract class SpringTransactionalTestCase extends AbstractTransactionalJUnit4SpringContextTests {

  protected DataSource dataSource;

  @Override
  @Autowired
  public void setDataSource(DataSource dataSource) {
    super.setDataSource(dataSource);
    this.dataSource = dataSource;
  }

}
