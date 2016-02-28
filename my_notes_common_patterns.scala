// Databricks notebook source exported at Sun, 28 Feb 2016 17:58:58 UTC
// MAGIC %md **myLearning**
// MAGIC Learning out comes So far
// MAGIC 1. Connecting to S3

// COMMAND ----------

//Connecting to S3 

import urllib

val AccessKey = "your access key"
val SecretKey = "your secret key"
val Encode_SecretKey = urllib(SecretKey, "")
val AWSBucket = "your aws bucket name"
val MountName = "your mount name"
//Mount your S3 bucket
dbutils.fs.mount("s3n://%s:%s@%s/" %(AccessKey,Encode_SecretKey,AWSBucket), "/mnt/%s/" %MountName) //protocol could be s3a / s3n

//Dispalay Mount
display(dbutils.fs.ls("/mnt/MountName"))
  //or
%fs ls




// COMMAND ----------

// MAGIC %md  ## 2. Downloading data from the web to S3
// MAGIC 1. Download data from web url to s3 for processinging in SPARK via Amazon Java SDK
// MAGIC   * Set *source uri*, *destination s3-bucket*, and *temperary location*  
// MAGIC   * Create tempfile object and downloaddata  
// MAGIC   * Create AmazonS3Client instance with authentication credentials  
// MAGIC   * Use putObject to load data into s3  
// MAGIC   [Reference Notebook](https://asimadi-leicester-research.cloud.databricks.com/#notebook/22278)

// COMMAND ----------

//Downloading data from the Web to S3 --- Did not work
val urlToRetrieve = "https://github.com/apache/spark/blob/master/README.md"
val tmpFileName ="/tmp/spark_README.md"
val s3FileName = "/testbed1/spark_README.md"

// Authentication Param
val AccessKey = "AKIAIA5PIGQFLCYCSSQA"
val SecretKey = "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"
val S3Bucket = "testbed1"

// retrieve file into temporary file.
import java.net.URL
import java.io.File
import org.apache.commons.io.FileUtils

val tmpFile = new File(tmpFileName)
FileUtils.copyURLToFile(new URL(urlToRetrieve), tmpFile)


// Use AmazonS3Client to load the tempfile
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.AmazonS3Client

val credentials = new BasicAWSCredentials(AccessKey, SecretKey);
val s3 = new AmazonS3Client(credentials);

s3.putObject(S3Bucket, s3Filename, tmpFile)

// for some reason this did not work and there was not error.


// COMMAND ----------

// MAGIC %md #Useful DBFS commands

// COMMAND ----------

//display(dbutils.fs.ls("/mnt"))
//dbutils.fs.help() 
//dbutils.fs.rm("/mnt/spark_README.md")
//display(dbutils.fs.ls("/databricks-datasets/learning-spark/data-001"))
//x.collect().foreach(println)

// COMMAND ----------

// MAGIC %md ##3. Download Web data to Databricks FileSystem. (Mount s3 == puting data in s3)
// MAGIC [Reference NoteBook](https://docs.cloud.databricks.com/docs/latest/databricks_guide/03%20Accessing%20Data/3%20Common%20File%20Formats/6%20Zip%20Files%20-%20scala.html)  Applies to Retrieve a zip file and write to dbfs
// MAGIC 
// MAGIC   

// COMMAND ----------

//Successfully load web data to dbfs/S3
// create a tmpfile object
import java.net.URL
import java.io.File
import org.apache.commons.io.FileUtils

//modify to have an array of URL for multiple downloads.

val localZipFile = new File("/tmp/ua.base")
FileUtils.copyURLToFile(new URL("http://files.grouplens.org/datasets/movielens/ml-100k/ua.base"), localZipFile)

dbutils.fs.mv("file:/tmp/ua.base", "dbfs:/mnt/xbrldata/ua.base")



display(dbutils.fs.ls("dbfs:/mnt/xbrldata/ua.base"))



