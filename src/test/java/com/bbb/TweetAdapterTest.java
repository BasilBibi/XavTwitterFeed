package com.bbb;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class TweetAdapterTest {
	
	@Test
	public void testNoMainWord() {				
		
		String[][] mainTermWords = 
			{ 
				{"oil", "filterMeOut"}			
			};
		
		TweetAdapter tweetAdapter = new TweetAdapter();
		tweetAdapter.setMainTermWords(mainTermWords);
		tweetAdapter.init();
		
		assertNull( "",
					tweetAdapter.filterOnMainTermWords( mainTermWords, 
													"No main word exists and all my words are fine.") );		
	}
	
	@Test
	public void testNoMainWordMulti() {				
		
		String[][] mainTermWords = 
			{ 
				{"oil",   "filterMeOut"},
				{"coal",  "mining"},
				{"wheat", "shredded"},
			};
		
		TweetAdapter tweetAdapter = new TweetAdapter();
		tweetAdapter.setMainTermWords(mainTermWords);
		tweetAdapter.init();
		
		assertNull( 
				tweetAdapter.filterOnMainTermWords( mainTermWords, 
													"No main word exists and all my words are fine.") );		
	}	
	
	@Test
	public void testFilterMainWordAndAllOk() {				
		
		String[][] mainTermWords = 
			{ 
				{"oil", "filterMeOut"}			
			};
		
		TweetAdapter tweetAdapter = new TweetAdapter();
		tweetAdapter.setMainTermWords(mainTermWords);
		tweetAdapter.init();
		
		assertNull( 
				tweetAdapter.filterOnMainTermWords( mainTermWords, 
													"Main word oil exists and all my words are fine.") );		
	}
	
	@Test
	public void testFilterMainWordMultiAndAllOk() {				
		
		String[][] mainTermWords = 
			{ 
				{"oil",   "filterMeOut"},
				{"coal",  "mining"},
				{"wheat", "shredded"},
			};
		
		TweetAdapter tweetAdapter = new TweetAdapter();
		tweetAdapter.setMainTermWords(mainTermWords);
		tweetAdapter.init();
		
		assertNull( 
				tweetAdapter.filterOnMainTermWords( mainTermWords, 
													"Main word oil exists and all my words are fine.") );		
	}
	
	@Test
	public void testFilterMainWordMultiCrossOverAndAllOk() {				
		
		String[][] mainTermWords = 
			{ 
				{"oil",   "filterMeOut"},
				{"coal",  "mining"},
				{"wheat", "shredded"},
			};
		
		TweetAdapter tweetAdapter = new TweetAdapter();
		tweetAdapter.setMainTermWords(mainTermWords);
		tweetAdapter.init();
		
		assertNull( 
				tweetAdapter.filterOnMainTermWords( mainTermWords, 
													"Main word oil exists and all my words are fine for mining.") );		
	}
	
	@Test
	@Ignore
	public void testFilterFoundOnMainTermWords() {
		
		String[][] mainTermWords = 
			{ 
				{"oil", "bad"}			
			};
		
		TweetAdapter tweetAdapter = new TweetAdapter();		
		tweetAdapter.setMainTermWords(mainTermWords);
		tweetAdapter.init();
		
		assertNotNull( 
				tweetAdapter.filterOnMainTermWords( mainTermWords, 
						"Main word oil exists and I'm a bad sentence.") );		
	}
	
	@Test
	public void testFilterFoundOnMainTermWordsMulti() {
		
		String[][] mainTermWords = 
			{ 
				{"oil",   "bad"},
				{"coal",  "mining"},
				{"wheat", "shredded"},
			};
		
		TweetAdapter tweetAdapter = new TweetAdapter();
		tweetAdapter.setMainTermWords(mainTermWords);
		tweetAdapter.init();
		
		assertNotNull( 
				tweetAdapter.filterOnMainTermWords( mainTermWords, 
						"Main word coal exists and I've got mining so I'm a bad sentence.") );		
	}
	
	@Test
	@Ignore
	public void testFilterFoundOnMainTermWordsWithEmoticons() {
		
		String[][] mainTermWords = 
			{ 
				{"oil", "filterMeOut"}			
			};
		
		TweetAdapter tweetAdapter = new TweetAdapter();
		tweetAdapter.init();
		tweetAdapter.setMainTermWords(mainTermWords);
		
		assertNotNull( 
				tweetAdapter.filterOnMainTermWords( mainTermWords, 
						"You must filterMeOut:) I'm a bad sentence and I'm talking about oil.") );		
	}
	
	
	@Test
	public void testFilterOnFragments() {
		
		String[][] mainTermWords = 
			{ 
				{"oil", "filterMeOut"}			
			};
		
		String[] fragments = { "fuck" };
		
		TweetAdapter tweetAdapter = new TweetAdapter();
		tweetAdapter.setMainTermWords( mainTermWords );		
		tweetAdapter.setFragments( fragments );
		tweetAdapter.init();
		
		assertNull( 
				tweetAdapter.filterOnFragments( fragments, 
						"I'm a good sentence and I'm talking about oil.") );		
	}

	/**
	 * Should pick up fucking fucker fucked fucks
	 */
	@Test
	public void testFilterFailOnFragments() {
		
		String[][] mainTermWords = 
			{ 
				{"oil", "filterMeOut"}			
			};
		String[] fragments = { "fuck" };
		
		TweetAdapter tweetAdapter = new TweetAdapter();
		tweetAdapter.setMainTermWords( mainTermWords );		
		tweetAdapter.setFragments( fragments );
		tweetAdapter.init();
		
		assertNotNull( "should fail because sentence contains fuck",
				tweetAdapter.filterOnFragments( fragments, 
						"I'm a bad fuck and I'm talking about oil.") );	
		
		assertNotNull( "should fail because sentence contains fucks",
				tweetAdapter.filterOnFragments( fragments, 
						"I'm a bad fucks and I'm talking about oil.") );	
		
		assertNotNull( "should fail  because sentence contains fucking",
				tweetAdapter.filterOnFragments( fragments, 
						"I'm a bad fucking and I'm talking about oil.") );	
		
		assertNotNull( "should fail  because sentence contains fucker",
				tweetAdapter.filterOnFragments( fragments, 
						"I'm a bad fucker and I'm talking about oil.") );	
	}

	
	/**
	 * Test term with filters, comment line
	 */
	@Test
	public void testsetupTermAndFilterComment() {
		
		TweetAdapter tweetAdapter = new TweetAdapter();
		try{
			tweetAdapter.setupTermAndFilter("#");
		} catch (Exception e){
			fail(e.getMessage());
		}
		
		assertNull( tweetAdapter.mainTermWords );
	}
	
	/**
	 * Test term with filters, blank line
	 */
	@Test
	public void testsetupTermAndFilterBlankLine() {
		
		TweetAdapter tweetAdapter = new TweetAdapter();
		try{
			tweetAdapter.setupTermAndFilter(" ");
		} catch (Exception e){
			fail();
		}
		
		assertNull( tweetAdapter.mainTermWords );
		
		try{
			tweetAdapter.setupTermAndFilter("");
		} catch (Exception e){
			fail(e.getMessage());
		}
		
		assertNull( tweetAdapter.mainTermWords );

	}

	/**
	 * Test fully expressed term with filters
	 */
	@Test
	public void testsetupTermAndFilter() {
		
		TweetAdapter tweetAdapter = new TweetAdapter();
		try{
			tweetAdapter.setupTermAndFilter("Term1:filter1,filter2,filter3");
		} catch (Exception e){
			fail(e.getMessage());
		}
		
		assertTrue( tweetAdapter.mainTermWords.length == 1 );
	}
	
	/**
	 * Test term with no filters
	 */
	@Test
	public void testsetupTermWithNoFilters() {
		
		TweetAdapter tweetAdapter = new TweetAdapter();
		try{
			tweetAdapter.setupTermAndFilter("Term2");
		} catch (Exception e){
			e.printStackTrace();
			fail(e.getMessage());
		}
		
		assertTrue( tweetAdapter.mainTermWords.length == 1 );
	}
	
	/**
	 * Test multiple terms
	 */
	@Test
	public void testsetupTerms() {
		
		TweetAdapter tweetAdapter = new TweetAdapter();
		try{
			tweetAdapter.setupTermAndFilter("Term0");
		} catch (Exception e){
			e.printStackTrace();
			fail(e.getMessage());
		}
		
		assertTrue( tweetAdapter.mainTermWords.length == 1 );
		
		try{
			tweetAdapter.setupTermAndFilter("Term1");
		} catch (Exception e){
			e.printStackTrace();
			fail(e.getMessage());
		}
		
		assertTrue( tweetAdapter.mainTermWords.length == 2 );
	}
}
