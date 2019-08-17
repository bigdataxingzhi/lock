package com.taotao.lock.zookeeper;

import java.util.concurrent.CountDownLatch;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.framework.recipes.cache.PathChildrenCache.StartMode;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs.Ids;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZookeeperLock {

	private static final Logger log = LoggerFactory.getLogger(ZookeeperLock.class);
	private CuratorFramework client = null;
	
	//用于挂起当前请求,并等待上一个分布式锁释放
	private static CountDownLatch zkCountDownLatch = new CountDownLatch(1);
	
	//分布式锁总节点名
	private static final String ZK_LOCK_PROJECT = "taotao-zklock";

	//分布式锁节点
	private static final String DISTRIBUTED_LOCK = "distributed-lock";

	public ZookeeperLock(CuratorFramework client) {
		super();
		this.client = client;
	}

	/**
	 * 初始化锁
	 */
	public void init() {
		//使用命名空间
		client = client.usingNamespace("zkLocks-Namespace");
		/**
		 * 结构如下:
		 * 
		 * zkLocks-Namespace
		 *     ------ZK_LOCK_PROJECT
		 *          ------DISTRIBUTED_LOCK
		 */
		
		try {
			if(client.checkExists().forPath("/"+ZK_LOCK_PROJECT)==null) {
				client.create()
						.creatingParentsIfNeeded()
						  .withMode(CreateMode.PERSISTENT)
						   .withACL(Ids.OPEN_ACL_UNSAFE)
						    .forPath("/"+ZK_LOCK_PROJECT);
			}
			//获取分布式锁
			addWatcherToLock("/"+ZK_LOCK_PROJECT);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 获取分布式锁
	 */
	public void getLock() {
		//当且仅当上一个锁释放且当前请求获取锁之后,才会跳出
		while(true) {
			try {
				client.create()
				.creatingParentsIfNeeded()
				  .withMode(CreateMode.EPHEMERAL)
				   .withACL(Ids.OPEN_ACL_UNSAFE)
				    .forPath("/"+ZK_LOCK_PROJECT+"/"+DISTRIBUTED_LOCK);
				log.info("获取分布式锁成功");
				return ;
			} catch (Exception e) {
				log.info("获取分布式锁失败");
				try {
				if (zkCountDownLatch.getCount() <= 0) {
					zkCountDownLatch = new CountDownLatch(1);
				}
				//阻塞线程
					zkCountDownLatch.await();
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * 监听事件
	 * @throws Exception 
	 */
	public void addWatcherToLock(String path) throws Exception {
		// cacheData: 设置缓存节点的数据状态
		final PathChildrenCache cache = new PathChildrenCache(client, path, true);
		/**
		 * StartMode: 初始化方式
		 * POST_INITIALIZED_EVENT：异步初始化，初始化之后会触发事件
		 * NORMAL：异步初始化
		 * BUILD_INITIAL_CACHE：同步初始化
		 */
		cache.start(StartMode.POST_INITIALIZED_EVENT);
		cache.getListenable().addListener(new PathChildrenCacheListener() {
			@Override
			public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
				if(event.getType().equals(PathChildrenCacheEvent.Type.CHILD_REMOVED)) {
					String path = event.getData().getPath();
					log.info("上一个会话已经释放锁或者会话已经断开,节点路径为:{}",path);
				if(path.contains(DISTRIBUTED_LOCK)) {
					log.info("释放计数器,让当前请求争夺分布式锁");
					zkCountDownLatch.countDown();
				}
				}
			}
		});
	}
	
	/**
	 * 释放锁
	 */
	public boolean releaseLock() {
		try {
			if(client.checkExists().forPath("/"+ZK_LOCK_PROJECT+"/"+DISTRIBUTED_LOCK)!=null) {
				client.delete().forPath("/"+ZK_LOCK_PROJECT+"/"+DISTRIBUTED_LOCK);
			}
		} catch (Exception e) {
			log.info("分布式锁释放失败");
			return false;
		}
		log.info("分布式锁释放完毕");
		return true;
	}
	
}
