## Linux相关概念以及常用命令

**说在前面的话**

    本文用于记录Linux的相关概念以及常用命令，以便日后查询，以Ubuntu为例。

    概念之间无先后顺序，只是用于记录。
***
1.**163软件源**
```
笔者一般使用Ubuntu自带的软件源，自做镜像，若遇到要更换国内源的情况，可以参考
deb http://mirrors.163.com/ubuntu/ trusty main restricted universe multiverse
deb http://mirrors.163.com/ubuntu/ trusty-security main restricted universe multiverse
deb http://mirrors.163.com/ubuntu/ trusty-updates main restricted universe multiverse
deb http://mirrors.163.com/ubuntu/ trusty-proposed main restricted universe multiverse
deb http://mirrors.163.com/ubuntu/ trusty-backports main restricted universe multiverse
deb-src http://mirrors.163.com/ubuntu/ trusty main restricted universe multiverse
deb-src http://mirrors.163.com/ubuntu/ trusty-security main restricted universe multiverse
deb-src http://mirrors.163.com/ubuntu/ trusty-updates main restricted universe multiverse
deb-src http://mirrors.163.com/ubuntu/ trusty-proposed main restricted universe multiverse
deb-src http://mirrors.163.com/ubuntu/ trusty-backports main restricted universe multiverse
```
2.**琐碎知识点**
```
1.  安装Ubuntu时创建的用户是在sudo组下的，所以，具有sudo权限
        e.g. 安装时，我的用户名是xw，则xw用户的primary group是xw，次要组有很多，其中包含sudo
    用useradd或adduser创建的用户若不指定组gid，是不包含在sudo组下的，所以，需要在visudo中添加该用户的sudo权限，
    更一般的做法则是在/etc/sudoers.d目录下添加文件
2.  visudo中用户权限说明
        xw ALL=(ALL:ALL)ALL
        xw          : 用户名
        第一个ALL  : 是从任何主机登录
        第二个ALL  : 是以任何用户的身份
        第三个ALL  : 是以任何用户组的身份
        第四个ALL  : 是可以执行任何指令
        综述： xw用户可以从任何主机登录，用任何用户或用户组的身份执行任何指令
3.  server中开启多个shell：ctrl + alt + F5
4.  apt-get update  : 从软件源（sourses.list）对应的软件仓库中将包列表get下来
5.  apt-get upgrade : 升级软件，将对应的包名称到软件仓库中寻找，找到仓库中对应的包，将其下载下来进行安装
6.  可将所需的包均从仓库中下载下来，做成镜像文件，即自己的软件仓库，挂在到mnt下，再将sources.list中的目录名称改成挂载的目录，就可以获取相应的包并进行安装
    sources.list 的修改: 将http网址改成 deb file:/mnt/cdrom(自定义) ./
7.  ctrl + alt + F6         // 切换到文本模式
8.  ctrl + alt + F7         // 切换到界面模式
9.  设置静态ip[VM中的虚拟机为例]
    修改 /etc/network/interfaces
        # This file describes the network interfaces available on your system
        # and how to activate them. For more information, see interfaces(5).
        # The loopback network interface
        auto lo
        iface lo inet loopback
        # The primary network interface
        # auto eth0
        # iface eth0 inet dhcp
        auto eht0
        iface eth0 inet static          // 设置静态ip
        address 192.168.157.134         // 设置ip地址，到DHCP设置里查看IP地址段
        netmask 255.255.255.0           // 设置子网掩码
        gateway 192.168.157.2           // 设置网关，到NAT设置中查看
        dns-nameservers 192.168.157.2   // 设置dns

    使静态IP生效
    $> sudo ifdown eth0
    $> sudo ifup eth0
10. 设置文本启动
    修改 /etc/default/grub
        # GRUB_CMDLINE_LINUX_DEFAULT="quiet"
        GRUB_CMDLINE_LINUX_DEFAULT="text"
        # Uncomment to disable graphical terminal (grub-pc only)
        GRUB_TERMINAL=console                                       // 打开注释
    使文本启动生效
        update-grub
    切换到图形界面
        sudo /etc/init.d/ligthdm start
    切换到文本界面
        ctrl + alt + f1
    可能的问题
        可能切换到图形界面后，需要多次输入密码，也无法进入桌面系统
    解决的办法
        查看 /var/log/upstart/lightdm.log
        发现 ~/.Xauthority 权限不够，无法写入
        修改 ~/.Xauthority 的权限为646

```
3.**常用简单命令**
```
xargs       -> 将输入的多行转换为一行字符串
``(反引号) -> 将字符串以命令的方式解析[$() 也有次功能]
    echo hostname   : 打印出 hostname 字符串
    echo `hostname` : 打印出主机名称
${}         -> 输出环境变量
tar         -> 归档/解归档（不是压缩,压缩是gzip,gunzip）
ln          -> 链接，默认是硬链接
                硬链接实际上是创建了一个文件，并实时镜像，删除一个，另一个不会删除,目录不允许创建硬链接
                软链接相当于快捷方式，ln -s target linkname
cd -P       -> 进入物理地址，对于符号链接而言
pwd -P      -> 进入物理地址，对于符号链接而言
jobs        -> 查看后台执行作业
xxx &       -> 将程序放到后台执行
bg          -> background 后台
fg          -> 前台
kill %n     -> 杀死某个作业
ps          -> 进程快照，相当于任务管理器
which       -> 从环境变量中寻找
```
4.**常用行命令**
```
:w !sudo tee % > /dev/null              -> vim忘加权限时用该命令保存,/dev/null为黑洞
ls *.txt | cp `xargs` ~/myfolder/       -> 将当前目录下的txt文件拷贝到 ~/myfolder 下
basename /bin/ping                      -> 提取文件名，等同于 basename `which ping`
dirname /bin/ping                       -> 提取目录名
export mypath=${mypath:-$mypath1}       -> 设置环境变量，若mypath存在，则为mypath，否则为mypath1，相当于三元运算符
nc -l 1234 &                            -> 启动服务器，监听1234端口,后台执行
```
5.**脚本相关**
```
if[ xxx ] ; then                        -> 中括号两边空格
-lt                                     -> 小于 <
-gt                                     -> 大于 >
-eq / =                                 -> 等于 =
$0                                      -> 提取执行命令
$1                                      -> 提取第一个参数
$?                                      -> 保存上次命令执行结果，0表示成功，1表示失败
$#                                      -> 现在参数的个数，若shift改变参数个数，则该值会发生变化
$@                                      -> 输出所有参数
-e                                      -> exists 判断文件(夹)是否存在
-d                                      -> 判断目录是否存在
a&&b                                    -> a 命令执行成功后执行b命令
a||b                                    -> a 命令执行失败后执行b命令
a;b                                     -> 多个命令执行，无逻辑
(a;b)                                   -> 组合命令，只在当前目录执行，不切换目录

注： 定义变量，若赋值，等号左右不能有空格
```