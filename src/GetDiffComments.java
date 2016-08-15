import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.jsoup.nodes.Element;

public class GetDiffComments {

	// public static void main(String[] args) {
	// getComment("/tmp/nag/test.doc");
	// }

	public static HashMap<String, String> getComment(String url) {

		Document doc = null;
		Elements table = null;

		try {
			doc = Jsoup.parse(new File(url), "UTF-8");
		} catch (IOException e) {
			e.printStackTrace();
		}
		table = doc.select("table");

		HashMap<String, String> ret = new HashMap<String, String>();
		Elements rows = table.select("tr");

		for (int i = 0; i < rows.size(); i++) {
			Element row = rows.get(i);
			Elements data = row.select("td");

			String comment = "";
			String host = "";
			String service = "";
			for (int j = 0; (j < data.size() && j < 3); j++) {
				String dataString = data.get(j).toString().replaceAll("<[^>]*>|&nbsp;|\n", "");
				switch (j) {
					case 0:
						comment = dataString;
						break;
					case 1:
						host = dataString.replaceAll("--BL--", "").trim();
						break;
					case 2:
						service = dataString.replaceAll("--BL--", "").trim();
				}
			}
//			System.out.println(comment + "\t" + host + "\t" + service);
			ret.put(host + "\t" + service, comment);
		}
		return ret;
	}

}
