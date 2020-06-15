# Introduction #
TipDM建模平台分支维护版

# Documentation #
[使用文档](http://python.tipdm.org/bzzx/index.jhtml?n=%E6%93%8D%E4%BD%9C%E6%96%87%E6%A1%A3)

# Communication #
[社区交流](http://bbs.tipdm.org)

# Features #
1. 基于Python，用于数据挖掘建模。
2. 使用直观的拖放式图形界面构建数据挖掘工作流程，无需编程。
3. 支持多种数据源，包括CSV文件和关系型数据库。
4. 支持挖掘流程每个节点的结果在线预览。
5. 提供5大类共40种算法组件，包括数据预处理、分类、聚类等数据挖掘算法。
6. 支持新增/编辑算法组件，自定义程度高。
7. 提供众多公开可用的数据挖掘示例工程，一键创建，快速运行。
8. 提供完善的交流社区，提供数据挖掘相关的学习资源（数据、代码和模型等）。

# Screenshot #
![输入图片说明](https://images.gitee.com/uploads/images/2019/0617/112412_0a4abed4_4964548.jpeg "1.jpg")
![输入图片说明](https://images.gitee.com/uploads/images/2019/0617/112438_656d0053_4964548.jpeg "2.jpg")
![输入图片说明](https://images.gitee.com/uploads/images/2019/0617/112450_a0ff4eb8_4964548.jpeg "3.jpg")
![输入图片说明](https://images.gitee.com/uploads/images/2019/0617/112509_238a7067_4964548.jpeg "4.jpg")

# Development #
## 环境依赖 ##
- [Oracle JDK 1.8.x及以上版本](http://www.oracle.com/technetwork/java/javasebusiness/downloads/java-archive-downloads-javase6-419409.html)，安装详情可参考[JDK安装教程](https://docs.oracle.com/javase/8/docs/technotes/guides/install/install_overview.html)
- [Apache-Maven 3.x及以上版本](http://maven.apache.org)，安装详情可参考[Maven安装教程](https://maven.apache.org/install.html)
- [Apache-Tomcat 8.x及以上版本](http://tomcat.apache.org)
- [PostgreSQL 9.4.x及以上版本](http://www.postgresql.org/download/)
- [Python 3.6.x版本及以上版本](https://www.python.org/)
- [Redis 3.2.x及以上版本](https://redis.io/)
- [Alibaba DataX](https://github.com/alibaba/DataX)
- IntelliJ Idea IDE(可选，您也可以使用其他IDE，如eclipse、NetBeans)，安装详情可参考[IntelliJ安装教程](https://www.jetbrains.com/help/idea/installing-and-launching.html)

### 安装Java开发环境 ###
略
### Python3环境配置 ###
```
# 创建python3虚拟环境
python3 -m venv python3_venv

# 激活(进入)虚拟环境
source ./python3_venv/bin/activate

# 在虚拟环境安装所需依赖
pip install numpy==1.16.4 -i http://pypi.douban.com/simple --trusted-host pypi.douban.com
pip install arch==4.4.1 -i http://pypi.douban.com/simple --trusted-host pypi.douban.com
pip install docx==0.2.4 -i http://pypi.douban.com/simple --trusted-host pypi.douban.com
pip install gensim==3.6.0 -i http://pypi.douban.com/simple --trusted-host pypi.douban.com
pip install graphviz==0.10.1 -i http://pypi.douban.com/simple --trusted-host pypi.douban.com
pip install jieba==0.38 -i http://pypi.douban.com/simple --trusted-host pypi.douban.com
pip install jieba-fast==0.53 -i http://pypi.douban.com/simple --trusted-host pypi.douban.com
pip install matplotlib==2.2.2 -i http://pypi.douban.com/simple --trusted-host pypi.douban.com
pip install pandas==0.23.4 -i http://pypi.douban.com/simple --trusted-host pypi.douban.com
pip install pdfminer3k==1.3.1 -i http://pypi.douban.com/simple --trusted-host pypi.douban.com
pip install pyclust==0.2.0 -i http://pypi.douban.com/simple --trusted-host pypi.douban.com
pip install pydot==1.2.4 -i http://pypi.douban.com/simple --trusted-host pypi.douban.com
pip install python-docx==0.8.10 -i http://pypi.douban.com/simple --trusted-host pypi.douban.com
pip install scikit-learn==0.19.1 -i http://pypi.douban.com/simple --trusted-host pypi.douban.com
pip install scipy==0.19.1 -i http://pypi.douban.com/simple --trusted-host pypi.douban.com
pip install SQLAlchemy==1.2.0 -i http://pypi.douban.com/simple --trusted-host pypi.douban.com
pip install scikit-learn==0.19.1 -i http://pypi.douban.com/simple --trusted-host pypi.douban.com
pip install statsmodels==0.9.0 -i http://pypi.douban.com/simple --trusted-host pypi.douban.com
pip install tensorflow==1.14.0 -i http://pypi.douban.com/simple --trusted-host pypi.douban.com
pip install thulac==0.2.0 -i http://pypi.douban.com/simple --trusted-host pypi.douban.com
pip install wordcloud==1.5.0 -i http://pypi.douban.com/simple --trusted-host pypi.douban.com
pip install psycopg2 -i http://pypi.douban.com/simple --trusted-host pypi.douban.com
pip install joblib -i http://pypi.douban.com/simple --trusted-host pypi.douban.com
pip install matplot -i http://pypi.douban.com/simple --trusted-host pypi.douban.com

# 退出虚拟环境
deactivate
```
### 安装并初始化PostgreSQL ###
    yum install https://download.postgresql.org/pub/repos/yum/reporpms/EL-7-x86_64/pgdg-redhat-repo-latest.noarch.rpm
    yum install postgresql96-server
    
    # 初始化数据库并启用自动启动
    /usr/pgsql-9.6/bin/postgresql96-setup initdb
    systemctl enable postgresql-9.6
    systemctl start postgresql-9.6
    
    # 进入postgres用户(安装pgsql时自动创建的用户)，然后psql进入tipdm库(自行创建)，执行初始化脚本
    su - postgres
    psql tipdm
    tipdm=# \i /root/sql/initData.sql

## 快速入门 ##
##### 配置文件说明 #####

	sysconfig/database.properties			数据库配置文件
	sysconfig/dbSupport.config			在此配置系统可支持的数据库类型
	sysconfig/system.properties			系统的相关配置
	sysconfig/redis.properties			Redis
	PyConnection.xml				Python服务(该文件在sysconfig目录的上层)

##### 源码模块说明 #####

	framework-common		公共模块
	framework-model  		数据模型
	framework-persist 		数据持久化
	framework-service 		service
	tipdm-server  			后台服务

### 注意事项
1. tomcat不要放在有空格和中文的路径下，否则调用DataX会出问题
1. 测试用表添加权限
    
    GRANT ALL ON ALL tables in schema "admin" to admin;
1. 执行python命令处，添加进入python3虚拟环境语句
```
com.tipdm.framework.dmserver.pyserve.PySftpProgressMonitor
String command = "export LANG=zh_CN.UTF-8;" +
             "export LC_CTYPE=zh_CN.UTF-8;" +
             "export LC_ALL=zh_CN.UTF-8; " +
            // 进入python3虚拟环境
            "source /root/venv/bin/activate;"+
             "python {}";
```
