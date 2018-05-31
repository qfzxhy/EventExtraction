<%@ page language="java" contentType="text/html; charset=utf-8"
    pageEncoding="utf-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>

<meta charset="UTF-8">
<title>EES</title>
<link rel="stylesheet" href="https://cdn.bootcss.com/bootstrap/3.3.7/css/bootstrap.min.css" integrity="sha384-BVYiiSIFeK1dGmJRAkycuHAHRg32OmUcww7on3RYdg4Va+PmSTsz/K68vbdEjh4u" crossorigin="anonymous">
<style type="text/css">

#container{
	
	width: 1024px;
}

h1
{
    font-size:xx-large;
    font-family:"华文新魏";
    text-shadow: 2px 2px 4px #bbb; 
    font-size:2.5em; 
    text-align:center;
}
h2
{
	color:Gray; 
    font-size:10px;
    font-family:"华文新魏";
    text-shadow: 2px 2px 4px #bbb; 
    font-size:1.3em;
    text-align:left;
}
h3
{
	
    font-size:10px;
    font-family:"华文新魏";
    text-shadow: 2px 2px 4px #bbb; 
    font-size:1.3em;
    text-align:left;
}
#copyright{
	text-align:center; 
	color:Gray; 
	font-size:1.2em;
	border-bottom-width: 0px;
	border-left-width: 0px;
	border-right-width: 0px;
}

.hyperlink
{
    color:#5eb876;
    font-size:1.3em;
}
.example{
	border-style: solid;
	border-width: 1px;
}

.conleft{
word-wrap:break-word; 
word-break:break-all; 
}
.label{
	float: right;
}
#nav ul {
Width:300px;    
height:40px;      

padding:0;
list-style:none; 

float: right
}
 
#nav ul li {
width:100px;
float:left;
text-align:center;
font:16px/2.5 "microsoft yahei";
}
#nav ul li a {
color:#800080; text-decoration:none;
}
#nav ul li a:hover {
display:block; color:#FFFFFF; background:#DC143C;
}
</style>


</head>
<body>
<nav class="navbar navbar-default" role="navigation">
    <div class="container-fluid">
    
    <div>
        <ul class="nav navbar-nav">
            <li><a href="introduce.jsp"><h3>中文事件抽取系统</h3></a></li>
            <li><a href="index.jsp"><h3>句子抽取</h3></a></li>
            <li><a href="index2.jsp"><h3>篇章抽取</h3></a></li>
            <li><a href="index4.jsp"><h3>模板添加</h3></a></li>
              <li><a href="index5.jsp"><h3>当天新闻</h3></a></li>
          
        </ul>
    </div>
    </div>
    
</nav>
<div class = "container">
	<div id = "example">
		<h1>中文军事政治新闻事件抽取系统介绍</h1>
	</div>
<h2>随着计算机的发展和互联网的日益普及，人们每天接受到的文本信息就像 汪洋大海，
如何准确有效的从大量无序、杂乱、无结构的文本信息中提取出用 户感兴趣的结构化信息已经成为亟待解决的问题。
信息抽取技术就可以将自然 语言文本处理成有意义的结构化知识，互联网是各种类型的信息的载体，
如果 信息技术非常成熟，那么互联网就可以抽取成一个庞大的知识库，人类获取知 识或者信息的方式会更加便捷</h2>
<div class="panel-body">
<h2>事件抽取任务描述：给定军事句子，识别句子中<事件触发词，事件触发词类别，事件参与者，事件发生时间和地点></h2>
<img src="a.jpg" width=700px height=500px>  
<h2>系统流程描述</h2>
<img src="b.jpg" width=700px height=300px>  
<h2>事件类别</h2>
<img src="c.jpg" width=700px height=600px>  
</div>

</div>

</body>