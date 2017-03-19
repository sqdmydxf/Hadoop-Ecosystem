##完全分布式集群下Hadoop客户端提交作业分析

**说在前面的话**

	最近在自学Hadoop，通过看视频和自己写程序编译查看源代码，查看Hadoop中完全分布式下MapReduce中作业提交过程。

	以下流程是根据Hadoop权威指南上的实现获取ncdc每年最高气温的代码进行调试，在Hadoop完全分布式模式中查看作业提交过程。

	操作系统：Ubuntu16.04

	IDE：eclipse

	Hadoop集群环境：完全分布式模式
***
1.**mapreduce中job提交过程**
```
--> org.apache.hadoop.mapreduce.Job.waitForCompletion(true)								// 等待完成
	--> job.submit()[*]																	// 提交
	--> ensureState(JobState.DEFINE)
	--> setUseNewAPI()
	--> connect() 
		-->new Cluster(getConfiguration())
	--> org.apache.hadoop.mapreduce.JobSubmitter.submitJobInternal()[*]					// 内部提交
		--> checkSpecs(job)																// 需要输入目录不能存在
		--> 设置jobStagingArea,并创建目录												// [/tmp/hadoop-yarn/staging/xw/.staging]
		--> ip
		--> YARNRunner.getNewJobID()													// application_1488793063088(集群的时间戳)_0002(applicationID)
			--> org.apache.hadoop.mapred.ResourceMgrDelegate.getNewJobID()		
				--> ApplicationSubmissionContextPBImpl[application]						// 获取ApplicationSubmissionContext
				--> application.getApplicationId()										// 获取applicationId
				--> TypeConverter.fromYarn(applicationId)								// 将appID转为jobID，appID和jobID之间存在1-1映射
					--> fromClusterTimeStamp(appID.getClusterTimestamp())				// 获取identifier[集群的时间戳]
					--> org.apache.hadoop.mapred.JobID(identifier,appID.getId())		// 将appID映射为jobID
		--> 设置submitJobDir													
			// /tmp/hadoop-yarn/staging/xw/.staging/job_1488793063088_0002(JobID)[jobStagingArea/jobID]
		--> copyAndConfigureFiles(job, submitJobDir)									// 拷贝和配置文件到文件系统中(上传文件到文件系统中)
			--> JobResourceUploader.uploadFiles()								
				// 上传文件到[/tmp/hadoop-yarn/staging/xw/.staging目录下，即创建job_1488793063088_0002(JobID)]目录
				--> FileSystem.mkdirs(jtFs, submitJobDir, mapredSysPerms)				// 创建job_1488793063088_0002(JobID)]目录
				--> new Path(jobJar)													// 获取自定义的jar文件			
					[/home/xw/Workspace/Hadoop/projects/HadoopMavenDemo/target/HadoopMavenDemo-1.0.0.jar]
				-->	copyJar(...)														// 拷贝jar文件到文件系统中			
			--> job.getWorkingDirectory()												// 获取工作目录，[hdfs://s01:8020/user/xw]
		--> submitJobFile																// 设置作业的自定义配置文件
			[/tmp/hadoop-yarn/staging/xw/.staging/job_1488793063088_0002/job.xml]
		--> writeSplits(job, submitJobDir)												// 写入切片文件[切片文件的个数即是mapTask的个数]，并进行排序
			--> writeNewSplits(job, jobSubmitDir)										// 调用新的切片方法
			--> List splits = input.getSplits(job)										// 对输入文件进行切片，并其后对其map
				--> long minSize = Math.max(1, getMinSplitSize(job))				
					// 在1和自定义的"mapreduce.input.fileinputformat.split.minsize"之间选一个大值作为切片的最小值
				--> long maxSize = getMaxSplitSize(job)								
					// 将自定义的"mapreduce.input.fileinputformat.split.maxsize"作为切片的最大值
				--> long splitSize = computeSplitSize(blockSize, minSize, maxSize)
					--> Math.max(minSize, Math.min(maxSize, blockSize))					// 在minSize,maxSize和blockSize之间选中间值作为切片文件的大小
				--> splits.add(...)														// 将切片存放到列表中
			--> splits.toArray(new InputSplit[splits.size()])							// 将列表转为数组
			--> Arrays.sort(array, new JobSubmitter.SplitComparator(null))				// 按切片文件大小进行排序	
			--> JobSplitWriter.createSplitFiles(...)									// 将切片文件写入临时文件夹submitJobDir
			--> return maps																// 返回所需的mapTask数
		--> writeConf(conf, submitJobFile1)												// 将job.xml(自定义+默认配置文件)文件写入到submitJobDir中
		--> this.submitClient.submitJob(...)											// 配置完作业，通过YARNRunner提交作业
			--> resMgrDelegate.submitApplication(appContext)							// 通过资源管理器的代理提交应用
				--> YarnClientImpl.submitApplication(appContext)						// 通过YarnClientImpl提交应用
					--> Records.newRecord(SubmitApplicationRequest.class)				// 通过反射获取SubmitApplicationRequestPBImpl，是对请求的封装
					--> ApplicationClientProtocolPBClientImpl[$proxy].submitApplication(request)	// 通过下一层代理提交请求，该方法通过反射调用
						[通过反射调用ApplicationClientProtocolPBClientImpl[$proxy].submitApplication(request)
						--> invokeMethod(method[submitApplication], args)				// 反射调用代理的submitApplication方法
							--> method.invoke(currentProxy.proxy, args)					// 调用proxy的submitApplication方法
								--> MethodAccessor.invoke(obj, args)					// 反射调用
							]
					--> ApplicationClientProtocolPBClientImpl.submitApplication(request)[即反射结束调用代理的提交方法]
						--> getProto()													// 获取SubmitApplicationRequestPBImpl
						--> proxy.submitApplication(null,requestProto)
							--> ProtobufRpcEngine.invoke()								// Rpc引擎调用方法
							--> constructRpcRequestHeader(method)						// 构造RequestHeaderProto[rpcRequestHeader]
							--> (Message) args[1]										// 获取theRequest[Message]
							--> new RpcRequestWrapper(RequestHeaderProto, theRequest)	// 通过请求头和请求封装RpcRequestWrapper
							--> (RpcResponseWrapper) client.call(...)					// org.apache.hadoop.ipc.Client发出请求，返回RpcResponseWrapper
								--> return call(rpcKind, RpcRequestWrapper)				// 返回call，call是对请求类型和RpcRequestWrapper的封装
									--> createCall(rpcKind, rpcRequest)					// 创建call对象
									--> getConnection()									// 创建org.apache.hadoop.ipc.Client.Connection对象[即连接到resourcemanager]
									-->connection.sendRpcRequest(call)					// 通过connection对象发送请求，这是其他线程调用，而不是Connection线程
										--> new DataOutputBuffer()						// 获取数据输出缓冲区，该类继承DataOutputStream
										--> RpcRequestHeaderProto header				// 获取RpcRequestHeaderProto对象
										--> header.writeDelimitedTo(d)					// 写入header
										--> call.rpcRequest.write(d)					// 写入call对象
										--> nio通信
```
2.**RequestHeaderProto[请求头信息]**
```
methodName: "submitApplication"
declaringClassProtocolName: "org.apache.hadoop.yarn.api.ApplicationClientProtocolPB"
clientProtocolVersion: 1
```
3.**theRequest[请求内容]**
```
application_submission_context {
  application_id {
    id: 1
    cluster_timestamp: 1488813322829
  }
  application_name: "Max temperature"
  queue: "default"
  am_container_spec {
    localResources {
      key: "jobSubmitDir/job.splitmetainfo"
      value {
        resource {
          scheme: "hdfs"
          host: "s01"
          port: 8020
          file: "/tmp/hadoop-yarn/staging/xw/.staging/job_1488813322829_0001/job.splitmetainfo"
        }
        size: 65
        timestamp: 1488813819442
        type: FILE
        visibility: APPLICATION
      }
    }
    localResources {
      key: "job.jar"
      value {
        resource {
          scheme: "hdfs"
          host: "s01"
          port: 8020
          file: "/tmp/hadoop-yarn/staging/xw/.staging/job_1488813322829_0001/job.jar"
        }
        size: 8612
        timestamp: 1488813818750
        type: PATTERN
        visibility: APPLICATION
        pattern: "(?:classes/|lib/).*"
      }
    }
    localResources {
      key: "jobSubmitDir/job.split"
      value {
        resource {
          scheme: "hdfs"
          host: "s01"
          port: 8020
          file: "/tmp/hadoop-yarn/staging/xw/.staging/job_1488813322829_0001/job.split"
        }
        size: 316
        timestamp: 1488813819305
        type: FILE
        visibility: APPLICATION
      }
    }
    localResources {
      key: "job.xml"
      value {
        resource {
          scheme: "hdfs"
          host: "s01"
          port: 8020
          file: "/tmp/hadoop-yarn/staging/xw/.staging/job_1488813322829_0001/job.xml"
        }
        size: 98192
        timestamp: 1488813819834
        type: FILE
        visibility: APPLICATION
      }
    }
    tokens: "HDTS\000\000\001\025MapReduceShuffleToken\b\373PpW\0024\002\244"
    environment {
      key: "HADOOP_CLASSPATH"
      value: "$PWD:job.jar/job.jar:job.jar/classes/:job.jar/lib/*:$PWD/*:null"
    }
    environment {
      key: "SHELL"
      value: "/bin/bash"
    }
    environment {
      key: "CLASSPATH"
      value: 
	  "$PWD:$HADOOP_CONF_DIR:$HADOOP_COMMON_HOME/share/hadoop/common/*:$HADOOP_COMMON_HOME/share/hadoop/common/lib/*:$HADOOP_HDFS_HOME/share/hadoop/hdfs/*:
	  $HADOOP_HDFS_HOME/share/hadoop/hdfs/lib/*:$HADOOP_YARN_HOME/share/hadoop/yarn/*:$HADOOP_YARN_HOME/share/hadoop/yarn/lib/*:$HADOOP_MAPRED_HOME/share/h
	  adoop/mapreduce/*:$HADOOP_MAPRED_HOME/share/hadoop/mapreduce/lib/*:job.jar/job.jar:job.jar/classes/:job.jar/lib/*:$PWD/*"
    }
    environment {
      key: "LD_LIBRARY_PATH"
      value: "$PWD:{{HADOOP_COMMON_HOME}}/lib/native"
    }
    command: "$JAVA_HOME/bin/java -Djava.io.tmpdir=$PWD/tmp -Dlog4j.configuration=container-log4j.properties -Dyarn.app.container.log.dir=<LOG_DIR> 
	-Dyarn.app.container.log.filesize=0 -Dhadoop.root.logger=INFO,CLA -Dhadoop.root.logfile=syslog  -Xmx1024m 
	org.apache.hadoop.mapreduce.v2.app.MRAppMaster 1><LOG_DIR>/stdout 2><LOG_DIR>/stderr "
    application_ACLs {
      accessType: APPACCESS_VIEW_APP
      acl: " "
    }
    application_ACLs {
      accessType: APPACCESS_MODIFY_APP
      acl: " "
    }
  }
  cancel_tokens_when_complete: true
  maxAppAttempts: 2
  resource {
    memory: 1536
    virtual_cores: 1
  }
  applicationType: "MAPREDUCE"
}
```
4.**RpcRequestWrapper[对请求头和内容的封装]**
```
RequestHeaderProto
theRequest
```
5.**call[对请求类型和请求的封装]**
```
rpcKind[请求类型]
RpcRequestWrapper
```
6.**RpcRequestHeaderProto[RPC请求头]**
```
rpcKind: RPC_PROTOCOL_BUFFER
rpcOp: RPC_FINAL_PACKET
callId: 38
clientId: "V\024\3037\244+L?\216\206\241\251\005\213Q\003"
retryCount: 0
```
7.**Format of a call on the wire[请求序列化格式]**
```
 0) Length of rest below (1 + 2)
 1) RpcRequestHeader  - is serialized Delimited hence contains length
 2) RpcRequest
```