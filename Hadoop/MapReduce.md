##Map和Reduce过程分析
**说在前面的话**

	最近在自学Hadoop，通过看视频和自己写程序编译查看源代码，查看Hadoop中MapReduce的运行流程。

	以下流程是根据Hadoop权威指南上的实现获取ncdc每年最高气温的代码进行调试，在Hadoop本地模式中查看其运行流程。

	操作系统：Ubuntu16.04

	IDE：eclipse

	Hadoop集群环境：本地模式

	一开始是通过intellij idea中的maven项目进行调试，但是发现其中的注释比较少，而且调试的时候出现一些问题，因此换到了eclipse中进行调试，最后跑通整个过程。
***
1.**MRJob 配置的常量所在的类**
```
org.apache.hadoop.mapreduce.MRJobConfig
```
2.**状态说明**
```
JobState {DEFINE, RUNNING}												// 作业状态，两种
TaskStatusFilter { NONE, KILLED, FAILED, SUCCEEDED, ALL }				// 任务状态，五种
```
3.**配置文件加载**
```
org.apache.hadoop.conf.Configuration
	addDefaultResource("core-default.xml");  							// 加载core-default.xml
    addDefaultResource("core-site.xml");								// 加载core-site.xml

org.apache.hadoop.mapred.JobConf extends Configuration
	--> org.apache.hadoop.mapreduce.util.ConfigUtil.loadResources()
			Configuration.addDefaultResource("mapred-default.xml");		// 加载默认mapred-default.xml
		    Configuration.addDefaultResource("mapred-site.xml");		// 加载默认mapred-site.xml
		    Configuration.addDefaultResource("yarn-default.xml");		// 加载默认yarn-default.xml
		    Configuration.addDefaultResource("yarn-site.xml");			// 加载默认yarn-site.xml
```
4.**配置自定义设置**

