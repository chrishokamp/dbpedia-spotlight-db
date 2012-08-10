package org.dbpedia.spotlight.db.io

import org.dbpedia.spotlight.db.io._
import java.io.{PrintWriter, FileInputStream, File}
import org.dbpedia.spotlight.model.Token

import org.dbpedia.spotlight.db._
import disk.JDBMStore
import org.dbpedia.spotlight.db.memory.{MemoryInvertedIndexStore, MemoryEsaVectorStore, MemoryStore}

import java.lang.{Short, String}
import org.apache.commons.lang.NotImplementedException
import java.util
import collection.mutable

import org.dbpedia.spotlight.eval.corpus.MilneWittenCorpus


import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

import collection.mutable.ListBuffer
import java.util.{Map, Set}
import org.dbpedia.spotlight.model._
import scala.{Array, Int}
import collection.mutable.HashMap
import tools.nsc.io.ManifestOps
import org.dbpedia.spotlight.exceptions.DBpediaResourceNotFoundException
import org.apache.lucene.analysis.en.EnglishAnalyzer
import org.apache.lucene.util.Version
import org.dbpedia.spotlight.disambiguate.mixtures.LinearRegressionMixture
import org.dbpedia.spotlight.spot.WikiMarkupSpotter
import org.dbpedia.spotlight.model.Factory.SurfaceFormOccurrence
import org.apache.commons.logging.LogFactory
import org.dbpedia.spotlight.eval.{EvalUtils, TSVOutputGenerator, EvaluateParagraphDisambiguator}
//import org.dbpedia.spotlight.io.TSVOutputGenerator
//import org.dbpedia.spotlight.evaluation.EvalUtils


/**
 * @author Chris Hokamp
 *
 * Working Notes: don't need SurfaceFormSource for now
 * TokenSource.fromPigFile was changed - fix this asap (don't modify previous functionality)!
 *
 * doc frequency is working - next step - add DocFreq counts from MemoryContextStore
 *      - calculate tfidf
 *
 *  MemoryContextStore has an attribute (size) that should provide |R|
 *
 *
 *  Objects to create or modify:
 *  I need a Map[Resource, Map[Int, Int --> per document counts (contains tf)
 *  this will be accompanied by an index [Int, Token]
 *
 *  MemoryContextStore.getContextCounts(DBpediaResource)
 *
 *         - the DBpediaResource for this is returned from the CandidateMap
 *         - the MemoryStoreIndexer object needs to be rewritten or modifed for my purposes
 *
 */

object BuildIndexTest {
  private val LOG = LogFactory.getLog(this.getClass)

