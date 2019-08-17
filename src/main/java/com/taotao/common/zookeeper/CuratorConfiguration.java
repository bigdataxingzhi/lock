package com.taotao.common.zookeeper;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryNTimes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.taotao.lock.zookeeper.ZookeeperLock;
import com.taotao.util.zookeeper.ZkCurator;

/**
 * Curator与spring整合
 * @author 星志
 *
 */
@Configuration
@ConditionalOnClass(org.apache.curator.framework.CuratorFrameworkFactory.class)
public class CuratorConfiguration {
	
	@Value("${zookeeper.host}")
	private String zkHost;
	
	@Value("${zookeeper.port}")
	private String zkPort;
	/**
	 * 构造重试策略
	 * @return
	 */
	@Bean("retryPolicy")
	public RetryNTimes getRetryNTimes() {
		/**
		 * 第一个参数表示重试次数
		 * 第二个参数表示重试间隔
		 */
		return new RetryNTimes(10, 5000);
	}
	
	/**
	 * 构造CuratorFramework客户端
	 * /**
     * Create a new client
     *
     * @param connectString       list of servers to connect to
     * @param sessionTimeoutMs    session timeout
     * @param connectionTimeoutMs connection timeout
     * @param retryPolicy         retry policy to use
     * @return client
     */
	@Bean(initMethod="start",name="curatorFramework")
	public CuratorFramework getCuratorFramework(RetryNTimes retryPolicy) {
		return CuratorFrameworkFactory.newClient(new StringBuffer().append(zkHost).append(":").append(zkPort).toString(),10000,5000, retryPolicy);
	}
	
	@Bean(initMethod="init",name="zkClient")
	public ZkCurator getZKClient(CuratorFramework curatorFramework) {
		
		return new ZkCurator(curatorFramework);
	}
	
	@Bean(initMethod="init",name="zookeeperLock")
	public ZookeeperLock getZookeeperLock(CuratorFramework curatorFramework) {
		
		return new ZookeeperLock(curatorFramework);
	}
	
}