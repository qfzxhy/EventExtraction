

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

	public class HelloWorldServlet extends HttpServlet{
		private static final long serialVersionUID = 1L;  
		  
	    /** 
	     * 构造函数 
	     */  
	    public HelloWorldServlet()  
	    {  
	        super();  
	    }  
	      
	    /** 
	     * 初始化 
	     */  
	    public void init() throws ServletException  
	    {}  
	      
		@Override
		/** 
	     * doGet()方法 
	     */  
	    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException  
	    {  
	        doPost(request, response);  
	    }  
	      
	    /** 
	     * doPost()方法 
	     */  
	    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException  
	    {  
	    	response.setHeader("content-type", "text/html;charset=UTF-8");  
	    	response.setCharacterEncoding("UTF-8");  
	    	request.setCharacterEncoding("UTF-8");  
	    	String sent = request.getParameter("sent");
	    	System.out.println(sent);
	    	String url = "index.jsp?first=aaaa&second=bbb";  
//	        String strRequest = "request传值";  
//	        String strSession = "session传值";  
//	        request.setAttribute("strRequest", strRequest);  
//	        request.getSession().setAttribute("strSession", strSession);  
	    	List<String> results = MyTask.event_extraction(sent);
//	    	
//	    	System.out.println(extract_result);
	        request.setAttribute("words", results.get(0)); 
	        request.setAttribute("entitys", results.get(1));
	        request.setAttribute("events", results.get(2));
	        request.setAttribute("sentiment", results.get(3));
	        request.setAttribute("useTemplate", results.get(4));
	          
	        /** 
	         * 客户端跳转：效率低 
	         * session范围属性，url中的参数会传递下去，request范围属性不传递 
	         */  
	        //response.sendRedirect(url);  
	          
	        /** 
	         * 服务器端跳转：常用，效率高 
	         * request范围属性，session范围属性，url中的参数会传递 
	         */  
	        request.getRequestDispatcher(url).forward(request, response);  
	    }  
	      
	    /** 
	     * 销毁 
	     */  
	    public void destroy()  
	    {  
	        super.destroy();  
	    }  
	}



