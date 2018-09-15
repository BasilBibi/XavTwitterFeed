package com.bbb;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Client;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.processor.StringDelimitedProcessor;
import com.twitter.hbc.httpclient.auth.OAuth1;

/**
 * Connects to Twitter feed, registers some terms to track and follows some commentators.
 * 
 * Writes JSON format tweets to flat files.
 * 
 * Optionally takes a retweet filter datetime formatted same as filename or starts filtering retweets since startup
 * 
 * mvn exec:java -Dexec.mainClass="com.bbb.TweetAdapter" -DtrackTermFileName=pathToFile
 * 
 * @author basil
 */
public class TweetAdapter {
	
	private static final Logger logger = LoggerFactory.getLogger( TweetAdapter.class );
		
	private static long startFilter=-1;
	private static final String EMPTY_STRING = "";
	
	// tweet format
	// Thu Apr 11 07:44:47 +0000 2013
	private static DateFormat tweetDF;
	
	// Filename dateformat 20130410.1532324
	private static DateFormat fileNameDF;
	
	private static String outputFolderPath;
	
	private static String[] trackTerms = {"OPEC", "OPEP", "Shale Gas",	"Brent crude", "natgas", "oil"};
	
	//private static String userName;
	
	//private static String passWord;
	
	private boolean filter = true;
	
	private Map<String, Map<String,String>> mainTermWholeWords;
	
	private String trackTermFileName = "src/main/resources/TrackTerms.txt";
	
	private BlockingQueue<String> queue = new LinkedBlockingQueue<String>(10000);
	
	private Client client;
		
	static {
		
		tweetDF = new SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy");
		tweetDF.setTimeZone(TimeZone.getTimeZone("GMT"));
		
		fileNameDF = new SimpleDateFormat("yyyyMMdd.HHmmss");
		fileNameDF.setTimeZone(TimeZone.getTimeZone("GMT"));	
		
		String userHome = System.getProperty( "user.home" );		
		
		outputFolderPath = userHome+File.separator+"twitter";				
		
	}
	
	protected void init() {	

		// Read in the file containing track terms.
		readTrackTermFile();
		
		if( mainTermWords == null || 
				mainTermWords.length == 0 ){
			logger.error("Could not initialise. No terms or filters were defined.");
			System.exit(-1);
		}
		
		mainTermWholeWords = new HashMap<String, Map<String, String>>();
		for( String[] f1 : mainTermWords ){
			if( f1.length == 0 ){
				continue;
			}
			String mainTerm = f1[0];
			HashMap<String, String> words = new HashMap<String, String>();
			for( int i=1; i<f1.length;i++ ){
				words.put( f1[i] , f1[i] );
			}
			mainTermWholeWords.put(mainTerm, words);			
		}
		
		// Create an output folder if none exists
		File theDir = new File( outputFolderPath );

		// if the directory does not exist, create it
		if (!theDir.exists()) {
			System.out.println("creating directory: " + outputFolderPath);
			boolean result = theDir.mkdir();
			if (result) {
				System.out.println("DIR created");
			}
		}
	}
	
	/**
	 * Read the track terms from the file.
	 * Expected format :
	 * tweet term:filter1,filter2,filter3...
	 */
	protected void readTrackTermFile() {
		
		ArrayList<String> trackTermsFromFile = new ArrayList<String>();
		
		try {
			
			FileInputStream fstream = new FileInputStream(trackTermFileName);		
			DataInputStream in 		= new DataInputStream(fstream);
			BufferedReader  br 		= new BufferedReader(new InputStreamReader(in));
			
			String strLine = null;
			
			// Read File Line By Line
			while ((strLine = br.readLine()) != null) {
				// Print the content on the console
				strLine = strLine.trim();
				if( strLine.length() > 1 ){
					trackTermsFromFile.add( strLine );
				}
			}
			
			// Close the input stream
			in.close();
			
		} catch( FileNotFoundException fnfe ) {
			logger.error("Exception: " + fnfe.getMessage());
			System.exit(-1);
		} catch (Exception e) {// Catch exception if any
			logger.error("Error: " + e.getMessage());
			logger.error("Expected format\nOne term per line. e.g.\ntweet term:filter1,filter2,filter3...");
			System.exit(-1);
		} 
		
		if( trackTermsFromFile.size() > 0 ){
			for( String line : trackTermsFromFile ){
				setupTermAndFilter( line );
			}
		}
	}
	
