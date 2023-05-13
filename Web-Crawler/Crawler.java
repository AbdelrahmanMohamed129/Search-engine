import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.*;
import java.util.ArrayList;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

//missing check robot.txt
//missing input threads from user

// Crawler class that implements runnable to allow threading
public class Crawler implements Runnable{

	//Thread data members
	private Thread thread;
	private int thread_id;

	// First link to be Crawled
	private String first_link;

	// Array of strings holding the already visited links
	public static ArrayList<String> visited_links;

	// Maximum amount of links to be Crawled and put in output file
	private final int max_links = 36000;

	// Maximum depth that could be reached in each sub-link
	private static final int max_depth = 5;
	private static int count = 0;


	// Constructor takes first link, thread id and array of strings(visitedlinks)
	public Crawler(String firstlink, int threadid, ArrayList<String> list) {
		first_link = firstlink;
		thread_id = threadid;
		visited_links = list;
		
		thread = new Thread(this);
		thread.start();
	}
	
	   
	// Crawl recursive function takes level and url and Crawls in depth
	private void Crawl(int level, String url) throws URISyntaxException {
		// If depth level is still less than max_depth allowed
		if(level <= max_depth) {
			// Get/Request Url
			Document document= GetUrl(url);
			// if not an empty document
			if(document!= null) {
				// Loop around in depth links
				for(Element link : document.select("a[href]")) {
					// Find the next link in depth (extract its URL)
					String linkUrl = link.absUrl("href");
					// in case of a link that does not exist in the visited list Crawl the link
					String next_link = NormalizeUrl(linkUrl);
					try {
						// Making sure befor crawling the next link that it is not in the visited_links before
						// and that it is not in the output file as well so that even if the crawler is interrupted it doesn't revisit visited links
						if(!visited_links.contains(next_link) && !Files.lines(Paths.get("output.txt")).anyMatch(line -> line.contains(next_link))) {
							Crawl(level++, next_link);
						}
					} catch (IOException e) {
					}
				}
			}
		}
	}
	
	private Document GetUrl(String url) throws URISyntaxException {
		try {
			// Initiate connection
			Connection connection = Jsoup.connect(url);

			// Get and parse the result
			Document document= connection.get();
			
			// Connection success and check document type is html
			if(connection.response().statusCode() == 200 && connection.response().contentType().contains("text/html")) {

				// add link if size < max_links
				if (Crawler.visited_links.size() <= this.max_links) {		
					synchronized(Crawler.visited_links) {

						PrintFile("\nThread with id " + thread_id + " received the url " + url);
						if(!visited_links.contains(url)){
						// Add the recieved url to the visited links list 
						visited_links.add(url);
						
						// Download the url and turn it to string then output the url in the output file
						String StringUrl = url.toString();
						Download(StringUrl);
						writeLinks(StringUrl);

						// Debug live printing of links size
						PrintFile("Link Number "+Integer.toString(visited_links.size()));
						return document;
						}
					}
				}

			}
			
			return null;
		}
		catch(IOException e) {
			return null;
		}
	}
	
	// If the URL is an absolute URL, the function extracts the path component of the URL using the getPath method of the URI class. It then replaces any occurrences of consecutive slashes (//) in the path with a single slash (/). If the path ends with a slash, that slash is removed.
	// Finally, the function constructs a new URI object using the scheme, user info, host, port, path, query, and fragment components of the original URI, with any repeated content removed. The function returns the string representation of this new URI.
	//  NormalizeUrl function that takes a String Url as input and returns a modified version of the url, with any repeated content removed.
	public String NormalizeUrl(String Url) throws URISyntaxException {

		// Checking if the input URL is null. If it is, the function returns null
		if (Url == null) {
			return null;
		}
		
		// CRemove questionmark 
		while(Url.contains("?")){
			int index = Url.indexOf('?');
			Url = Url.substring(0, index);
		}

		// Remove hash
		while(Url.contains("#")){
			int index2 = Url.indexOf("#");
			Url = Url.substring(0, index2);
		}

		// Remove any login pages as i get alot of them and they are useless (cannot extract useful data from them)
		if (Url.contains("/login") ) {
			Url = Url.substring(0, Url.length() - "/login".length());
		}
		if (Url.contains("/users") ) {
			Url = Url.substring(0, Url.length() - "/users".length());
		}
		if (Url.contains("Login") ) {
			Url = Url.substring(0, Url.length() - "Login".length());
		}

		// Remove any signup pages as i get alot of them and they are useless (cannot extract useful data from them)
		if (Url.contains("/signup") ) {
			Url = Url.substring(0, Url.length() - "/signup".length());
		}
		if (Url.contains("Signup") ) {
			Url = Url.substring(0, Url.length() - "Signup".length());
		}

		if (Url.contains("custhelp") ) {
			Url = Url.substring(0, Url.length() - "custhelp".length());
		}

		if (Url.contains("/landing") ) {
			Url = Url.substring(0, Url.length() - "/landing".length());
		}
		if (Url.contains("facebook") ) {
			Url = "www.facebook.com";
		}
		if (Url.contains("apps.apple.com") ) {
			Url = "https://apps.apple.com";
		}
		
		// Checking if the url is absolute (i.o.w. it includes a scheme like http or https)
		URI absoluteUriChecker = new URI(Url);
		if (absoluteUriChecker.isAbsolute() == false) {
			throw new URISyntaxException(Url, "Url is not absolute");
		}
	
		String webPagePath = absoluteUriChecker.getPath();
		
		// Extra checks to remove unwanted things from url
		if (webPagePath != null) {
			// Replace any amount of repetative slash with one slash
			webPagePath = webPagePath.replaceAll("//*/", "/");
			// Remove slash at the end of url
			if (webPagePath.length() > 0 && webPagePath.charAt(webPagePath.length() - 1) == '/') {
				webPagePath = webPagePath.substring(0, webPagePath.length() - 1);
			}
		}
	
		String resultUri = new URI(absoluteUriChecker.getScheme(), absoluteUriChecker.getUserInfo(), absoluteUriChecker.getHost(), absoluteUriChecker.getPort(), webPagePath, absoluteUriChecker.getQuery(), absoluteUriChecker.getFragment()).toString();
	
		return resultUri;
	}
	

