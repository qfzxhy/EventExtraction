

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class ImageAction
 */
public class ImageAction extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ImageAction() {
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
		FileInputStream inputStream = new FileInputStream("G:\\MasterTwoU\\28proj\\Test\\WebContent\\cloud.jpg");
		int i = inputStream.available();
		//byte数组用于存放图片字节数据
		byte[] buff = new byte[i];
		inputStream.read(buff);
		//记得关闭输入流
		inputStream.close();
		//设置发送到客户端的响应内容类型
		response.setContentType("image/*");
		OutputStream out = response.getOutputStream();
		out.write(buff);
		//关闭响应输出流
		out.close();

	}

}
