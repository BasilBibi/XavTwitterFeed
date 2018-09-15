Hi Xav,

Ok you asked for it here's a zip containing the source code.

Please bear in mind this is not what I'd consider to be production grade code
and the project is not structured in a manner that I'd normally push out but
it works and is reasonably simple. Filtering is perhaps a little overcomplex.


To get this project running you need to install maven 3, subversion or git and an ide.
I use eclipse and Intellij (community edition is free).

Then import the pom.xml into a new project run, the tests and away you go.

The code is not perfect and some of the primary filtering (and tests ) has been commented out or disabled.
I anticipate there will be a few rounds of support to get you or your dev up and running with this.


It will run as is but here are some thoughts about refactoring and improvement:
1. It is hard coded to write tweets to the user home folder (com.bbb.TweetAdapter.java:88)
2. Change the Twitter OAuth1 code used. It is hard coded to be me (at com.bbb.TweetAdapter.java:304)
   so you will want to run it from your account and put the actual key in a property file somewhere.
3. I'd also recommend using spring to manage the properties that are in the TrackTerms.txt file.
4. You might also consider abstracting the writer although importing to a database / hadoop etc is
   pretty trivial with the file output as it is.
5. Simplify the filtering.
6. Make a simpler deployment using maven.


Then I guess it's about how you analyse the data. Retweet frequencies, locations and latency might
be good avenues for interpretation.

One final word about building, deploying and running it. You can build it in the ide but to package
it you need to run mvn clean install in the root folder where the pom file resides.

This will generate a jar file in the "target" folder.
There is a script to run this in resources/scripts.

I deliberately didn't use maven to generate a deployable script - you will have to copy the one
in the resources/scripts folder into the same location you copy the jar file.


Ok so here is the build deploy process:

1. go to the folder where pom.xml resides
2. mvn clean compile assembly:single

This creates a "target" folder which will contain the jar file
(twitter-0.0.2-SNAPSHOT-jar-with-dependencies.jar) and this part of the sentence is a test to see if you read my instructions :)


To get it all running you need to copy the jar file and the following files
from the resources folder into a destination folder before running:

TrackTerms.txt to this folder.
logback.xml to this folder
runTweetAdapter.cmd

To run just execute the runTweetAdapter.cmd file in the destination folder.

Regards Basil