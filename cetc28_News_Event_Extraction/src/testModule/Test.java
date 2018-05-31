package testModule;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import jdk.jfr.events.FileWriteEvent;
import cetc28.java.config.FileConfig;

public class Test {
	public static void main(String[] args) {
		String type = "公开声明";
		String dir = FileConfig.getRulePath();
		File file = new File(dir);
		File[] fs = file.listFiles();
		int et = 1;
		int i = 0;
		for(File f : fs)
		{
			String filename = f.getName();
			if(filename.indexOf(filename)!=-1)
			{
				et = i;
				break;
			}
			i++;
		}
		
		BufferedWriter bw = null;
		try {
			bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fs[et], true)));
			bw.write("test上述"+"\n");
			
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
	}
}
