<%@page import="java.util.List"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.io.*"%>


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
	
	
	
	 <div class="panel panel-default">
	  <div class="panel-heading">
	    
	  </div>
	   <%
	   List<String> res = new ArrayList<String>();
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader("G:\\MasterTwoU\\28proj\\Test\\WebContent\\events"));
			String line = null;
			while((line = br.readLine())!=null)
			{
				if(line.indexOf("<")!=-1)
					res.add(line.trim());
				
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			br.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		%>
	  <div class="panel-body">
	    <ul class="list-group">
	    	<%for(String re:res){ %>
		  <li class="list-group-item list-group-item-info"><%=re %></li>
		  <%} %>
		</ul>
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