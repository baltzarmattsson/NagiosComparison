package baltzar;

import java.io.File;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class tester {

	public static void main(String[] args) {

		Document doc;
		Elements table;
		try {
			
			Runtime.getRuntime().exec("curl --user asd:asd https://confluence.cybercom.com/exportword?pageId=48209859 -o /tmp/nag/firstdiff.html");
			Thread.sleep(2000);
			
			doc = Jsoup.parse(new File("/tmp/nag/firstdiff.html"), "UTF-8");
			table = doc.select("table");

			for (Element row : table.select("tr")) {
				Elements data = row.select("td");
				if (data.toString().contains("mkn")) {
					System.out.println(data.toString());
					System.out.println("------------------");
				}
				
				
//				if (row.toString().contains("mkn")) {
//					System.out.println(row.toString().replaceAll("<[^>]*>|&nbsp;|\n", ""));
//					System.out.println("------------------------------");
//				}
			}
		} catch (Exception e) {

		}

	}

}
