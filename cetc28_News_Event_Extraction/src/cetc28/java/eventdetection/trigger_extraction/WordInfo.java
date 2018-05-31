
package cetc28.java.eventdetection.trigger_extraction;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * 触发词上下文特征类
 * @author qf
 *
 */
public class WordInfo {
	public String word;//词
	public String lexical;//词性
	public String syntactic;//0,1
	public String semantic;//语义
	public String relation;//依存关系
	public void setWord(String word)
	{
		this.word = word;
	}
	public void setLexical(String lexical)
	{
		this.lexical = lexical;
	}
	public void setSyntactic(String syntactic)
	{
		this.syntactic = syntactic;
	}
	public void setSemantic(String semantic)
	{
		this.semantic = semantic;
	}
	public void setRelation(String relation)
	{
		this.relation = relation;
	}
}
