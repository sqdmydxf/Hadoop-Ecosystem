## Hive

**说在前面的话**

    本文用于记录Hive的原理说明、常用命令、以及优化，以便日后查询。

***
1.**原理说明**
    1. Hive是一个数据仓库的软件，用来管理大量的数据集，对其进行数据统计分析
    
    2. Hive并不是数据库，它和数据库的应用场景不同，数据库适用于OLTP[online transaction process 在线事务处理]，
    
       而Hive适合于OLAP[online analysis process 在线分析处理]
    
    3. 知识点说明
    
        a. Hive可以用类SQL语言[HiveQL/HQL]进行操纵,但是并不是操作的真实的表
    
           Hive中的数据库和表都是HDFS上的文件夹，表中的数据实际上是put在HDFS上的文本文件
    
        b. Hive将数据库、表、字段的描述信息存放在关系型数据库中，默认是Derby，可以存放MySQL
    
        c. Hive用HQL对数据库进行操作时，将HQL语句中的信息和存放在MySQL中的描述信息进行比对，
    
           从而映射到HDFS上的文件上去，找到数据存放的文件，从而操作数据
    
        d. MySQL中存放的是数据库和表的结构，而数据则是HDFS上的文本，只需要文本的格式符合表行和列的分割符要求，
    
           将该文本手动put到HDFS上之后，用HQL查询表中数据的时候，也会将该文本中的数据查询出来，
    
           若是不符合分割要求，查询出来则会显示MULL
    
        e. Hive不支持更新，删除，也不支持事务
    
        f. set指令可以显示所有的环境变量信息
    
        g. hive命令行下，可以通过"!+Linux指令"的方式执行Linux指令，例如：!clear

2.**Hive数据类型**
```
    TINYINT[1 byte]                         ARRAY
    SMALLINT[2 bytes]                       MAP
    INT[4 bytes]                            STRUCT
    BIGINT[8 bytes]                         NAMED STRUCT
    FLOAT[4 bytes]                          UNION
    DOUBLE[8 bytes]
    DECIMAL[38 digits]
    BINARY
    BOOLEAN
    STRING
    CHAR
    VARCHAR
    DATE
    TIMESTAMP
```
3.**常用命令**
    在Hive中可以使用hdfs命令，即dfs命令


    创建数据库，MySQL记录数据库的描述信息，对应HDFS上的数据库文件夹
    ```sql
    create database <basename>;
    e.g.
        CREATE DATABASE IF NOT EXISTS myhive
        COMMENT 'hive database demo'
        LOCATION '/hdfs/directory'                  // 指定数据库在HDFS上的存放位置
        WITH DBPROPERTIES ('creator'='xw','date'='2017-04-10');
    ```
    常看所有数据库
    ```sql
    show databases;
    ```
    查看数据库结构
    ```sql
    desc database <databasename> ;
    ```
    查看数据库结构及扩展信息
    ```sql
    desc database extended <databasename>;
    ```
    修改数据库结构,增加数据库属性
    ```sql
    alter database <databasename> set dbproperties('created'='xw'); [属性即是键值对，可以自行添加]
    ```
    显示所有的表
    ```sql
    show tables;
    ```
    查看表结构
    ```sql
    desc <tablename>;
    ```
    查看表结构及扩展信息
    ```sql
    desc extended <tablename>;
    desc formatted <tablename>;         // 友好显示
    ```
    创建表，MySQL记录表的描述信息，对应HDFS上的表文件夹，不加修饰则是内部表[托管表 managed_table 即由Hive管理]
    ```sql
    create table <tablename>(id int, name string, age int,...)
    comment 'table description'                                         // 表的描述
    row format delimited                                                // 定义分隔符
    fields terminated by ','                                            // 列分隔符，用于HQL查询某一字段时，对文本进行分割
    lines terminated by '\n'                                            // 行分隔符
    stored as textfile;                                                 // 以文本形式存储，也可以是序列文件或RC文件
    [e.g. 
    1. create table test(id int, name string, age int) comment 'table description' row format delimited fields terminated by ',' lines terminated 
       by '\n' stored as textfile;
    2. CREATE TABLE if not exists test (name string, str_arr ARRAY<string>, t_struct STRUCT<sex:string,age:int>, t_map MAP<string,int>, t_map_arr 
       MAP<string,ARRAY<string>> ) ROW FORMAT DELIMITED FIELDS TERMINATED BY '|' COLLECTION ITEMS TERMINATED BY ',' MAP KEYS TERMINATED BY ':';
    ]
    注：分隔符不能为分号
    ```
    从本地加载数据，即插入数据，即将数据put到HDFS上的对应文件夹中
    ```sql
    LOAD DATA LOCAL INPATH '...[本地路径]' OVERWRITE INTO TABLE <tablename>;        // 省去local则是加载集群上的数据，该加载时移动[剪切]
                                                                                    // overwrite 会覆盖原来的数据
    ```
    重命名表名
    ```sql
    alter table <tablename> rename to <new tablename>;
    ```
    添加列
    ```sql
    alter table <tablename> add columns(...)
    ```
    复制表
    ```sql
    create table default.test1 like mybase.test2;       // 复制mybase数据库中的test2到default中的test1，只复制表结构，不复制数据
    ```
    创建表的时候复制数据，该功能不能用于外部表
    ```sql
    create table <tablename> as select ... from <tablename> where ...
    ```
    复制表数据，批量插入
    ```sql
    insert into <tablename> select ... from <tablename> where ...
    ```
    导出数据，即把hive[hdfs]中的数据导出到本地
    ```sql
    insert overwrite local directory '...[local path]' select * from test where ...
    ```
    连接join
    ```sql
    Hive默认的是mapjoin
    属性设置为hive.auto.convert.join，默认是true
    动态设置mapjoin
        SELECT /*+ MAPJOIN(table_name) */ ...
    mapjoin不支持以下操作：
        在UNION ALL, LATERAL VIEW, GROUP BY/JOIN/SORT BY/CLUSTER BY/DISTRIBUTE BY之后使用
        在UNION, JOIN, and 另一个 MAPJOIN之前使用
    ```
    类型转换
    ```sql
    cast(value as type);
    select cast('100' as int) from xxx;
    ```
    修改分隔符
    ```sql
    alter table <tablename> set serdeproperties ('field.delim' = ',');
    ```
    修改表位置
    ```sql
    alter table <tablename> set location '...[path]';
    ```
    保护表[不能被删除]  
    ```sql
    alter table <tablename> enable no_drop;
    alter table <tablename> disable no_drop; // 取消保护
    ```
    离线表[不能查询]
    ```sql
    alter table <tablename> enable offline;
    alter table <tablename> disable offline; // 取消离线
    ```
    导出表到hdfs
    ```sql
    EXPORT TABLE employee TO '/home/xw/tmp';
    ```
    从hdfs上导入数据到一张新表中
    ```sql
    IMPORT TABLE empolyee_imported FROM '/home/xw/tmp';
    ```
    排序
    ```
    1. order by         全局排序
    2. sort by          每个reducer排序，并不整体排序
    3. DISTRIBUTE BY    类似于分组
    4. CLUSTER BY       先DISTRIBUTE BY后sort by，即先分组[reducer]，后组内排序
    ```
