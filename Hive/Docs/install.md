## Hive安装过程

**说在前面的话**

    本文用于记录Hive的安装过程，以便日后查询。

***
```
1. jdk[略]

2. hadoop[略]

3. 下载Hive

4. 安装Hive[tar] --> 设置软链接

5. 配置Hive[matestore database: MySql]
    a. 环境变量
        HADOOP_HOME=/usr/local/soft/hadoop
        HIVE_HOME=/usr/local/soft/hive
        HIVE_CONF_DIR=/usr/local/soft/hive/conf
        PATH=$PATH:/usr/local/soft/hive/bin:/usr/local/soft/hive/conf
    b. cd hive/conf
        cp hive-default.xml.template hive-site.xml
        cp hive-env.sh.template hive-env.sh
        cp hive-exec-log4j.properties.template hive-execlog4j.properties
        cp hive-log4j.properties.template hive-log4j.properties
    c. $HIVE_HOME/conf/hive-env.sh
        export HADOOP_HOME=/usr/local/soft/hadoop
        export HIVE_CONF_DIR=/usr/local/soft/hive/conf
    d. $HIVE_HOME/conf/hive-site.xml
        [the path for Hive warehouse storage. default:/user/hive/warehouse]
        hive.metastore.warehourse.dir:/user/hive/warehouse
        [temporary data file path. default:/tmp/hive-${user.name}]
        hive.exec.scratchdir:/tmp/hive-${user.name}
    e. 配置MySql作为matestore[默认是Derby]
        [$HIVE_HOME/conf/hive-site.xml]
        注：确保MySQL JDBC驱动程序在$HIVE_HOME/lib下
            在mysql中创建一个数据库，例如myhive
        <property>
            <name>javax.jdo.option.ConnectionURL</name>
            <value>jdbc:mysql://localhost:3306/myhive</value>
            <description>JDBC connect string for a JDBC metastore</description>
        </property>
        <property>
            <name>javax.jdo.option.ConnectionDriverName</name>
            <value>com.mysql.jdbc.Driver</value>
            <description>Driver class name for a JDBC metastore</description>
        </property>
        <property>
            <name>javax.jdo.option.ConnectionUserName</name>
            <value>root</value>
            <description>username to use against metastore database</description>
        </property>
        <property>
            <name>javax.jdo.option.ConnectionPassword</name>
            <value>root</value>
            <description>password to use against metastore database</description>
        </property>
    f. HDFS：
        hdfs dfs -mkdir /tmp
        hdfs dfs -mkdir /user/hive/warehouse
        hdfs dfs -chmod g+w /tmp
        hdfs dfs -chmod g+w /user/hive/warehouse

6. 启动Hive
    使用mysql数据库进行初始化,默认会建大量的表
    $HIVE_HOME/bin/schematool -dbType <db type> -initSchema
    [$HIVE_HOME/bin/schematool -dbType mysql -initSchema]
    执行hive
    $> hive

7. Relative path in absolute URI问题：
        [$HIVE_HOME/conf/hive-site.xml][给定绝对路径]
        <property>
            <name>hive.exec.local.scratchdir</name>
            <value>/home/xw/hive/tmp</value>
            <description>Local scratch space for Hive jobs</description>
        </property>
        <property>
            <name>hive.downloaded.resources.dir</name>
            <value>/home/xw/hive/downloads</value>
            <description>Temporary local directory for added resources in the remote file system.</description>
        </property>
```