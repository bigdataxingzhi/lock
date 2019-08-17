package com.taotao.util.zookeeper;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.imps.CuratorFrameworkState;


public class ZkCurator {

	private CuratorFramework client;
	
	public ZkCurator(CuratorFramework client) {
		this.client = client;
	}
	
	//初始化操作
	public void init() {
		client = client.usingNamespace("zk-curator-connection");
	}
	
	//得到zk客户端状态
	public CuratorFrameworkState isZkAlive() {
		return client.getState();
	}
	
	

}
