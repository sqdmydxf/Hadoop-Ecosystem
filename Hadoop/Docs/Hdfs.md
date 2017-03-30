## HDFS相关概念以及常用命令

**说在前面的话**

    本文用于记录Hadoop中HDFS相关概念以及常用命令，以便日后查询。

    概念之间无先后顺序，只是用于记录。
***
1.**角色**
```
hadoop fs [hdfs dfs]        // 普通用户
hdfs dfsadmin               // hdfs管理员
```
2.**edit日志和fsimage文件融合**
```
1. hadoop集群启动时融合
    生成新的inprogress_edits日志文件，将旧的inprogress_edits日志文件与fsimage文件融合，但是不生成新的fsimage文件
2. SecondaryNamenode周期生成检查点，进行融合（并不生成检查点）
    生成新的inprogress_edits日志文件，将旧的inprogress_edits日志文件与fsimage文件拷贝到secondaryNamenode中，
    逐一执行edits日志文件中的指令，并写进fsimage文件中，生成一个检查点文件，将检查点文件拷贝到namenode中，
    重命名检查点，即去掉后缀，并使之生效
3. 手动融合，进入安全模式，保存命名空间
    生成新的inprogress_edits日志文件，将旧的inprogress_edits日志文件与fsimage文件融合，写进一个新的fsimage文件中
```
3.**dfs.hosts / dfs.hosts.exclude**
```
0. 说明： slaves.sh 中配置的节点信息只是说明在集群启动的时候启动这些节点，而这些节点能否连上 resourcemanager ，需要配置这两个属性
1. 若没有配置这两个属性，则默认情况下，属性值为空，表示所有节点都能连
2. 在 hdfs-site.xml 文件中配置 dfs.hosts / dfs.hosts.exclude 属性
3. 属性值是文件的绝对路径，可在 hadoop 配置目录下创建 dfs.hosts.include / dfs.hosts.exclude 两个文件
4. 文件中的内容是 slaves.sh 中配置的节点
5. 属性优先级： include > exclude 
    dfs.hosts           dfs.hosts.exclude
    -------------------------------------
        0                       0           // 不能连
        0                       1           // 不能连
        1                       0           // 能连
        1                       1           // 能连，但是显示decommissioning，即不可用，但文件能够存储在该节点上
6. 重新配置后，可刷新节点，而无需重启集群
    hdfs dfsadmin -refreshNodes
```
4.**safemode(安全模式)**
```
1. dfsadmin 手动进入
2. 安全模式下，不可put文件
3. 安全模式下，可手动融合edits日志和fsimage文件，即保存命名空间
4. 安全模式下，不可向hdfs文件系统中写入数据，比如，put文件，创建目录...
5. 常用命令
    hdfs dfsadmin -safemode get     // 获取当前是否在安全模式下
    hdfs dfsadmin -safemode enter   // 手动进入安全模式
    hdfs dfsadmin -safemode leave   // 手动离开安全模式
    hdfs dfsadmin -safemode wait    // 等待退出安全模式，一般在脚本中使用
```
5.**目录快照**
```
1. 默认情况下，目录是不允许创建快照的
2. 需要管理员允许目录快照
    hdfs dfsadmin -allowSnapshot <dirname>
3. 创建快照
    hadoop fs -createSnapshot <dirname> <snapname>
    快照保存在<dirname>/.snapshot/<snapname>下
4. 重命名快照
    hadoop fs -renameSnapshot <dirname> <oldSnapname> <newSnapname>
5. 删除快照
    hadoop fs -deleteSnapshot <dirname> <snapname>
```
6.**回收站**
```
1. 每个用户都有一个回收站，在 /user/xw/.Trash下
2. 回收站默认保存时间是 0s，即禁用回收站，可在core-site.xml中配置fs.trash.interval属性设置
3. shell中的删除是进回收站的，而编程中的删除是不进入回收站的，直接被删除，除非调用moveToTrash()方法
4. 清空回收站
    1) hadoop fs -expunge 或者 超过fs.trash.interval设置的时间，则将回收站内的文件设置检查点，放到时间戳表示的文件夹下
    2) 配置fs.trash.checkpoint.interval属性，设置间隔多长时间检查回收站的检查点中的文件，若该属性的值为0，
       则和fs.trash.interval属性值相同,将其删除
    3) fs.trash.checkpoint.interval属性 <= fs.trash.interval属性值
```
7.**配额(也可以叫限制)**
```
1. 配额：hdfs dfsadmin -setQuota <quota> <dirname>
    即设置该<dirname>中元素的个数，该元素包括文件，目录，以及子元素
    <quota> > 0
2. 空间配额：hdfs dfsadmin -setSpaceQuota <quota> <dirname>
    即设置该<dirname>中物理空间的大小
    <quota> > 0，比如 10[B]，10m，10g
```
8.**image/edits查看器**
```
1. oiv(offline image viewer)
    hdfs oiv -i imagefile -o outputfile -p processor(XML)
2. oev(offline edits viewer)
    hdfs oev -i editsfile -o outputfile -p processor(XML)
```