	public void Download(String webPageUrl) {
		try {
			
			// Create URL object
            URL url = new URL(webPageUrl);
            BufferedReader readr = 
              new BufferedReader(new InputStreamReader(url.openStream()));
            
            // Enter filename in which you want to download
            BufferedWriter writer = 
              new BufferedWriter(new FileWriter("downloads/webpage" + Crawler.count + ".html"));
			 // increase the count
			  Crawler.count += 1;
            
            // read each line from stream till end
            String line;
            while ((line = readr.readLine()) != null) {
                writer.write(line);
            }
  
            readr.close();
            writer.close();
            PrintFile("Success downloading");
		}
		// Exceptions
        catch (MalformedURLException mue) {
            PrintFile("issue with url");
        }
        catch (IOException ie) {
            PrintFile("IOException");
        }
	}


	public void writeLinks(String Url) {
		PrintWriter PrintWriter = null;
		try {
			FileWriter writer = new FileWriter("output.txt", true);
			BufferedWriter bufferwriter = new BufferedWriter(writer);
			PrintWriter = new PrintWriter(bufferwriter);
			PrintWriter.println(Url);
			PrintFile("Successfully retrieved");
			PrintWriter.flush();
		} catch (IOException e) {
			PrintFile("error");
			e.printStackTrace();
		} finally {
			if (PrintWriter != null) {
				PrintWriter.close();
			}
		}
	}	
	
	// Function PrintFile writes in printFile.txt file
	private static void PrintFile(String message) {
		try {
			FileWriter writer = new FileWriter("printFile.txt", true);
			writer.write(message + "\n");
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public Thread getThread() {
		return thread;
	}
	
	@Override
	public void run() {
		// TO DO Auto-generated method stub
		try {
			Crawl(1, first_link);
		} catch (URISyntaxException e) {
			String errorMsg = "error " + e.getMessage();
		    PrintFile(errorMsg);
			try {
				e.printStackTrace(new PrintWriter(new FileWriter("log.txt", true)));
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}
	
	public static void main(String[] args) throws IOException {
		
		// list contains all the Crawlers threads
		ArrayList<Crawler> bots = new ArrayList<Crawler>();
		
		// list will contain all the Crawled urls
		ArrayList<String> list = new ArrayList<String>();
		
		// list will contain all the seeds from seed list file
		ArrayList<String> seeds = new ArrayList<String>();
		
		// read seedList file
		File file = new File("seedList.txt"); //creates a new file instance 

		//reads the file 
		FileReader fr = new FileReader(file);
		
		//creates a buffering character input stream  
		BufferedReader br=new BufferedReader(fr);  
		
		String line;
		while((line=br.readLine())!=null)   {
			seeds.add(line);
		}
		
		//closes the stream and release the resources 
		fr.close(); 
		
		PrintFile("Seed Urls:");
		
		int seedCounter = 1;
		for (String link : seeds) {
			PrintFile(link);
			bots.add(new Crawler(link, seedCounter, list));
			seedCounter += 1;
		}
			
		for(Crawler c : bots) {
			try {
				c.getThread().join();
			}
			catch(InterruptedException e) {
				e.printStackTrace();
			}
		}
				
		for(String s : list) {
			PrintFile(s);
		}
		
	}

}