	/**
	 * We are trying to populate the mainTermWords array for filtering.
	 * 
	 * Expected termLine as follows...
	 *  
	 * tweet term:filter1,filter2
	 * 
	 * e.g. as a default it looks like this
	 * 
	 * public String[][] mainTermWords = 
		{
			{"oil", 
				"olbas", "coconut", "massage", "palm", "olive", "cannabis",	"skin",  "nude", "pussy", "dressing", 
				"annoint", "anoint", "marinate", "flour", "onion", "hair", "painting", " vegan", 
				"fry", "fried", "fries", "sex", "slippery", "rope", "wank",	"voyeur", "chick", "cook", 
				"stain", "salad", "tanning", "sunscreen", "ass", "lotion", "vegetable", "trippy", "stoned",
				"groundnut", "ghee", "incense", "samosa", "chapati"},
		};
	 * @param termAndFilter
	 */
	protected void setupTermAndFilter( String termLine ) {
		
		if( termLine                     == null	|| 
			termLine.trim().indexOf("#") == 0 		||
			termLine.trim().length()     == 0 ){
			return;
		}
		
		/*
		if( termLine.indexOf(":") != -1 ){
		
			String[] termAndFilterTest = termLine.split(":");
			if( termAndFilterTest.length!=2 ){
				throw new Exception("Incorrect format for tweet term and filters "+termLine);
			}
			
			String[] filterTest = termAndFilterTest[1].split(",");
			if( filterTest.length == 0 ){
				throw new Exception("Incorrect format for tweet term and filters "+termLine);
			}
		
		}
		*/
		
		String[] filter = termLine.split("[:,]");
		
		// increase the size of mainTermWords to hold another line of filters
		String[][] mainTermWordsTemp = new String[mainTermWords==null?1:mainTermWords.length+1][];
		if( mainTermWords!=null ){
			for (int i=0; i<mainTermWords.length; i++) {
				mainTermWordsTemp[i]=mainTermWords[i];
			}
		}
		
		mainTermWordsTemp[mainTermWordsTemp.length-1] = filter;
		
		mainTermWords = mainTermWordsTemp;
		
	}

	
	public static void main(String[] args) {

        parseArgs(args);

        TweetAdapter  tweetAdapter = new TweetAdapter();

        initialiseAndRunTweetAdapter(tweetAdapter);
		
	}

    private static void initialiseAndRunTweetAdapter(TweetAdapter tweetAdapter) {
        tweetAdapter.trackTermFileName = System.getProperty("trackTermFileName");
        if( tweetAdapter.trackTermFileName == null ){
            String workingDir = System.getProperty( "user.dir" );
            tweetAdapter.trackTermFileName = workingDir+ File.separator+"TrackTerms.txt";
        }

        logger.info("Using trackTermFileName [{}]", tweetAdapter.trackTermFileName);

        tweetAdapter.init();

        try{
            tweetAdapter.connect();
            tweetAdapter.processTweets();
        } catch ( Exception e ){
            e.printStackTrace();
        } finally {
            tweetAdapter.close();
        }
    }

    private static void parseArgs(String[] args) {
        if( args.length >= 2 ){

            //userName = args[0];
            //passWord = args[1];

            if( args.length == 3){
                try {
                    Date d = fileNameDF.parse(args[2]);
                    System.out.println("Retweet Filter:"+tweetDF.format(d));
                    startFilter = d.getTime();
                } catch (Exception e) {
                    e.printStackTrace();
                    System.exit(-1);
                }
            }
        } else {
            Date d = new Date();
            System.out.println("Retweet Filter:"+tweetDF.format(d) );
            startFilter = d.getTime();
        }
    }