// COMMAND ----------

// MAGIC %md ###3.1 Load data to S3 vai AWS CLI in python.
// MAGIC [Reference](https://databricks.com/wp-content/uploads/2015/08/Databricks-how-to-data-import.pdf)

// COMMAND ----------

// MAGIC %python
// MAGIC #1. install awscli
// MAGIC >> pip install awscli
// MAGIC #2. Get your aws credential from amazon
// MAGIC #3. Configure security credentials
// MAGIC >> aws configure
// MAGIC #4. create aws s3 bucket using the make bucket (mb) command
// MAGIC >> aws s3 mb s3://my-data-for-databricks/
// MAGIC #5. copy files to s3 bucket using cp command
// MAGIC >> aws s3 cp . s3://my-data-for-databricks/ --recursive

// COMMAND ----------

// MAGIC %md ## 4. Funcion to consume rest API 
// MAGIC This uses the pyspark and databricks seamlessly integrates the switch.  The Scala seems to have a complex http client I will investigate later.

// COMMAND ----------

// MAGIC %python
// MAGIC 
// MAGIC # Function takes on argument IP and returns its geolocation...
// MAGIC # getCCA2: Obtains two letter country code based on IP address
// MAGIC   
// MAGIC def getCCA2(ip):
// MAGIC   # Obtain CCA2 code from FreeGeoIP
// MAGIC   url = 'http://freegeoip.net/csv/' + ip
// MAGIC   str = urllib2.urlopen(url).read()
// MAGIC   cca2 = str.split(",")[1]
// MAGIC   
// MAGIC   # return
// MAGIC   return cca2
// MAGIC 
// MAGIC // #Test
// MAGIC 
// MAGIC #url = 'http://freegeoip.net/csv/41.67.167.185'
// MAGIC #str = urllib2.urlopen(url).read()
// MAGIC #str.split(",")[1]
// MAGIC 
// MAGIC 
// MAGIC //Alternatively
// MAGIC 
// MAGIC import requests 
// MAGIC 
// MAGIC response = requests.get(url)
// MAGIC 
// MAGIC data = response.json()
// MAGIC data[a][b][c]

// COMMAND ----------

// MAGIC %md **Define UDF function for sql**

// COMMAND ----------

# define function 
def osFamily(ua_string) : return xstr(parse(xstr(ua_string)).os.family)
#register udf furnction and use in SQL 
udfBrowserFamily = udf(browserFamily, StringType())

// COMMAND ----------

// MAGIC %md ## 5. USING Case Class in RDD
// MAGIC RDD containing case class can be converted into dataframe and schema inferred from the case class(limited to 22fields, use custome classes to bypass this)

// COMMAND ----------

//Define case clase
case class userMovieRating(userID: Int, movieID: Int, rating:Int)

// Function utilising the caseclass
def parse(line: String) = {
  val p = line.split("\t")
  val userID = p(0).toInt
  val movieID = p(1).toInt
  val rating = p(2).toInt
  //nested p() definition is posible
  //val example = p.slice(1,2).map(toDouble)
  
  userMovieRating(userID, movieID, rating)
}

val ratingsTRDD = ratings.map(line => parse(line))

// COMMAND ----------

// Download Zip File os
import os
os.system("cd /dbfs; rm master.zip; rm -rf learning-spark-master; wget https://github.com/databricks/learning-spark/archive/master.zip; unzip master.zip;ls")

// COMMAND ----------

// MAGIC %md ## 6. Working with Words -py (Stop-words and Lemmatizers)

// COMMAND ----------

// Python to import stop words "pip install stop-words". In databricks -> load library stop-words from new library menu.
%python
from stop_words import get_stop_words
stop_words = get_stop_words('en')
stop_words #display the stop words

#import the lemmatizer to normalise/stem words.
from nltk.stem import WordNetLemmatizer
lemmatizer = WordNetLemmatizer()
lemmatizer.lemmatize("Shareholders")