  def main(args: Array[String]) {


    /*
    //Test file from pig.storage
    val sourceFile = new File("data/pig/out.pig")
    val path: String = sourceFile.getAbsolutePath()
    System.out.println("Absolute path is: " + path)
    //val tokenCount: Map[Token, Int] = TokenSource.fromPigFile(sourceFile)


    val docFreqCount: Map[Token, Int] = TokenSource.dfFromPigFile(sourceFile)

    val tokens = new Array[String](tokenCount.size)
    val counts = new Array[Int](tokenCount.size)

    //val docFreq: Map[Token, Int]

    var largestCount = 0
    var largest = 0
    var total: Int = 0
    var largestToken: Token = null
    tokenCount.foreach {
      case (token, count) => {
        tokens(token.id) = token.name
        //System.out.println("id is: " + token.id)
        counts(token.id) = count
        //System.out.println("name is: " + token.name)
        if (token.id > largest) {
          largest = token.id
        }
        if (count > largestCount) {
          largestCount = token.count
          largestToken = token

        }

        total += 1
      }
    }
    System.out.println("total is: " + total)
    System.out.println("largest is: " + largest)
    System.out.println("largestCount is: " + largestCount)
    System.out.println("largestToken is: " + largestToken.name)
    */

    //working on EsaStoreIndexer...
    //ImportPig uses MemoryStoreIndexer, which EsaStoreIndexer extends
    //val memoryIndexer = new MemoryStoreIndexer(new File("data/"))

    val esaMemoryIndexer = new EsaStoreIndexer(new File("data/"))

    val resStore = MemoryStore.loadResourceStore(new FileInputStream("data/res.mem"))
    val tokenStore = MemoryStore.loadTokenStore(new FileInputStream("data/tokens.mem"))
    val sfStore = MemoryStore.loadSurfaceFormStore(new FileInputStream("data/sf.mem"))
    val cm = MemoryStore.loadCandidateMapStore(new FileInputStream("data/candmap.mem"), resStore)
    //val invertedIndex: MemoryInvertedIndexStore = MemoryStore.loadInvertedIndexStore(new FileInputStream("data/invertedIndex.mem"))


    //TESTING disk-backed inverted index
    val baseDir = new File("/home/chris/data/indexes")
    //val persistedInvertedIndex = new JDBMStore[Int, mutable.HashMap[Int, Double]](new File(baseDir, "ii.disk").getAbsolutePath)

    //Create wikipedia to DBpedia closure
    val wikipediaToDBpediaClosure = new WikipediaToDBpediaClosure(
      new FileInputStream(new File("raw_data/pig/redirects_en.nt")),
      new FileInputStream(new File("raw_data/pig/disambiguations_en.nt"))
    )


    //Create the InvertedIndex and the EsaVectorStore Simultaeneously
    // (1) Load the tfidf index via ImportJSON
    // (2) For each doc, iterate over its tokens
    // (3) For each token, check if its vector exists
    //          - vectors are stored in MemoryEsaVectorStore, tokensIDs are


    val resourceMap: Iterator[(DBpediaResource, Array[Token], Array[Double])] =
      //TokenOccurrenceSource.fromJsonFile(new File("raw_data/json/tfidf-sample.json"),
      TokenOccurrenceSource.fromJsonFile(new File("raw_data/json/min5-50000-docs.json"),
      tokenStore,
      wikipediaToDBpediaClosure,
      resStore
    )

    //ids are token ids, vals are resID --> tokenWeight
    //lazy val invertedIndex: Map[Int, Map[Int, Double]] = new HashMap[Int, Map[Int, Double]]()
    //End test
    //TODO: this structure should be persisted




    //TESTING MemoryInvertedIndexStore
    resourceMap.filter(t => t!=null && t._1 != null).foreach{
      t: Triple[DBpediaResource, Array[Token], Array[Double]] => {
        val Triple(res, tokens, weights) = t
        //println("the resource is: " + res)
        val resId = res.id
       //println("its id is: " + resId)

        var i =0
        //get the vector from Map structure

        tokens.foreach {
          (t: Token) => {
             //System.out.println("The token is: "+ t.name)
             //System.out.println("its id is: " + t.id)
             //System.out.println("Its value is: " + weights(i))
             val tokenId = t.id
             //Map[tokenId[resourceId, weight]]
             //val index = new HashMap[Int, Double]

             //TODO: fix hard-coded tf-idf threshold below (changed to test indexing efficiency)
             if (weights(i) > 50){

                val index = esaMemoryIndexer.invertedIndex.index.getOrElse(tokenId, new HashMap[Int,Double]())
                index.put(resId, weights(i))
                esaMemoryIndexer.addResourceSet(tokenId, index)

               /*
               var index = new mutable.HashMap[Int, Double]()
               //if (invertedIndex.contains(tokenId)) {
               //   println("the index contains this id")
                  index = invertedIndex.getResources(t)
               //}
               index.put(resId, weights(i))
               invertedIndex.add(tokenId, index)
             */
             }

             i += 1
          }
        }

      }
    }
    //TODO: testing here
    //esaMemoryIndexer.writeInvertedIndex()
    //invertedIndex.commit()
    //val iiTest = new JDBMStore[Int, Map[Int, Double]]("ii.disk")
    //persistedInvertedIndex.data = invertedIndex.index


    //TODO: create a more efficient method - this repeats loading tokens
    val docs: Iterator[(DBpediaResource, Array[Token], Array[Double])] =
      TokenOccurrenceSource.fromJsonFile(new File("raw_data/json/min5-50000-docs.json"),
        tokenStore,
        wikipediaToDBpediaClosure,
        resStore
      )

    //Now the invertedIndex is finished - create the ESAVectorIndex
    //TODO: iterate over the resource map (iteration code copied from above)
    println ("now for the vector index...")
    var docCount: Int = 0
    docs.filter(t => t!=null && t._1 != null).foreach{
      t: Triple[DBpediaResource, Array[Token], Array[Double]] => {
        val Triple(res, tokens, weights) = t
        //println("indexing docs: the resource is: " + res)
        //val resId = res.id
        //println("its id is: " + resId)
        docCount += 1


        var docIndex = new HashMap[Int, Double]()
        var i =0
        //get the vector from Map structure

        tokens.foreach {

              //TODO: this was throwing null-pointer - find problem and fix
          (tok: Token) => {
            //query the inverted index and sum the scores for each doc
            val tokId = tok.id

            val tokenVector = esaMemoryIndexer.invertedIndex.getResources(tok)

             if (tokenVector != null) {
                //val tokenVector: Map[Int,Double] = invertedIndex.getResources(tok)
                tokenVector.foreach {case (id: Int, weight: Double) => {

                   val currentScore: Double = docIndex.getOrElse(tokId, 0.00)
                   //sum the weights multiplied by the token's tfidf score in this doc
                   docIndex.put(tokId, currentScore + (weight * weights(i)))
                   //println("token: " + tok.name)
                   //val newWeight = docIndex.getOrElse(tokId, 0.0)
                   //println("new weight: " + newWeight)
                }
              }
             }
            i+=1
          }
        }
        esaMemoryIndexer.addDocOccurrence(res, docIndex)
      }
      docCount += 1
    }

      /*
      //TEST
      val vals = esaMemoryIndexer.vectorStore.resources.keys

      vals.foreach {
        (i: Int) => {
          println("THE RESOURCE IS: " + resStore.getResource(i).uri)
            val vector = esaMemoryIndexer.vectorStore.getDocVector(i)
            val sorted = vector.toList sortBy {_._2}
            sorted.foreach {
              case (id: Int, weight: Double) => {
                 //println ("docId: " + id)
                 try {
                    println ("Resource: " + resStore.getResource(id).uri)
                    println ("Score: " + weight)
                 }
                 catch {
                   case e: DBpediaResourceNotFoundException => println ("resource: "+ id + " doesn't exist")
                 }

              }
            }
        }
      }
      //END TEST
      */

      //Now test DBEsaDisambiguator
    println("About to create the disambiguator...")
      val disambiguator = new DBEsaDisambiguator(
        tokenStore,
        sfStore,
        resStore,
        cm,
        esaMemoryIndexer.invertedIndex,
        esaMemoryIndexer.vectorStore,
        new LuceneTokenizer(new EnglishAnalyzer(Version.LUCENE_36)),
        new LinearRegressionMixture()
        )
      /*
      val spotter = new WikiMarkupSpotter()
      //val t = new Text("[[Berlin]] is the capital of [[Germany]].")
      val t = new Text("The [[ocean]] has lots of [[water]] and is full of fish, sharks, whales and underwater wonders.")
      val spots = spotter.extract(t)
      val p = new Paragraph(t, spots.asScala.toList)

    val results = disambiguator.bestK(p, 10)
    results.foreach {
      case (sf: SurfaceFormOccurrence, matches: List[DBpediaResourceOccurrence]) => {
        println("for Surface form occurrence: " + sf.toString())
        println("RESULTS: " + matches.toString())
      }
    }
      //println(disambiguator.bestK(p, 10))
     */
    //TODO: now use EvaluateParagraphDisambiguator to test on MilneWitten
    //(1) create the corpus from directory
    val mw = MilneWittenCorpus.fromDirectory(new File("raw_data/MilneWitten-wikifiedStories"))
    val testSourceName = mw.name
    val dName = disambiguator.name.replaceAll("""[.*[?/<>|*:\"{\\}].*]""","_")
    //val tsvOut = new TSVOutputGenerator(new PrintWriter("%s-%s-%s.milne-witten.log".format(testSourceName,dName,EvalUtils.now())))
    //val arffOut = new TrainingDataOutputGenerator()
    //val outputs = List(tsvOut)
    val pw = new PrintWriter("%s-%s-%s.milne-witten.log".format(testSourceName,dName,EvalUtils.now()))

    //(2) create the EvaluateParagraphDisambiguator
    EvaluateParagraphDisambiguator.evaluate(mw, disambiguator, pw)



    //TODO: persist invertedIndex and EsaVectorIndex
    /*
    - for indexing documents, and when a query comes:
    - tokenize
    - for each token, check if it's in the index
    - iterate over tokens keeping count
        - foreach, get df from invertedIndex 1x
        - this is (noKeys) in the invertedIndex
        - save in docFreq HashMap
        -

    - for resources, we already know the ifidf values
    - for querys, we'll need to do tfidf
     */




    /*
    memoryIndexer.addTokenOccurrences(
      TokenOccurrenceSource.fromPigFile(
        //TODO: change to the correct file name and format
        //TESTING with 100000 resources -"raw_data/pig/100000resources_min_2.TSV"
        new File("raw_data/pig/token_counts_min_2.TSV"),
        tokenStore,
        wikipediaToDBpediaClosure,
        resStore
      )
    )
    esaMemoryIndexer.writeTokenOccurrences()
  }

  //def printTokens (tokenCount: Map[Token, Int]) {

  //
      */

  }
}