    public void connect() throws Exception {
			
		StatusesFilterEndpoint endpoint = new StatusesFilterEndpoint();
		
		// add some track terms
		endpoint.trackTerms( Lists.newArrayList(trackTerms) );
		
		// People we are following...
		// The Economist (@theeconomist), the New York Times (@nytimes), and the Wall Street Journal (@wsj), @forexLive,@CrudeOilPrices
		endpoint.followings( Lists.newArrayList(follows) );
		
		//if( logger.isDebugEnabled() )
		//	logger.debug("userName[{}] auth[{}]", userName, passWord );		
		OAuth1 auth = new OAuth1( 	"R1v21NAyrsCdKaVw8aIA", 
									"ZxsgJV2GBDCtKaAxId6vIGO9gKregZPaeXYtP4AwzCU", 
									"1176837962-MFKqcmXjSNbTkOon6ApfMcO8OL0w37HrPWtPLfC",
									"VedZJ5B1Dy5pvNmUMooBwJHnZ0etJ4eEJzqhLymVM" );

		// Create a new BasicClient. By default gzip is enabled.
		client = new ClientBuilder()
						.name("BasicTweetFeed")
						.hosts(Constants.STREAM_HOST)
						.endpoint(endpoint)
						.authentication(auth)
						.processor(new StringDelimitedProcessor(queue)).build();

		logger.info("Connecting...");

		// Establish a connection
		client.connect();
		logger.info("Connected");
		
	}

