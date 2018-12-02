
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Scanner;

public class Input {

	private Scanner scanner;

	public Input(String name) {
		System.out.println("NAME>>"+name);
		if (name == null)
			throw new IllegalArgumentException("argument is null");
		try {
			// first try to read file from local file system
			File file = new File(name);
			if (file.exists()) {
				FileInputStream fis = new FileInputStream(file);
				scanner = new Scanner(new BufferedInputStream(fis));
				return;
			}

			// files included in jar
			URL url = getClass().getResource(name);

			if (url == null) {
				try {
					url = getClass().getClassLoader().getResource(name);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			URLConnection site = url.openConnection();
			InputStream is = site.getInputStream();
			scanner = new Scanner(new BufferedInputStream(is));
		} catch (IOException ioe) {
			throw new IllegalArgumentException("Could not open " + name, ioe);
		}
	}

	public String[] read() {
		ArrayList<String> arrayList = new ArrayList<>();
		while (scanner.hasNext()) {
			arrayList.add(scanner.nextLine());
		}
		return arrayList.toArray(new String[arrayList.size()]);

	}

}
