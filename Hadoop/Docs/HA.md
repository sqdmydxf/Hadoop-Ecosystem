## Hadoop中HA设置

**说在前面的话**

	本文设置的HA为手动式的，后期会添加利用Zookeeper设置自动HA

	Hadoop集群环境：完全分布式模式
***
1.**HA作用**
	
	HA解决的是高可用问题，高可用不等同于高性能，高可用是为了可靠性

2.**架构**

	在一个典型的HA集群中，有两台namenode，在任一时刻，只有其中的一个namenode处于active状态，另一个处于standby状态，处于active状态的namenode提供服务，而

处于standby状态的namenode仅仅作为一个附属，同时需要维持足够的状态，在需要的时候提供快速容错。

	为了保持standby节点和active节点同步，两个节点和一组 “JournalNodes” (JNs)守护进程通信，当active节点修改了命名空间，修改记录会以日志的方式写入到JNs中，

standby节点可以读取JNs中的编辑日志，同时，standby节点会持续的监控JNs中编辑日志的变化，当standby节点读到编辑日志，它就会应用到自己的命名空间中。当错误

发生时，standby节点会在变成active节点之前确保已经从JNs中读取了所有的编辑日志，这就确保了命名空间和错误发生之前的状态同步。

为了提供快速容错，standby节点有必要拥有最新的信息来监控集群中块的位置，为了达到这一点，DataNodes配置了两个Namenode的位置信息，并同时向两个namenode发

送块信息和心跳。

	很重要的一点是，在HA集群中，同一时刻只有一个namenode处于active状态，否则会出现错误的结果，为了保证正确性以及避免“脑裂”的出现，JNs在同一时刻只允许一

个namenode写入，在错误发生时，即将变成active状态的namenode会向JNs中写入数据，有效避免了另一个namenode继续向JNs中写入数据。

3.**IDs**
```
nameservice ID：HA集群ID
NameNode ID：HA集群中namenode的ID
```
4.**配置细节**
```
	[hdfs-site.xml]
	1、dfs.nameservices - 新的nameservice的逻辑名称
		<property>
		  <name>dfs.nameservices</name>
		  <value>mycluster</value>
		</property>
	2、dfs.ha.namenodes.[nameservice ID] - nameservice中每一个namenode的唯一的标识
		<property>
		  <name>dfs.ha.namenodes.mycluster</name>
		  <value>nn1,nn2</value>
		</property>
		注：目前，每一个nameservice中最多只能配置两个namenode
	3、dfs.namenode.rpc-address.[nameservice ID].[name node ID] - 每一个namenode的完整RPC地址
		<property>
		  <name>dfs.namenode.rpc-address.mycluster.nn1</name>
		  <value>s01:8020</value>
		</property>
		<property>
		  <name>dfs.namenode.rpc-address.mycluster.nn2</name>
		  <value>s06:8020</value>
		</property>
	4、dfs.namenode.http-address.[nameservice ID].[name node ID] - 每一个namenode的HTTP地址
		<property>
		  <name>dfs.namenode.http-address.mycluster.nn1</name>
		  <value>s01:50070</value>
		</property>
		<property>
		  <name>dfs.namenode.http-address.mycluster.nn2</name>
		  <value>s06:50070</value>
		</property>
	5、dfs.namenode.shared.edits.dir - 一组JNs的URI
		<property>
		  <name>dfs.namenode.shared.edits.dir</name>
		  <value>qjournal://s01:8485;s02:8485;s06:8485/mycluster</value>
		</property>
	6、dfs.client.failover.proxy.provider.[nameservice ID] - HDFS客户端用来连接active namenode的java类
		<property>
		  <name>dfs.client.failover.proxy.provider.mycluster</name>
		  <value>org.apache.hadoop.hdfs.server.namenode.ha.ConfiguredFailoverProxyProvider</value>
		</property>
	7、[可选] dfs.ha.fencing.methods - 容错发生时防护active namenode的一系列脚本或者是java类
	8、dfs.journalnode.edits.dir - JournalNode 守护进程本地状态的存储路径
		<property>
		  <name>dfs.journalnode.edits.dir</name>
		  <value>/home/xw/hadoop/hadoop-${user.name}/journal</value>
		</property>
	9、[core-site.xml]
		fs.defaultFS - 默认的文件系统
		<property>
		  <name>fs.defaultFS</name>
		  <value>hdfs://mycluster</value>
		</property>
```
4.**发布细节**

配置完成后，需要在相关节点上依次启动JournalNode守护进程，指令如下：

	hadoop-daemon.sh start journalnode

一旦JournalNodes守护进程启动，最初必须同步HA的namenode在磁盘上的元数据

	1. 如果建立一个新的HDFS集群，首先在其中一个namenode上运行格式化命令

		hdfs namenode -format

	2. 如果已经格式化了，或是正在将一个非HA集群转换为一个HA集群，应该在未格式化的namenode节点上运行“hdfs namenode -bootstrapStandby”命令来将namenode的

	   元数据内容拷贝到另一个未格式化的namenode上
		
	3. 如果正在将一个非HA集群转换为一个HA集群，应该运行命令“hdfs namenode -initializeSharedEdits”，这个命令将会用namenode的本地日志文件初始JournalNodes

5.**管理员命令**
	
	以上配置的是手动HA，需要使用管理员命令来将standby节点转换为active节点
	
	[可以通过ZooKeeper来转换为自动HA集群]
	```
	Usage: haadmin
		[-transitionToActive <serviceId>]			// 转换为active节点
		[-transitionToStandby <serviceId>]			// 转换为standby节点
	```