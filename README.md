AndroidCrashReport
=================
AndroidCrashReport用于分析Android崩溃日志(java层和c层的都支持),统计日志崩溃信息，对日志整理归类，最终形成一个以崩溃触发函数分类，并按照崩溃次数从大到小排列的崩溃报表。
通过此报表我们可以清楚的看到，哪种类型的崩溃发生次数最多，崩溃的概率有多大。以方便我们对发生次数多，崩溃概率大的BUG，优先处理。

AndroidCrashReport架构图
-----------------
无

AndroidCrashReport主要功能
-----------------------------------
1. 统计java日志，生成报表
2. 根据符号表自动解析C层日志，统计C层日志，生成报表

AndroidCrashReport Limitations
-----------------------------------
1. 报表以txt文件格式的方式展现，不支持图表
2. 分析C日志的耗时较长。

AndroidCrashReport工作原理
------------------------------------------------------------------------------------
1. 从崩溃的文件中读取一条崩溃日志
2. 判断日志的类型，如果是C层的崩溃（以*** ***开头的），则执行步骤3.否则认为日志是java层的崩溃，执行步骤4
3. 调用Android NDK安装包下的ndk-stack命令，将日志解析成带符号的崩溃日志。(因为SO一般都设置了隐藏符号表，这时崩溃日志里面看不到崩溃的地址，看不到函数名，和文件的行号)
4. 分析日志的格式，生成日志的ID（ID相同的日志，具有相同崩溃信息），提取触发崩溃的函数，并用此函数名作为keyword
	ID相同表示崩溃日志完全一样，即崩溃时的调用堆栈完全一样，keyword相同只表示，触发崩溃的函数是一样的，但调用堆栈有可能不一样
	ID用于标识一个崩溃，而keyword用于标识一类崩溃
5. 用ID进行初次统计，计算得到每个崩溃发生的次数
6. 用keyword再次统计，计算得到每类崩溃发生的次数


安装（编译脚本在build目录下）
----------------------------------
```
环境：jdk1.7以上
Linux:
		cd build
		sh build.sh
Mac:
		双击build.command
Windows:
		双击build.bat
		
安装后会在bin目录下生成如下可执行文件：
	acrash-report.jar   可执行jar包，
	acrash-report.bat   windows下使用此批处理文件对acrash-report.jar进行了包装
	acrash-report.sh    Linux和Mac下使用此Shell脚本对acrash-report.jar进行了包装


```

acrash-report命令使用方法
---------------------------------------
Usage:
   acrash-report -sym <path> [-dump <path>]

      -dump Contains full path to the file containing the crash dump.
      -sym  Contains full path to the root directory for symbols.
      This is an optional parameter. If ommited, C crash will ignore.
注意：如果需要编译C层崩溃，请将Android ndk目录下的ndk-stack命令加入PATH环境变量。

报表文件说明（acrash-repor命令生成）
---------------------------------------------------------------------------------
```
XX_crash_report               报表目录
    c_report.txt              C层崩溃报表，此报表中只列出每种崩溃中崩溃次数最多的两个崩溃。
    c_detail_report.txt       C层崩溃详细报表，此报表列出每种崩溃的所有崩溃。
    java_report.txt           java层崩溃报表，此报表中只列出每种崩溃中崩溃次数最多的两个崩溃。
    java_detail_report.txt    java层崩溃详细报表，此报表列出每种崩溃的所有崩溃。
    invalid.txt               无效的崩溃日志，将会输出在此文件。
```

崩溃日志格式
-------------------------------------------------------
	格式1: 每条崩溃占一行，行内使用\\n进行分隔。
	格式2: 每条崩溃占多行，但两条日志之间需要有一个空行分隔。

引用
-----------------------------
无


注意
-----------------------------
AndroidCrashReport工具非常简单，近期也不会对AndroidCrashReport做出大的修改
如果有小的bug或者建议，请告知我，我会第一时间修复。

![Weixin QR Code](screenshot/weixin.jpg)
