// Databricks notebook source exported at Tue, 23 Feb 2016 07:28:59 UTC
// MAGIC %md #My Learning from Standford NLP

// COMMAND ----------

// acquiring data from github url

import java.net.URL
import java.io.File
import org.apache.commons.io.FileUtils


val localZipFile = new File("/tmp/SMSSpamCollection")  // not a zipfile just a name.:)
FileUtils.copyURLToFile(new URL("https://raw.githubusercontent.com/arunma/ScalaDataAnalysisCookbook/master/chapter5-learning/SMSSpamCollection"), localZipFile)

dbutils.fs.mv("file:/tmp/SMSSpamCollection", "dbfs:/mnt/xbrldata/SMSSpamCollection")

val corpus = sc.textFile("dbfs:/mnt/xbrldata/SMSSpamCollection")

corpus.take(100).map(println)


// COMMAND ----------

//Case Clase for label and content

case class documents(title: String, content: String)

val plaintext = corpus.map(line => {
                    val words = line.split("\t")
                    documents(words.head.trim() , words.tail.mkString(" "))
                })

plaintext.take(100).map(println)

// COMMAND ----------

// (NB:Not used) Handy funtions -case class- for building (Label, document) pair from corpus

//1. Create the case class to keep it clean.

case class document(Heading: String, Content: String)

val docs = sc.textFile("dbfs:/mnt/xbrldata/SMSSpamCollection").map(line => {
  val words = line.split("\t")
  document(words.head.trim(), words.tail.mkString(" "))
} )

docs.take(5).map(println)

//import epic.preprocessor.TreebankTokenizer
//import epic.preprocessor.MLSentenceSegmenter  # epic libraries failed during maven build.

// COMMAND ----------

// (NB:Not used) Nice patterns for extrating words and Spam Filters.

//Tokens(using Tokenizer) that are letters or digits and more than 2 letters 
//#filteredTokens = Tokens.toList.filter(token => token.forall(_.isLetterOrDigit)).filter(_.lenght() > 1 ) )

//#new Labeledpoint(if (doc.label=="ham") 0 else 1, hashTF.transform(filteredTokens) )

// COMMAND ----------

//Using the Stanford NLP library *when you attach the library you need to restart the cluster.
import scala.collection.JavaConversions._
import edu.stanford.nlp.pipeline._
import edu.stanford.nlp.ling.CoreAnnotations._
import java.util.Properties

//Create the NLP Pipeline
def createNLPPipeline(): StanfordCoreNLP = {
  val props = new Properties()
  props.put("annotators","tokenizer, ssplit, pos, lemma")
  
  new StanfordCoreNLP(props)
}

// COMMAND ----------

def isOnlyLettersOrDigits(word: String): Boolean = {
  //word.forall(c => Character.isLetterOrDigit(c))
  word.forall(_.isLetterOrDigit)
}

isOnlyLettersOrDigits("checkmeplease1 2334") // # Testing.

// COMMAND ----------

val myval = scala.io.Source.fromFile("file:/mnt/xbrldata/sona/sona_2008.txt").getLines().toSet

// COMMAND ----------

// MAGIC %fs help

// COMMAND ----------

// Convert Plain Text to Lemmas

def plainTextToLemmas(content: String, stopwords: set[String], pipeline: StanfordCoreNLP): Seq[String]{
  val annotateddoc =  new Annotation(content)
  pipeline.annotate(annoteddoc)
  
  lemmas = new ArrayBuffer[String]()
  sentences = annotateddoc.get([ClassOfSentencesAnnotation])
  
  for (sentence <- sentences;
    tokens <- sentence.get([ClassOfTokensAnnotation])  ) {
    val lemma = tokens.get([ClassOfLemmaAnnotation])
    if (lemma.length() > 2 && !stopwords.contains(lemma) && isOnlyLettersOrDigits(lemma) )
    { lemmas = lemma.toLowerCase() }
  }
  
  lemmas
}

val stopWords = sc.broadcast( scala.io.source.fromFile(/mnt/xbrldata/stop_words.txt).getLines().toSet).value

val lemmatized: RDD[String] = plaintext.mapPartitions(it => { 
  val pipeline = new createPipeline()
  
  it.map{case (title, content) => plainTextToLemmas(content, stopwords, pipeline)}

  
})

// COMMAND ----------

