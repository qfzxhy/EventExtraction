
import java.io.*;
import java.util.List;

import org.python.core.PyFunction;
import org.python.core.PyObject;
import org.python.util.PythonInterpreter;

class PyCaller {
    private static final String DATA_SWAP = "event.txt";
    private static final String PY_URL = System.getProperty("user.dir") + "\\test.py";

    public static void writeResult(List<String> events) throws IOException {
//    	System.out.println(events);
//    	StringBuilder sb = new StringBuilder();
//		for(String event: events)
//		{
//			String[] units = event.split(",");
//			for(int i = 0; i < units.length-1;i++)
//			{
//				if(units[i].length() > 0)
//				{
//					sb.append(units[i] + ",");
//				}
//			}
//		}
//		System.out.println(sb.toString());
//		PrintWriter pw = new PrintWriter(new FileOutputStream("events.txt"));
//		//用文本格式打印整数writestr
//		pw.println(sb.toString());
//		//清除printwriter对象
//		pw.close();
//		System.out.println("end!");
    }

    public static String readAnswer() {
        BufferedReader br;
        String answer = null;
        try {
            br = new BufferedReader(new FileReader(new File(DATA_SWAP)));
            answer = br.readLine();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return answer;
    }

    public static void execPy(String content) {
    	System.out.println("begin!");
    	Process proc = null;
        try {
            proc = Runtime.getRuntime().exec("python G:\\MasterTwoU\\28proj\\Test\\test.py "+content+" G:\\MasterTwoU\\28proj\\Test\\WebContent\\cloud.jpg");
            proc.waitFor();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("end!");
//        
//    	PythonInterpreter interpreter = new PythonInterpreter();
//    	interpreter.execfile("test.py");  
////
//        PyFunction pyFunction = interpreter.get("hello", PyFunction.class); // 第一个参数为期望获得的函数（变量）的名字，第二个参数为期望返回的对象类型
//        PyObject pyObject = pyFunction.__call__(); // 调用函数
////
//        System.out.println(pyObject);
    	
    }

    public static void main(String[] args) {
//        writeImagePath("src/test.jpg");
//        execPy();
//        System.out.println(readAnswer());
    }
}