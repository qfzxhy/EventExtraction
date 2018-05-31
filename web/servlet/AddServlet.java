

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cetc28.java.config.FileConfig;

/**
 * Servlet implementation class Add_temp
 */
public class AddServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public AddServlet() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.setHeader("content-type", "text/html;charset=UTF-8");  
    	response.setCharacterEncoding("UTF-8");  
    	request.setCharacterEncoding("UTF-8");  
    	String template = request.getParameter("template");
    	System.out.println(template);
    	String type = request.getParameter("type");
    	String url = "index4.jsp?first=aaaa&second=bbb";  
    	String dir = FileConfig.getRulePath();
    	int e_t = -1;
    	
		
		File file = new File(dir);
		File[] fs = file.listFiles();
		int et = -1;
		int i = 0;
		for(File f : fs)
		{
			String filename = f.getName();
			
			if(filename.substring(2).equals(type+".txt"))
			{
				et = i;
				break;
			}
			i++;
		}
		if(et == -1)
		{
			request.setAttribute("message", "重新输入事件类别"); 
		}else
		{
			BufferedWriter bw = null;
			try {
				bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fs[et], true)));
				bw.write(template+"\n");
				
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				bw.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			request.setAttribute("message", "添加成功");
		}
		
    
        
          
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

}
