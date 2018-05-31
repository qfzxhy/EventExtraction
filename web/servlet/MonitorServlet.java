

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class NewsServlet
 */
public class MonitorServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public MonitorServlet() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
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
    	String content = request.getParameter("content");
    	System.out.println(content);
    	String url = "index3.jsp?first=aaaa&second=bbb";  
//        String strRequest = "request传值";  
//        String strSession = "session传值";  
//        request.setAttribute("strRequest", strRequest);  
//        request.getSession().setAttribute("strSession", strSession);  
    	String imgPath = MyTask.load_wordcloud_fig(content);
//    	
//    	System.out.println(extract_result);
        request.setAttribute("imgPath", imgPath); 
        
////        FileInputStream inputStream = new FileInputStream("G:\\MasterTwoU\\28proj\\Test\\WebContent\\cloud.jpg");
////		int i = inputStream.available();
////		//byte数组用于存放图片字节数据
////		byte[] buff = new byte[i];
////		inputStream.read(buff);
////		//记得关闭输入流
////		inputStream.close();
////		//设置发送到客户端的响应内容类型
////		response.setContentType("image/*");
////		OutputStream out = response.getOutputStream();
////		out.write(buff);
////		//关闭响应输出流
//		out.close();
          
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