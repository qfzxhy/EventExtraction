<%@page import="java.util.List"%>
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
h3
{
	
    font-size:10px;
    font-family:"华文新魏";
    text-shadow: 2px 2px 4px #bbb; 
    font-size:1.3em;
    text-align:left;
}
h1
{
    font-size:xx-large;
    font-family:"华文新魏";
    text-shadow: 2px 2px 4px #bbb; 
    font-size:2.5em; 
    text-align:center;
}
textarea{
	height:50px;
	width:1024px
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
          
        </ul>
    </div>
    </div>
</nav>



<div class = "container">
	<div id = "example">
		<h1>中文军事政治新闻事件抽取系统</h1>
	</div>
	
	
	
	
	
	<div class="panel panel-default">
	  <div class="panel-heading"><h3 class="panel-title">请输入军事新闻文章</h3></div>
	  <form action="forward2" method="post">  
			      
			      <textarea id="ta" name="content"><%=request.getParameter("content")==null ? "":request.getParameter("content") %></textarea>
			      
			        <input type="submit" value="Go!" onclick="control()"/>  
	  </form> 
	  
	  <%
	  String imgPath = (String)request.getAttribute("imgPath");
		
	  %>

	  <div class="panel-heading"><h3 class="panel-title">事件抽取结果</h3> </div>
	  <%if(imgPath != null){ %>
	 	<img src="http://localhost:8080/Test/imgurl">  
	  <%} %>

	  
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
var autoTextarea = function (elem, extra, maxHeight) {
    //判断elem是否为数组
    if(elem.length > 0){
        for(var i = 0; i < elem.length; i++ ){
           e(elem[i]);
        }
    }
    else{
        e(elem);
    }

    function e(elem){
	extra = extra || 0;
    var isFirefox = !!document.getBoxObjectFor || 'mozInnerScreenX' in window,
    isOpera = !!window.opera && !!window.opera.toString().indexOf('Opera'),
            addEvent = function (type, callback) {
                    elem.addEventListener ?
                            elem.addEventListener(type, callback, false) :
                            elem.attachEvent('on' + type, callback);
            },
            getStyle = elem.currentStyle ? function (name) {
                    var val = elem.currentStyle[name];

                    if (name === 'height' && val.search(/px/i) !== 1) {
                            var rect = elem.getBoundingClientRect();
                            return rect.bottom - rect.top -
                                    parseFloat(getStyle('paddingTop')) -
                                    parseFloat(getStyle('paddingBottom')) + 'px';        
                    };

                    return val;
            } : function (name) {
                            return getComputedStyle(elem, null)[name];
            },
            minHeight = parseFloat(getStyle('height'));

    elem.style.resize = 'none';

    var change = function () {
            var scrollTop, height,
                    padding = 0,
                    style = elem.style;

            if (elem._length === elem.value.length) return;
            elem._length = elem.value.length;

            if (!isFirefox && !isOpera) {
                    padding = parseInt(getStyle('paddingTop')) + parseInt(getStyle('paddingBottom'));
            };
            scrollTop = document.body.scrollTop || document.documentElement.scrollTop;

            elem.style.height = minHeight + 'px';
            if (elem.scrollHeight > minHeight) {
                    if (maxHeight && elem.scrollHeight > maxHeight) {
                            height = maxHeight - padding;
                            style.overflowY = 'auto';
                    } else {
                            height = elem.scrollHeight - padding;
                            style.overflowY = 'hidden';
                    };
                    style.height = height + extra + 'px';
                    scrollTop += parseInt(style.height) - elem.currHeight;
                    document.body.scrollTop = scrollTop;
                    document.documentElement.scrollTop = scrollTop;
                    elem.currHeight = parseInt(style.height);
            };
    };

    addEvent('propertychange', change);
    addEvent('input', change);
    addEvent('focus', change);
    change();
    }
};
var T = document.getElementById("ta");
autoTextarea(T)
</script>

</html>