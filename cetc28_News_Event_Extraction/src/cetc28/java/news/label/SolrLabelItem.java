package cetc28.java.news.label;



public class SolrLabelItem {
	public String event_id = null;
	public String news_title = null;
	public String news_url = null;
	public String img_address = null;
	public String news_content = null;
	public String saveTime = null;
	public String dataSource = null;

	public SolrLabelItem(String event_id, 
									String news_title, 
									String news_url, 
									String img_address,
									String news_content,
									 String saveTime,
									 String dataSource) {
		// TODO Auto-generated constructor stub
		this.event_id = event_id;
		this.news_title = news_title;
		this.news_url = news_url;
		this.img_address = img_address;
		this.news_content = news_content;
		this.saveTime = saveTime;
		this.dataSource = dataSource;
	}
	public void show()
	{
		System.out.println("event_id: "+event_id+"\n"+
									"news_title: "+news_title+"\n"+
									"news_url: "+news_url+"\n"+
									"img_address: "+img_address+"\n"+
									"news_content: "+news_content+"\n"+
									"saveTime: "+saveTime+"\n"+
									"dataSource:"+dataSource+"\n");
	}
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
