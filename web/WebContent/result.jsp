<%@ page language="java" contentType="text/html; charset=utf-8"
    pageEncoding="utf-8"%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
	<body>
		<%
		String c = (String)request.getAttribute("er");
		String strURL = request.getParameter("second");
		String strRequest = (String)request.getAttribute("strRequest");
		String strSession = (String)request.getSession().getAttribute("strSession");
		
		%>
		<p>
			c：<%=c%>
		</p>		
		<p>
			URL中取得的属性值为：<%=strURL%>
		</p>		
		<p>
			request中取得的属性值为：<%=strRequest%>
		</p>
		<p>
			session中取得的属性值为：<%=strSession%>
		</p>
	</body>
</html>
