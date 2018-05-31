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
	
	width: 800px;
}

h1
{
    font-size:xx-large;
    font-family:"华文新魏";
    text-shadow: 2px 2px 4px #bbb; 
    font-size:2.5em; 
    text-align:center;
}

#copyright{
	text-align:center; 
	color:Gray; 
	font-size:1.2em;
	border-bottom-width: 0px;
	border-left-width: 0px;
	border-right-width: 0px;
}
h3
{
	
    font-size:10px;
    font-family:"华文新魏";
    text-shadow: 2px 2px 4px #bbb; 
    font-size:1.3em;
    text-align:left;
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
             <li><a href="index4.jsp"><h3>系统训练</h3></a></li>
              <li><a href="index5.jsp"><h3>实时显示</h3></a></li>
          
        </ul>
    </div>
    </div>
</nav>

<div class = "container" style="width: 800px">
	<div id = "example">
		<h1>中文军事政治新闻事件抽取系统</h1>
	</div>
	
	
	
	<!-- <div class="panel panel-primary">
	  <div class="panel-heading">
	    <h3 class="panel-title">举例</h3>
	  </div>
	  <div class="panel-body">
	    <ul class="list-group">
		  <li class="list-group-item list-group-item-info">许其亮会见马来西亚海军司令		抽取结果：<许其亮，会见，马来西亚海军司令，3></li>
		  <li class="list-group-item list-group-item-info">马来西亚首家清真航空公司宣布暂时停止营运</li>
		  <li class="list-group-item list-group-item-info">阿富汗发生7.1级地震</li>
		</ul>
	  </div>
	</div> -->
	
	<div class="panel panel-primary">
	  <div class="panel-heading"><h3 class="panel-title">请输入军事新闻语句</h3></div>
	  <div class="panel-body">
	    <div class="col-lg-6">
		    <div class="input-group">
		    	<form action="forward" method="post">  
			      <input type="text" id = "title" name="sent" class="form-control" style="width: 700px" placeholder="军事句子" value=<%=request.getParameter("sent")==null ? "":request.getParameter("sent") %>>
			        <input type="submit" value="Go!" onclick="control()"/>  
			    </form>  
		    </div><!-- /input-group -->
  		</div><!-- /.col-lg-6 -->
	  </div>
	  <div class="panel-body" id = "showprocess" style="display:none;">
	    <div class="progress">
		  <div id="processbar" class="progress-bar progress-bar-striped " role="progressbar" aria-valuenow="45" aria-valuemin="0" aria-valuemax="100" style="width: 40%">
		    <span class="sr-only">45% Complete</span>
		  </div>
		</div>
	  </div>
	  
	</div>
	
	
	
	<div id = "result" style="display:none;" class="panel panel-primary">
	  <div class="panel-heading">
	  	<h3 class="panel-title">Results</h3>
	  </div>
	  <div class="panel-body">
	    <!-- begin -->
	    <div class="panel panel-info">
		  <div class="panel-heading">
		  	<h3 class="panel-title">title</h3>
		  </div>
		  <div  class="panel-body" >
		    <div id = "result_title" class="conleft"></div>
		  </div>
		</div>
	    <!-- exit words -->
	    <div class="panel panel-success">
		  <div class="panel-heading">
		  	<h3 class="panel-title">existing words</h3>
		  </div>
		  <div  class="panel-body" >
		    <div id = "result_isword" class="conleft"></div><span class="label label-warning"><a id ="isworda" href="JavaScript:showIsword();" class="navbar-link">more</a></span>
		  </div>
		</div>
		<!-- len 3 -->
		<div class="panel panel-success">
		  <div class="panel-heading">
		  	<h3 class="panel-title">length 3</h3>
		  </div>
		  <div  class="panel-body" >
		    <div id = "result_len3" class="conleft"></div><span class="label label-warning"><a id ="length3a" href="JavaScript:showLength3();" class="navbar-link">more</a></span>
		  </div>
		</div>
		<!-- len 4 -->
		<div class="panel panel-success">
		  <div class="panel-heading">
		  	<h3 class="panel-title">length 4</h3>
		  </div>
		  <div  class="panel-body" >
		    <div id = "result_len4" class="conleft"></div><span class="label label-warning"><a id ="length4a" href="JavaScript:showLength4();" class="navbar-link">more</a></span>
		  </div>
		</div>
		<!-- len 5 -->
		<div class="panel panel-success">
		  <div class="panel-heading">
		  	<h3 class="panel-title">length 5</h3>
		  </div>
		  <div  class="panel-body" >
		    <div id = "result_len5" class="conleft"></div><span class="label label-warning"><a id ="length5a" href="JavaScript:showLength5();" class="navbar-link">more</a></span>
		  </div>
		</div>
		<!-- len 6 -->
		<div class="panel panel-success">
		  <div class="panel-heading">
		  	<h3 class="panel-title">length 6</h3>
		  </div>
		  <div  class="panel-body" >
		    <div id = "result_len6" class="conleft"></div><span class="label label-warning"><a id ="length6a" href="JavaScript:showLength6();" class="navbar-link">more</a></span>
		  </div>
		</div>
		<!-- len 7 -->
	    <div class="panel panel-success">
		  <div class="panel-heading">
		  	<h3 class="panel-title">length 7</h3>
		  </div>
		  <div  class="panel-body" >
		    <div id = "result_len7" class="conleft"></div><span class="label label-warning"><a id ="length7a" href="JavaScript:showLength7();" class="navbar-link">more</a></span>
		  </div>
		</div>
	    
	    
	    
	    
	    
	    
	    
	    
	    <!-- end -->
	  </div>
	</div>
	<%
		String words = (String)request.getAttribute("words");
		String entitys = (String)request.getAttribute("entitys");
		String events = (String)request.getAttribute("events");
		String sentiment = (String)request.getAttribute("sentiment");
		
%>
	<div class="panel panel-primary">
	  <div class="panel-heading">
	  	<h3 class="panel-title">分词结果</h3>
	  </div>
	  <div  class="panel-body" >
	    <div id = "result" class="conleft">
	    <%if(words!=null){ %>
	    	<%=words %>
	    <%} %>
	    </div>
	  </div>
	</div>
	<div class="panel panel-primary">
	  <div class="panel-heading">
	  	<h3 class="panel-title">实体识别结果</h3>
	  </div>
	  <div  class="panel-body" >
	    <div id = "result" class="conleft">
	    <%if(entitys!=null){ %>
	    <%=entitys %>
	    <%} %>
	    </div>
	  </div>
	</div>
	<div class="panel panel-primary">
	  <div class="panel-heading">
	  	<h3 class="panel-title">事件抽取结果<发起者，触发词，承受者，时间，地点，事件类型></h3>
	  </div>
	  <div  class="panel-body" >
	    <div id = "result" class="conleft">
	    <%if(events!=null){ %>
	    <%=events %>
	    <%} %>
	    </div>
	  </div>
	</div>
	<div class="panel panel-primary">
	  <div class="panel-heading">
	  	<h3 class="panel-title">事件情感极性判断</h3>
	  </div>
	  <div  class="panel-body" >
	    <div id = "result" class="conleft">
	    <%if(sentiment!=null){ %>
	    <%=sentiment %>
	    <%} %>
	    </div>
	  </div>
	</div>
	
</div>





	
	


<input type="hidden" id = "len0top10" />
<input type="hidden" id = "len0top50" />
<input type="hidden" id = "len3top10" />
<input type="hidden" id = "len3top50" />
<input type="hidden" id = "len4top10" />
<input type="hidden" id = "len4top50" />
<input type="hidden" id = "len5top10" />
<input type="hidden" id = "len5top50" />
<input type="hidden" id = "len6top10" />
<input type="hidden" id = "len6top50" />
<input type="hidden" id = "len7top10" />
<input type="hidden" id = "len7top50" />



</body>

<script language="javascript"  type="text/javascript">

function control()
{
    $("result").text("显示");
}
</script>

</html>