5.**通过waitForCompletion()分析MR**
```
--> org.apache.hadoop.mapreduce.Job.waitForCompletion(true)							// 等待完成
	--> job.submit()[*]																// 提交
		--> ensureState(JobState.DEFINE)
		--> setUseNewAPI()
		--> connect() 
			-->new Cluster(getConfiguration())
		--> org.apache.hadoop.mapreduce.JobSubmitter.submitJobInternal()[*]			// 内部提交
			--> checkSpecs(job)														// 需要输入目录不能存在
			--> 设置jobStagingArea,并创建目录											// [file:/tmp/hadoop-xw/mapred/staging/xw902757940/.staging]
			--> ip
			--> JobID																// job_local902757940(randID:随机数)_0001(jobID)
			--> 设置submitJobDir													
				// file:/tmp/hadoop-xw/mapred/staging/xw902757940/.staging/job_local902757940_0001(JobID)[jobStagingArea/jobID]
			--> user_name
			--> copyAndConfigureFiles(job, submitJobDir)							// 拷贝和配置文件到文件系统中(上传文件到文件系统中)
				--> JobResourceUploader.uploadFiles()								// 上传文件到[file:/tmp/hadoop-xw/mapred/staging/xw902757940/.staging目录
				下，即创建job_local902757940_0001(JobID)]目录
				--> job.getWorkingDirectory()										// 获取工作目录，即项目目录
				[file:/D:/MyDocument/Y1Document/code/java/eclipse/HadoopDemo]
			--> submitJobFile														// 设置作业的自定义配置文件
				[file:/tmp/hadoop-xw/mapred/staging/xw268204430/.staging/job_local268204430_0001/job.xml]
			--> writeSplits(job, submitJobDir)										// 获取需要map的输入文件，并进行排序，切割，写入
				--> List splits = input.getSplits(job)								// 获取需要map的输入文件
				--> Arrays.sort(array, new JobSubmitter.SplitComparator(null))		// 按文件大小进行排序	
				--> JobSplitWriter.createSplitFiles(...)							// 文件切割并写入临时文件夹submitJobDir
					--> writeNewSplits(conf, splits, out)							// 写入切割文件
					[job.split:SPL.../org.apache.hadoop.mapreduce.lib.input.FileSplit2file:map切割文件...其他文件类似]
					--> writeJobSplitMetaInfo(...)									// 写入切割文件元文件[job.splitmetainfo:META-SPL...]
				--> return maps														// 返回所需的mapTask数
			--> writeConf(conf, submitJobFile1)										// 将job.xml(自定义+默认配置文件)文件写入到submitJobDir中
			--> this.submitClient.submitJob(...)									// 配置完作业，通过LocalJobRunner提交作业
				--> new LocalJobRunner.Job()										// 创建LocalJobRunner.Job
					--> localFs.makeQualified(new Path(new Path(conf.getLocalPath("localRunner/"), user), jobid.toString()))
						// 创建localRunner的临时目录[/tmp/hadoop-xw/mapred/local]
					--> localFs.create(this.localJobFile)							
					// 创建本地运行器的作业配置文件[/tmp/hadoop-xw/mapred/local/localRunner/xw/job_local1600947809_0001/job_local1600947809_0001.xml(JobID)]
					--> this.start()[*]												// 开启Job线程
			--> return new LocalJobRunner.Job(...).status							// 返回配置完的作业的状态
		--> job.state = Job.JobState.RUNNING										// 开始启动作业

--> LocalJobRunner.Job线程
	--> createOutputCommitter()														// 文件输出提交器
		--> new TaskID(jobId, TaskType.MAP, 0)										// 创建一个mapTaskID
		--> outputFormat.getOutputCommitter(taskContext)							// 获取文件输出器
			--> getOutputPath(context)												// 获取结果输出路径[/home/xw/Documents/Java/Hadoop/Docs/out]
			--> return new FileOutputCommitter(...)									// 返回指向输出路径的文件输出器
		--> return committer														// 返回通过反射得到的文件输出器对象
	--> TaskSplitMetaInfo															// 获取文件分割元信息
	--> job.getNumReduceTasks()														// 获取reduceTask数
	--> outputCommitter.setupJob(jContext)											// 创建结果输出目录
		--> getJobAttemptPath(context)												// 获取尝试路径[/home/xw/Documents/Java/Hadoop/Docs/out/_temporary/0]
		--> fs.mkdirs()																// 创建尝试目录
	--> initCounters(mapRunnables.size(), ioe)										// 初始化mapTask和reduceTask数
	--> // 开始执行MapTask
	--> runTasks(mapRunnables, mapService, "map")									// 运行runTask
		--> service.submit(r[LocalJobRunner.Job.MapTaskRunnable])					// 使用ThreadPoolExecutor循环提交mapTask到线程池
			--> newTaskFor(task, null)												// 使用MapTaskRunnable创建FutureTask[可以开始和结束计算，查询计算结果]
			--> ThreadPoolExecutor.execute(ftask)									// 执行MapTask
				--> addWorker(command, true)										// 将mapTask添加到工作列表中
				--> t = new Worker(firstTask).thread								// 获取一个Worker线程
				--> t.start()[*]													// 开启Worker线程
	--> 获取LocalJobRunner$Job$ReduceTaskRunnable对象
	--> runTasks(reduceRunnables, reduceService, "reduce")							// 运行reduceTask
		--> service.submit(r[LocalJobRunner.Job.ReduceTaskRunnable])				// 使用ThreadPoolExecutor循环提交ReduceTaskRunnable到线程池
			--> newTaskFor(task, null)												// 使用ReduceTaskRunnable创建FutureTask[可以开始和结束计算，查询计算结果]
			--> ThreadPoolExecutor.execute(ftask)									// 执行ReduceTaskRunnable
				--> addWorker(command, true)										// 将ReduceTask添加到工作列表中
				--> t = new Worker(firstTask).thread								// 获取一个Worker线程
				--> t.start()[*]													// 开启Worker线程

--> ThreadPoolExecutor.Worker线程[LocalJobRunner MapTask/ReduceTask Executor]
--> ThreadPoolExecutor.Worker线程(LocalJobRunner MapTask Executor)(分析map过程)
	--> runWorker(this)																// 启动mapTask
		--> worker.firstTask = null													// 清空worker的第一个mapTask
		-->	w.unlock()																// 解锁worker,用于执行下一个mapTask
		--> task.run()																// 调用LocalJobRunner.Job.MapTask的run方法，执行mapTask
			--> 生成TaskAttemptID(mapID)											// 生成mapTaskID[JobID_m_taskID]
			--> childMapredLocalDir													// 获取mapTask的本地目录
				// /tmp/hadoop-xw/mapred/local/localRunner//xw/jobcache/job_local677056674_0001[JobID]/attempt_local677056674_0001_m_000000_0[mapID]
			--> org.apache.hadoop.mapred.MapTask.run(localConf, Job.this)[*]		// 运行mapTask
				--> if (conf.getNumReduceTasks() == 0)
					--> mapPhase = getProgress().addPhase("map", 1.0f)				// 若没有reduceTask，则全为mapTask,不需要排序
				--> else															// 如果有reduceTask
					--> mapPhase = getProgress().addPhase("map", 0.667f)
					-->	sortPhase  = getProgress().addPhase("sort", 0.333f)		    // 默认mapPhase占67%,sortPhase占33%
				--> startReporter(umbilical)										// 启动报告器
				--> initialize(job, getJobID(), reporter, useNewApi)				// 初始化mapTask,生成报告器
				--> runNewMapper(job, splitMetaInfo, umbilical, reporter)			// 运行新的Mapper
					--> org.apache.hadoop.mapreduce.Mapper mapper = ReflectionUtils.newInstance(taskContext.getMapperClass(), job)
						// 使用反射机制生成自定义的mapper类进行map处理
					--> split = getSplitDetails(new Path(splitIndex.getSplitLocation()),splitIndex.getStartOffset())
						// 获取要处理的切割文件，这里是 file:/home/xw/Documents/Java/Hadoop/Docs/ncdc/1902:0+888978
					--> output = new NewOutputCollector(taskContext, job, umbilical, reporter)
						// 生成输出集合器 org.apache.hadoop.mapred.MapTask$NewOutputCollector@35b3e9c
					--> input.initialize(split, mapperContext)						// 导入切割文件
					--> mapper.run(mapperContext)									// 运行mapper
						--> setup -> map -> cleanup									// local模式下,setup和cleanup为空
						--> com.xw.demo.MaxTemperatureMapper.map(...)				// 运行自定义的map方法
					--> mapPhase.complete()											// 结束map阶段
					--> setPhase(TaskStatus.Phase.SORT)								// 设置排序阶段
					--> output.close(mapperContext)									// 关闭输出，将map中间数据写入本地目录
				--> done(umbilical, reporter)										// 结束本次mapTask
						
--> ThreadPoolExecutor.Worker线程(LocalJobRunner ReduceTask Executor)(分析reduce过程)
	--> runWorker(this)																// 启动reduce
		--> worker.firstTask = null													// 清空worker的第一个reduce
		-->	w.unlock()																// 解锁worker,用于执行下一个reduce
		--> task.run()																// 调用LocalJobRunner.Job.reduceTask的run方法，执行reduceTask
			--> 生成TaskAttemptID(reduceID)											// 生成reduceTaskID[JobID_r_taskID]
			--> setupChildMapredLocalDirs											// 获取reduceTask的本地目录
				// /tmp/hadoop-xw/mapred/local/localRunner//xw/jobcache/job_local677056674_0001[JobID]/attempt_local677056674_0001_r_000000_0[reduceID]
			--> org.apache.hadoop.mapred.ReduceTask.run(localConf, Job.this)[*]		// 运行reduceTask
				--> if (isMapOrReduce())											// 判断是否是mapreduce
					--> copyPhase = getProgress().addPhase("copy");					// shuffle过程就是拷贝的过程
					-->	sortPhase  = getProgress().addPhase("sort");
					--> reducePhase = getProgress().addPhase("reduce");				// 若是，则reduce分为copy,sort,reduce三个阶段
				--> startReporter(umbilical)										// 启动报告器
				--> initialize(job, getJobID(), reporter, useNewApi)				// 初始化reduceTask
				--> codec = initCodec()												// 初始化压缩器
				--> shuffleConsumerPlugin = ReflectionUtils.newInstance(clazz, job)	// 通过反射生成shuffle
				--> shuffleConsumerPlugin.init(shuffleContext)						// 初始化shuffle
				--> shuffleConsumerPlugin.run()										// 运行shuffle，[拷贝,归并排序]
					--> new EventFetcher<K,V>(...)									// 生成提取器守护线程
					--> eventFetcher.start()										// 启动线程，开始提取map中间结果到内存
					--> fetcher.shutDown()											// 提取结束
					--> copyPhase.complete()										// 完成拷贝
					--> merger.close()												// 将map中间数据进行归并排序并写入到本地
						// jobcache/job_local1159170351_0001/attempt_local1159170351_0001_r_000000_0/output/map_1.out.merged
					--> return kvIter[org.apache.hadoop.mapred.Merger$MergeQueue]	// 返回合并排序后的队列	
				--> sortPhase.complete()											// 完成排序
				--> setPhase(TaskStatus.Phase.REDUCE)								// 进入reduce过程
				--> runNewReducer(...)												// 运行reduceTask
					--> org.apache.hadoop.mapreduce.Reducer reducer = ReflectionUtils.newInstance(taskContext.getReducerClass(), job)
						// 通过反射机制获取自定义的reducer对象[com.xw.demo.MaxTemperatureReducer]
					--> reducer.run(reducerContext)									// 运行reducer
						--> setup -> reduce -> cleanup								// local模式下,setup和cleanup为空
						--> com.xw.demo.MaxTemperatureReducer.reduce(...)			// 运行自定义的reduce方法
				--> done(umbilical, reporter)										// 结束本次reduceTask
					--> commit(umbilical, reporter, committer)						// 提交reduce结果到本地
						--> committer.commitTask(taskContext)						// 提交
							--> commitTask(context, null)
								--> Path committedTaskPath = getCommittedTaskPath(context)
									// 获取reduce结果临时目录[file:/home/xw/Documents/Java/Hadoop/Docs/out/_temporary/0/task_local1159170351_0001_r_000000]
				--> processWorkerExit(w, completedAbruptly)							// 将最终reduce结果写入到自定义的目录中
```