4.**表分类**
```
1. internal/managed[内部表/托管表]    
        由Hive完全管理表和数据的生命周期
        默认创建的表是内部表
        删除表的时候，数据也被删除
    2. external[外部表]
        是由LOCATION属性指定数据存放地，而不是由默认的warehouse决定的
        删除表的时候，表的元数据被删除了，但是数据还在
        [e.g. CREATE EXTERNAL TABLE if not exists test_ext ( name string, str_arr ARRAY<string>, t_struct STRUCT<sex:string,age:int>, t_map 
        MAP<string,int>, t_map_arr MAP<string,ARRAY<string>> ) ROW FORMAT DELIMITED FIELDS TERMINATED BY '|' COLLECTION ITEMS TERMINATED BY ',' MAP KEYS 
        TERMINATED BY ':' LOCATION '/user/xw/data/tmp';]
    3. 临时表
        在hive session结束时被删除
    4. 分区表
        针对目录[将目录切分为子目录]
        显示分区
            SHOW PARTITIONS <table_name>;
        建立分区表
            create table test_partition(id int, name string, age int) comment 'table description' PARTITIONED BY (year INT, month INT) row format delimited 
            fields terminated by ',' lines terminated by '\n' stored as textfile;
        分区表需要手动激活分区[即给分区建立子目录]
            alter table test_partition add partition(year=2017,month=3) partition(year=2017,month=4);
        删除分区
            alter table test_partition drop if exists partition(year=2017,month=3);
        向分区表加载数据
            load data local inpath '/home/xw/tmp/table.txt' overwrite into table test_partition partition(year=2016,month=2);
        常用的partition keys
            date and time
            locations
            business logics
    5. bucket表
        针对文件[将文件切割成片段]
        将指定列根据hash算法进行切割，对应列的值相同的记录始终会被划分到同一个桶中
        每个bucket的大小的设定：一个比较好的选择是将每个bucket的大小设置为hadoop blocksize的两倍，例如，blocksize是256MB，则将每个bucket的大小设为512MB
        建立bucket表
            create table test_bucket(id int, name string, age int) comment 'bucket table' clustered by(id) into 2 buckets row format delimited 
            fields terminated by ',' lines terminated by '\n' stored as textfile;
        加载数据到bucket表
            INSERT OVERWRITE TABLE test_bucket SELECT * FROM test;
            注：不能使用load来加载数据，load加载数据只是将数据put到hdfs上，不经过MR，因而不会划分桶
            bucket的划分紧紧依赖于底层的数据加载，为了能够正确加载数据到bucket表中，我们可以：
                1.将reduce的数量设为自定义的bucket数
                    set map.reduce.tasks = 2;
                2.设置可以强制划分bucket表
                    set hive.enforce.bucketing = true;
            数据的加载是通过MR进行的，MR中shuffle过程中的拷贝即分区，就可以实现桶表的划分，MR中默认是按照key进行hash分区的，设置reduce的数目与划分bucket表的数量一致，就正好实现了bucket表的划分。
```
5.**Hive优化**
```
    1. explain[解释执行计划]
        explain select sum(...) from table_name;
    2. 动态分区调整[*]
        hive.exec.dynamic.partition.mode = strict       // 默认是strict
    3. bucket表[*]
    4. 索引[*]
    5. 文件格式优化
        TEXTFILE, SEQUENCEFILE, RCFILE[可切分], ORC[增强的RCFILE], 和 PARQUET
    6. 压缩
        SET hive.exec.compress.intermediate=true        // 设置MR中间数据可以进行压缩，默认是false
        SET hive.intermediate.compression.codec=org.apache.hadoop.io.compress.SnappyCodec   // 设置MR中间数据压缩算法
        SET hive.exec.compress.output=true              // 设置MR输出数据可以进行压缩，默认是false
        SET mapreduce.map.output.compress.codec=org.apache.hadoop.io.compress.SnappyCodec   // 设置MR输出数据压缩算法，Hadoop的配置
    7. 设置本地模式，在当台机器上处理所有任务
        适用于小数据情况
        hive.exec.mode.local.auto = true                // 默认false
        mapreduce.framework.name = local
        运行本地模式的job需要满足的条件
            job的输入总大小要小于hive.exec.mode.local.auto.inputbytes.max        // 默认是134217728
            map任务的数量要小于hive.exec.mode.local.auto.input.files.max        // 默认是4
            reduce任务的数量要是1或者是0
    8. JVM重用[*]
        SET mapreduce.job.jvm.numtasks=5;               // 每个JVM能运行的任务数，默认是1，即为每一个任务开一个JVM，如果设为-1，则没有限制
    9. 并行执行
        如果Job之间没有依赖，可以并行执行
        hive.exec.parallel = true                       // 默认是false
        SET hive.exec.parallel.thread.number=16         // 默认是8，能够并行执行的job数
    10. 启动limit调优，避免全表扫描，使用抽样机制
        select * from ... limit 1,2
        hive.limit.optimize.enable = true               // 默认是false
    11. JOIN[*]
        动态mapjoin使用(/*+ streamtable(table_name)*/)
        连接查询表的大小从左到右依次增长
        默认是true
        SET hive.auto.convert.join=true                 // 默认是true
        SET hive.mapjoin.smalltable.filesize=600000000  // 默认是25000000，mapjoin的阀值，如果小表小于该值，则会将普通join[reduce join]转为mapjoin  
    12. 严格模式[*]
        启用严格模式：hive.mapred.mode = strict            // Deprecated
        hive.strict.checks.large.query = true
        该设置会禁用： 1. 不指定分页的orderby
                        2. 对分区表不指定分区进行查询    
                        3. 和数据量无关，只是一个查询模式
        hive.strict.checks.type.safety = true
        严格类型安全，该属性不允许以下操作：1. bigint和string之间的比较
                                            2. bigint和double之间的比较
        hive.strict.checks.cartesian.product = true
        该属性不允许笛卡尔积操作
    13. 调整Mapper和Reducer的个数
        hive.exec.reducers.bytes.per.reducer = 256000000    // 每个reduce任务的字节数，256M
        hive.exec.reducers.max = 1009                       // reduce task的最大值，属性为负数时，会使用该属性
    14. 推测执行[hadoop]
        让多个map/reduce多个实例并发执行
        mapreduce.map.speculative = true                    // 默认是true
        mapreduce.reduce.speculative = true                 // 默认是true
    15. 多个分组优化
        hive.multigroupby.singlereducer = true              // 默认是true
        若多个groupby使用的是一个公用的字段，则这些groupby可以生成一个MR
    16. 虚拟列
        hive.exec.rowoffset = true                          // 默认是false
```