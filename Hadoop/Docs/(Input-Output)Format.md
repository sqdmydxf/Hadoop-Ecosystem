## InputFormat & OutputFormat分析

**说在前面的话**

	最近在自学Hadoop，通过看视频和自己写程序编译查看源代码，查看Hadoop中InputFormat & OutputFormat的作用。

	IDE：eclipse

	Hadoop集群环境：完全分布式模式
***
1.**``org.apache.hadoop.mapreduce.InputFormat``**
```
1. InputFormat是对一个Map-Reduce作业的指定输入的描述
2. InputFormat对于一个Map-Reduce框架的作用 :
	1) 确认一个作业的指定输入
	2) 把输入的文件切分成逻辑上的InputSplit，每一个InputSplit被分配给一个Mapper，即InputSplit和MapTask一一对应
		[逻辑上是指将输入的文件以开始位置和偏移量做一个逻辑上的切分，而不是真的把文件进行切割]
	3) 提供RecordReader的实现，其为Mapper的处理从逻辑上的InputSplit中收集输入记录[该输入记录即是key-value的形式]
		[RecordReader是针对每一个InputSplit的，在InputSplit使用之前会调用 RecordReader.initialize()方法]
3. 基于文件的InputFormat，典型的是 FileInputFormat，的默认行为是将输入的文件按照文件的字节总大小进行切片；
	但是输入切片大小的上限是该输入文件在该文件系统上的块大小，
	而切片大小的下限可以由 mapreduce.input.fileinputformat.split.minsize进行设置
```
2.**``org.apache.hadoop.mapreduce.RecordReader``**
```
1. 将输入的InputSplit打散，变成key-value对，这些key-value键值对就是Mapper的输入
2. 函数说明：
	initialize()		// 只调用一次，在InputSplit被使用之前调用
	nextKeyValue()		// 获取下一个key-value键值对
	getCurrentKey()		// 获取当前key
	getCurrentValue()	// 获取当前value
```
3.**``org.apache.hadoop.mapreduce.OutputFormat``**
```
1. OutputFormat是对一个Map-Reduce作业的指定输出的描述
2. OutputFormat对于一个Map-Reduce框架的作用 :
	1) 确认一个作业的指定输出，例如检查输出目录是否存在，对应了checkOutputSpecs(...)函数
	2) 提供一个 RecordWriter的实现，用于将作业的输出文件写入到文件系统中，对应了getRecordWriter(...)函数
```
4.**``org.apache.hadoop.mapreduce.RecordWriter``**
```
1. 将输出的键值对写入到文件系统中
2. 函数说明：
	write(K key, V value)		// 写入到文件系统的也是key-value键值对
```
###几种常用的InputFormat & OutputFormat
1.**``org.apache.hadoop.mapreduce.lib.input.TextInputFormat``**
```
1. 用于处理普通的文本文件，将文件打散成行
2. RecordReader的实现是LineRecordReader，即收集的key-value键值对是行偏移量和行，这些key-value键值对就是Mapper的输入
   即说明，对于通过的文本文件的Mapper处理，是按照行处理的，因此，即使splitsize小于一行数据的大小，也不会对行进行切分
   实践证明如下：
   blocksize 需是512的倍数
1. splitsize < blocksize < linesize
	只有一行数据
	spiltsizemin = 60
	spiltsizemin = 100
	blocksize = 512
	linesize = 537
	处理结果：
		number of splits:6
		map
		s05-Mapper@2c1b9e4b--1877982512-set=1
		s05-Mapper@2c1b9e4b--2083004745-cle=1
		s05-Mapper@2c1b9e4b-107768775-cle=1
		s05-Mapper@2c1b9e4b-1938151989-set=1

		s05-Mapper@44c79f32--1183436527-set=1
		s05-Mapper@44c79f32-1725456363-cle=1
		s05-Mapper@44c79f32-1890371796-map-00..99-536x=1

		s05-Mapper@649725e3--1463924917-set=1
		s05-Mapper@649725e3-1332864012-cle=1

		s05-Mapper@757d6814--361298993-set=1
		s05-Mapper@757d6814-320947766-cle=1

		s05-Mapper@c7a975a--1526891499-set=1
		s05-Mapper@c7a975a-1803006044-cle=1
		reduce
		s04-Reducer@28d6290--1478275326-red-1901=1
		s04-Reducer@28d6290--1830677549-set=1
		s04-Reducer@28d6290--956562205-cle=1
	总结：
		虽然block和split的大小都比一行数据要小，切片数量是按照算法得出，为6，map处理的时候，有6个mapTask，
		但是只有一个mapTask处理了一行所有的数据

2. splitsize < blocksize < linesize
	有多行数据
	spiltsizemin = 200
	spiltsizemin = 250
	blocksize = 512
	linesize = 537
	line = 4
	处理结果：
		number of splits:9
	map
		s03-Mapper@743cb8e0--1949015896-set=1
		s03-Mapper@743cb8e0-1890373262-map-00..99-536x=1
		s03-Mapper@743cb8e0-37990731-cle=1

		s03-Mapper@c7a975a--1788885610-map-00..99-536x=1
		s03-Mapper@c7a975a-1550437242-set=1
		s03-Mapper@c7a975a-1854359630-set=1
		s03-Mapper@c7a975a-2046099884-cle=1
		s03-Mapper@c7a975a-209336627-cle=1

		s04-Mapper@235f4c10--1252710452-set=1
		s04-Mapper@235f4c10--1645186417-cle=1

		s04-Mapper@2a1edad4--1022522447-map-00..99-536x=1
		s04-Mapper@2a1edad4--1157356719-cle=1
		s04-Mapper@2a1edad4--1810534542-set=1

		s04-Mapper@2c1b9e4b--270718615-set=1
		s04-Mapper@2c1b9e4b-1562159055-cle=1

		s04-Mapper@7fcbe147--1757583916-cle=1
		s04-Mapper@7fcbe147-1927818137-map-00..99-536x=1
		s04-Mapper@7fcbe147-589064948-set=1

		s05-Mapper@235f4c10--180828109-set=1
		s05-Mapper@235f4c10-847305323-cle=1

		s05-Mapper@743cb8e0--1524971834-cle=1
		s05-Mapper@743cb8e0--213739930-set=1
	reduce
		s05-Reducer@50eca7c6--2016491009-cle=1
		s05-Reducer@50eca7c6--530075780-set=1
		s05-Reducer@50eca7c6-979337267-red-1901=1
	总结：
		切片数量决定mapTask的个数,如果mapTask的数量多于行数，则剩余的mapTask只运行setup和cleanup方法，
		而不执行map方法

3. linesize < splitsize < 2 * linesize
	有多行数据
	spiltsizemin = 200
	spiltsizemin = 500
	blocksize = 512
	linesize = 403
	line = 8
	处理结果：
		number of splits:7
	map
		s03-Mapper@235f4c10-cle=2
		s03-Mapper@235f4c10-map-00..99-402x=2
		s03-Mapper@235f4c10-set=2

		s03-Mapper@2c1b9e4b-cle=1
		s03-Mapper@2c1b9e4b-map-00..01-408x=1
		s03-Mapper@2c1b9e4b-set=1

		s03-Mapper@44c79f32-cle=1
		s03-Mapper@44c79f32-map-00..99-402x=2
		s03-Mapper@44c79f32-set=1

		s03-Mapper@649725e3-cle=1
		s03-Mapper@649725e3-map-00..99-402x=1
		s03-Mapper@649725e3-set=1

		s03-Mapper@743cb8e0-cle=1
		s03-Mapper@743cb8e0-map-00..99-402x=1
		s03-Mapper@743cb8e0-set=1

		s03-Mapper@c7a975a-cle=1
		s03-Mapper@c7a975a-map-00..99-402x=1
		s03-Mapper@c7a975a-set=1
	reduce
		s03-Reducer@376a312c--354436423-set=1
		s03-Reducer@376a312c--770418631-cle=1
		s03-Reducer@376a312c-699718477-red-1901=1
	总结：
		split个数为7，setup和cleanup被调用7次，但是map方法是针对每一行的，即每一行都会调用一次map方法。

4. 对于不可切分的文件，例如gz压缩文件，不可切分，但是hdfs层面上存储的时候，还是会分为多个block，block的块数由blocksize决定，
   但是由于不可切分，所以，split的个数为1，由一个mapTask处理，调用一次setup和cleanup，每行调用一次map函数，
   即一个mapTask调用多次map函数。
   处理数据：1902.gz
   处理结果：
		number of splits:1
		map
		s04-Mapper@7fd4acee-cle=1
		s04-Mapper@7fd4acee-map-00..01-105x=7
		s04-Mapper@7fd4acee-map-00..01-140x=47
		s04-Mapper@7fd4acee-map-00..21-140x=494
		s04-Mapper@7fd4acee-map-00..31-105x=2
		s04-Mapper@7fd4acee-map-00..41-105x=5
		s04-Mapper@7fd4acee-map-00..41-140x=11
		s04-Mapper@7fd4acee-map-00..61-105x=5
		s04-Mapper@7fd4acee-map-00..61-140x=1
		s04-Mapper@7fd4acee-map-00..71-105x=2
		s04-Mapper@7fd4acee-map-00..81-105x=2
		s04-Mapper@7fd4acee-map-00..81-140x=19
		s04-Mapper@7fd4acee-map-00..91-105x=3
		s04-Mapper@7fd4acee-map-00..91-140x=1
		s04-Mapper@7fd4acee-map-00..99-134x=5965
		s04-Mapper@7fd4acee-map-00..M1-153x=1
		s04-Mapper@7fd4acee-set=1
		reduce
		s04-Reducer@28d6290-cle=1
		s04-Reducer@28d6290-red-1902=1
		s04-Reducer@28d6290-set=1

综上：split的个数决定了mapTask的个数，一个mapTask为一个Mapper的完整生命周期，即setup和cleanup被调用的次数为mapTask的个数，
	  但是map函数是针对每一行的，即每一行都会调用一次map函数，所以，map函数被调用的次数由输入的key/value的个数，
	  即输入的行数决定，即一个mapTask可以调用多次map函数。
```
2.**``SequenceFile``**
```
1. SequenceFile是一个二进制文件，相当于是一个key-value的集合
2. SequenceFile不是一个文本文件，无法用查看文本文件的指令[cat...]来进行查看，查看该类文件的指令为：
	hdfs dfs -text SequenceFile 
3. SequenceFile输出的默认压缩格式是记录压缩，即CompressionType.RECORD[该压缩方式只压缩record中的value值]
4. SequenceFile中会有随机生成的sync标记
```
3.**``WholeFile``**
```
1. WholeFile是一个不可切分的文件，整个文件作为一个整体进行Mapper操作，key为NullWritable，value为BytesWritable，
   即将整个文件作为一个value存放到字节数组中
2. 需要自定义WholeFileInputFormat，其继承FileInputFormat<NullWritable, BytesWritable>，用于对WholeFile进行描述，
   并设置其为不可切分
3. 需要自定义WholeRecordReader，用于WholeFile的读取工作，只要是nextKeyValue()函数，获取下一对key-value键值对，
   key为NullWritable，value即为整个文件，将其存放到BytesWritable中，即流的读取。
```
4.**``DB``**
```
--> DBWritable
1. 自定义读写数据库的对象应继承DBWritable
   [该对象相当于表中的一条记录，也可以说是一个数据模型，是数据库和HDFS的中间数据，是Mapper处理的value值]
2. 函数说明：
   readFields(ResultSet)					// 将ResultSet中的记录读到自定义对象中的属性中
   write(PreparedStatement statement)		// 将自定义的对象的字段设置到PreparedStatement中，以便更新数据库

--> DBRecordReader
DBRecordReader.nextKeyValue()分析：
1) 将自定义的DBWritable作为Mapper的value值
2) 如果第一次调用nextKeyValue()函数，则读取数据库中的数据到results中
	if (null == this.results) {
        // First time into this method, run the query.
        this.results = executeQuery(getSelectQuery());
    }
3) 判断结果集中是否有下一条数据，同时游标下移
	if (!results.next())
        return false;
4) 读取一条记录到自定义对象中
	value.readFields(results)

--> DBOutputFormat
1. 将reduce的输出写入到数据库中
2. DBOutputFormat接受一个key-value键值对，其中key是自定义的DBWritable类型，通过DBRecordWriter向数据库写入数据
   注：它只将key写入到数据库中
```