	private void processTweets() {
		
		FileWriter fw = null;
		BufferedWriter bw = null;

		try {

			boolean madeFile = false;
			
			while (true) {
				
				for (int msgNumberRead = 0; msgNumberRead < 10000; msgNumberRead++) {
					
					if( !madeFile ){
						bw = new BufferedWriter( makeOutputFileAndWriter() );
						madeFile=true;
					}

					if( logger.isDebugEnabled() )
						logger.debug("Waiting for tweet...");

                    // Get the next tweet from the queue.
					String jsonMsg = queue.take();
					
					try{

                        // Turn the tweet into a JSONObject
						JSONObject json = (JSONObject) JSONSerializer.toJSON(jsonMsg);
						
						// This is the text from the tweet
						String text = json.getString("text");
						
						if( text==null || text.isEmpty()){
                            continue;
                        }

						text = text.toLowerCase();

						// Get the retweet timestamp 
						String retweetTs = filterRetweet( json );
						
						if( retweetTs != EMPTY_STRING ){
							System.out.println("*** "+retweetTs+":"+text);
							continue;
						}

                        if (filterTweet(text)) continue;

                        printConsoleLine( msgNumberRead, json, retweetTs);

                        // Write the whole tweet to the file
						bw.write(jsonMsg);
						bw.flush();
						
					} catch( net.sf.json.JSONException e ){
						e.printStackTrace();						
					} 
					
				}
				
				madeFile = false;
				
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {

            // Clean up tidily
			if (fw != null)
				try {
					fw.close();
				} catch (Exception e2) {
                    System.out.println(e2.getMessage());
                    e2.printStackTrace();
                }

			if (bw != null)
				try {
					bw.close();
				} catch (Exception e2) {
                    System.out.println(e2.getMessage());
                    e2.printStackTrace();
                }
		}
	}

    private boolean filterTweet(String text) {
        if( filter ) {

            String filterFound 	= filterOnMainTermWords( mainTermWords, text );

            /*
            if( filterFound==null )
                filterFound 		= filterOnMainTermFragments( mainTermFragments, text );
            */

            if( filterFound==null )
                filterFound 		= filterOnWholeWords( fragments, text );

            if( filterFound==null )
                filterFound 		= filterOnFragments( fragments, text );

            if( filterFound==null )
                filterFound 		= filterOnWholeWords( racist, text );

            if( filterFound!= null ) {
                System.out.println("*** filtered:"+filterFound+":"+text);
                return true;
            }

        }
        return false;
    }

    private FileWriter makeOutputFileAndWriter() throws IOException {
		FileWriter fw;
		String fileDate = fileNameDF.format(new Date());
		File file = new File(outputFolderPath + File.separator+"tweets." + fileDate	+ ".json");
		if (!file.exists()) {
			file.createNewFile();
		}

		fw = new FileWriter(file.getAbsoluteFile());
		return fw;
	}
	
	public void close() {
		if( client != null )
			client.stop();
		
	}
	
	/**
	 * Uses the main term contextualised array to drive filtering.
	 * 
	 *      main 
	 *      term
	 *      
	 * e.g. {oil, abc, def ...
	 *       ---
	 * 
	 * will filter abc or def if exists with oil
	 * 
	 * @param text
	 * @return
	 */
	protected String filterOnMainTermWords( String[][] mainTermWordArr, String text ){
		
		if( text==null || text.length()==0 )
			return null;
		
		String 		delims 		= "[ ,;:\\)\\(]+";
		String[] 	textWords 	= text.split(delims);
		
		String		foundAssociatedWord = null;
		
		// First see if the main term is in text		
		for( String mainWd : mainTermWholeWords.keySet() ){
			if( text.indexOf(mainWd) != -1 ){
				// Get the associated Words
				Map<String, String> associatedWords = mainTermWholeWords.get(mainWd);
				// iterate textWords to see if they are in the associatedWord map 
				for( String textWord : textWords ){
					foundAssociatedWord =  associatedWords.get( textWord );
					if( foundAssociatedWord != null )
						return foundAssociatedWord;
				}
			}				
		}
				
		return null;
	}

	
	/**
	 * Uses the main term contextualised array to drive filtering any fragments or sequences.
	 * 
	 *      main 
	 *      term
	 *      
	 * e.g. {oil, "abc on air", def ...
	 *       ---
	 * 
	 * will filter "abc on air", "abc on airline" if exists with oil
	 * 
	 * @param text
	 * @return
	 */
	protected String filterOnMainTermFragments( String[][] mainTermFrags,  String text ){
		for( String[] f1 : mainTermFrags ){
			String mainTerm = f1[0];
			if( text.contains(mainTerm) ) {
				for( int i=1; i<f1.length;i++ ){
					if( text.contains(f1[i])){
						return f1[i];
					}
				}
			}
		}
		return null;
	}
	
	/**
	 * Filters if any of the words in the  are found in text.
	 * 
	 * @param text
	 * @return
	 */
	protected String filterOnWholeWords( String[] wholeWords, String text ){
		String 		delims = "[ ]+";
		String[] 	tokens = text.split(delims);		
		Map<String, String> textMap = new Hashtable<String,String>(tokens.length);
		for( String token : tokens ){
			textMap.put(token, token);
		}
		for( String word : wholeWords ){
			if( textMap.containsKey( word ) ){
				return word;
			}
		}
		return null;
	}
	
	/**
	 * Filters if any of the words are found in text.
	 * 
	 * @param text
	 * @return
	 */
	protected String filterOnFragments( String[] fragments, String text ){		
		for( String fragment : fragments ){
			if( text.contains( fragment ) ){
				return fragment;
			}
		}
		return null;
	}

	/**
	 * Checks the retweeted_status object to see if the retweet timestamp is > tweet cut off.
	 * 
	 * tweet cut off is set by commandline input or just the start of this process instance.
	 * 
	 * @param json
	 * @return
	 */
	private String filterRetweet( JSONObject json ){
		String 		retweetTs = EMPTY_STRING;
		try{
			JSONObject retweeted_status = json.getJSONObject("retweeted_status");
			if( retweeted_status!=null ){
				retweetTs = retweeted_status.getString("created_at");
			}
		} catch( Exception e ){	
		}
		
		// test for null again because "created_at" could be null
		if( retweetTs!= null && !retweetTs.equals("")){
			try {
				long tweetTime = tweetDF.parse(retweetTs).getTime();
				if( tweetTime>startFilter ){
					return "retweet:"+retweetTs;
				} else {
					retweetTs = EMPTY_STRING;
				}
			} catch (Exception e) {								
			}			
		}
		return retweetTs;
	}
	
	private void printConsoleLine( int msgNumber, JSONObject json, String retweetTs ) throws Exception {
		JSONObject user = json.getJSONObject("user");
		System.out.println("" + msgNumber + 
				"|"						+ json.get("created_at") +
				//",retweeted="			+ json.get("retweeted") + 
				"|"						+ user.get("name") +
				"|"						+ user.get("name") +
				"|"						+ user.get("location") +
				"|"						+ user.get("followers_count") + 
				"|"						+ user.get("friends_count") +
				//",user.description="	+ user.getString("description").replaceAll("\n", "") + 
				"|"						+ json.getString("text").replaceAll("\n", "") +
				"|"						+retweetTs+
				"");
	}
		
	public void setMainTermWords(String[][] mainTermWords) {
		this.mainTermWords = mainTermWords;
	}

	public void setMainTermFragments(String[][] mainTermFragments) {
		this.mainTermFragments = mainTermFragments;
	}
	
	public void setFragments(String[] fragments) {
		this.fragments = fragments;
	}

	// Expr 0 is used in context with the rest e.g. if text contains filterStrings[][0] then check the rest...
	// Contextualised absolute strings	
	// Exact matches	
	public String[][] mainTermWords;/* = 
		{
			{"oil", 
				"olbas", "coconut", "massage", "palm", "olive", "cannabis",	"skin",  "nude", "pussy", "dressing", 
				"annoint", "anoint", "marinate", "flour", "onion", "hair", "painting", " vegan", 
				"fry", "fried", "fries", "sex", "slippery", "rope", "wank",	"voyeur", "chick", "cook", 
				"stain", "salad", "tanning", "sunscreen", "ass", "lotion", "vegetable", "trippy", "stoned",
				"groundnut", "ghee", "incense", "samosa", "chapati"},
		};*/
	
	// Contextualised string fragments exist e.g. "orangut" will filter : orangutang orangutangs
	public String[][] mainTermFragments = 
		{
			{"oil", 
				"cod liver", "cod-liver", "baby oil", "masturbat", "prayer oil", "essential oil",
				"holy oil", "orangut", "auto repair", "get my oil changed", "holy oil", "oil print",
				"oil paint", "cooking oil", "fish oil", "castor oil" }
		};
	
	// partials
	// fuck : fuck, fucks, fucker, fucking, fucked
	public String[] fragments = 
		{ "shit", "shite", "piss", "fuck", "cunt", "cocksucker", "motherfucker", "tits", "penis", "arse" };
	
	// whole words
	public String[] racist = 
		{ "nigger", "nigga", "raghead" };
	
	// Who am I following : get all tweets 
	public final Long[] follows = 
		{ 	
			//http://www.twibes.com/finance/twitter-list
			16827489L, 	//CBOE
			7985672L,	//sorenmacbeth
			14886375L,	//StockTwits
			16429977L,	//dantanner
			53796756L,	//TheNicheReport
			14502632L,	//techstartups
			16465409L,	//taxtherapy
			28764153L,	//eisenhofer
			609783664L,	//webdesigniinc
			16908796L,	//TS_Elliott
			230459023L,	//MrAndreRabie
			29748372L,	//stockguy22
			103783202L,	//stock_tweets
			13796572L,	//mint
			17894233L,	//SamJones_71
			24349486L,	//currencynews
			33370627L,	//BudgetDude
			15897179L,	//BreakoutStocks
			24823295L,	//BankFreedom	
			19399038L,	//forexLive
			23760398L,	//CrudeOilPrices
			5988062L,	//theeconomist
			807095L,	//nytimes
			3108351L,	//wsj
			
			//http://www.businessinsider.com/the-best-finance-people-on-twitter-2012-4?op=1
			15485461L,	//abnormalreturns
			348345136L,	//chrisadamsmkts
			327577091L,	//justinwolfers
			27707080L,	//PIMCO
			22522178L,	//ReformedBroker
			68739089L,	//CGasparino
			1797991L,	//carney
			135575282L,	//morningmoneyben
			251379913L,	//neilbarofsky
			217284148L,	//Austan_Goolsbee
			24002724L	//TonyFratto
						
		};
}
