package org.dbpedia.spotlight.db.io

import org.dbpedia.spotlight.db.io._
import java.io.{PrintWriter, FileInputStream, File}
import org.dbpedia.spotlight.model.Token

import org.dbpedia.spotlight.db._
import disk.{DiskInvertedIndexStore, JDBMStore}
import org.dbpedia.spotlight.db.memory.{MemoryInvertedIndexStore, MemoryEsaVectorStore, MemoryStore}

import java.lang.{Short, String}
import org.apache.commons.lang.NotImplementedException
import java.util
//import collection.mutable

import org.dbpedia.spotlight.eval.corpus.MilneWittenCorpus


import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

import collection.mutable.ListBuffer
//import java.util.{Map, Set}
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

    val resStore = MemoryStore.loadResourceStore(new FileInputStream("data/res.mem"))
    val tokenStore = MemoryStore.loadTokenStore(new FileInputStream("data/tokens.mem"))

    //val invertedIndex: MemoryInvertedIndexStore = MemoryStore.loadInvertedIndexStore(new FileInputStream("data/invertedIndex.mem"))

    val esaMemoryIndexer = new EsaStoreIndexer(new File("data/"))
    esaMemoryIndexer.createInvertedIndexStore(tokenStore.size)
    LOG.info("the size of token store is %d".format(tokenStore.size))

    //Create wikipedia to DBpedia closure
    val wikipediaToDBpediaClosure = new WikipediaToDBpediaClosure(
      new FileInputStream(new File("raw_data/pig/redirects_en.nt")),
      new FileInputStream(new File("raw_data/pig/disambiguations_en.nt"))
    )

    //TODO: TESTING with TOLIST - check this - update: appears to be working
    //Note: there were problems with garbage collection - put back to Iterator for now
    val resourceMap: Iterator[(DBpediaResource, Array[Token], Array[Double])] =
      //TokenOccurrenceSource.fromJsonFile(new File("raw_data/json/top150-50000docs.json"),
      TokenOccurrenceSource.fromJsonFile(new File("raw_data/json/token_counts-top150.json"),
      tokenStore,
      wikipediaToDBpediaClosure,
      resStore
    )//.toList

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
             //println("The token is: "+ t.name)
             //println("its id is: " + t.id)
             //println("Its value is: " + weights(i))
             val tokenId = t.id
             //Map[tokenId[resourceId, weight]]
             //val index = new HashMap[Int, Double]

             //TODO: fix hard-coded tf-idf threshold below (changed to test indexing efficiency)
             //if (weights(i) > 1){

                //val index = esaMemoryIndexer.invertedIndex.index.getOrElse(tokenId, new HashMap[Int,Double]())
                //index.put(resId, weights(i))
                //esaMemoryIndexer.addResourceSet(tokenId, index)


                //println("resId is " + resId + " weights(i) is " + weights(i))
                val doc = (resId, weights(i))
                esaMemoryIndexer.addResource(tokenId, doc)
               /*
               var index = new mutable.HashMap[Int, Double]()
               //if (invertedIndex.contains(tokenId)) {
               //   println("the index contains this id")
                  index = invertedIndex.getResources(t)
               //}
               index.put(resId, weights(i))
               invertedIndex.add(tokenId, index)
             */
             //}
          }
          i += 1
        }

      }
    }
    //sort every list in the Inverted index and retain only topN elements
    //TODO: testing here - make sure that the sort is correct
    esaMemoryIndexer.invertedIndex.topN(25)

    /*
    //TEST - working
    var c =0
    esaMemoryIndexer.invertedIndex.docs.foreach {
      case (null) =>
      case (list: ListBuffer[(Int, Double)]) => {
        println("For token: " + tokenStore.getTokenByID(c))
        list.foreach {
          case (d: Int, w: Double) => {
            println("doc is: " + d + ", weight is: " + w)

          }
        }
        c += 1
      }

    }
    */

    /*
    //TODO: disk store persistence testing - update: It seems like I/O is too slow for now
    //esaMemoryIndexer.writeInvertedIndex()
    //invertedIndex.commit()
    //val iiTest = new JDBMStore[Int, Map[Int, Double]]("ii.disk")

    //BEGIN PERSISTING InvertedIndex
    //Now persist the inverted index
    //val baseDir = new File("/home/chris/data/indexes")
    val testFileName = "/home/chris/data/indexes/iiTest.disk"
    esaMemoryIndexer.invertedIndex.persistIndex(testFileName)
    //END PERSISTING INVERTED INDEX

    val testInvertedIndex = new DiskInvertedIndexStore("/home/chris/data/indexes/iiTest.disk")
    */
    /*
    //TEST (I know this token should be there)
    for (i <- 1 to 200000) {
      testInvertedIndex.printAll(i)
    }
    */


    //Now the invertedIndex is finished - create the ESAVectorIndex
    //TODO: Change this section to use the disk-backed inverted index
    //  - (1) load the inverted index (created in another step)
    //  - (2) read vectors directly from disk
    //iterate over the resource map (iteration code copied from above)
    //println ("now for the vector index...")
    LOG.info("now for the vector index...")
    val dataMap: Iterator[(DBpediaResource, Array[Token], Array[Double])] =
    //TokenOccurrenceSource.fromJsonFile(new File("raw_data/json/top150-50000docs.json"),
    TokenOccurrenceSource.fromJsonFile(new File("raw_data/json/token_counts-top150.json"),
        tokenStore,
        wikipediaToDBpediaClosure,
        resStore
      )
    var docCount: Int = 0
    dataMap.filter(t => t!=null && t._1 != null).foreach {
      t: Triple[DBpediaResource, Array[Token], Array[Double]] => {
        val Triple(res, tokens, weights) = t
        //println("indexing docs: the resource is: " + res)
        //val resId = res.id
        //println("its id is: " + resId)
        docCount += 1
        if (docCount % 10000 == 0) {
          LOG.info("Made ESA vectors for %d resources".format(docCount))
        }

        var docIndex = new HashMap[Int, Double]()
        var i =0

        //get the vector of docs from the inverted index
        tokens.foreach {
          (tok: Token) => {
            //query the inverted index and sum the scores for each doc
            //val tokId = tok.id
            val tokenWeight = weights(i)

            //TODO: get the vector from the disk-backed inverted index
            //TODO: handle nulls properly

            //TODO: Decide whether to use in-memory or disk-backed inverted indexes

            //TESTING MEMORY BACKED - this is throwing a garbage collection error
            val tokensDocVector = esaMemoryIndexer.invertedIndex.getResources(tok)

            //TESTING DISK BACKED...
            //val tokensDocVector = testInvertedIndex.getResources(tok)

             if (tokensDocVector != null) {
                //TEST
                val s = tokensDocVector.size
                //println("The token vector for " + tok.name +" contains " + s + " docs" )
                //val tokenVector: Map[Int,Double] = invertedIndex.getResources(tok)
                tokensDocVector.foreach {case (docId: Int, weight: Double) => {

                   val currentScore: Double = docIndex.getOrElse(docId, 0.00)
                   //sum the weights multiplied by the token's tfidf score in this doc
                   docIndex.put(docId, currentScore + (weight * tokenWeight))
                   //println("token: " + tok.name)
                   //val newWeight = docIndex.getOrElse(docId, 0.0)
                   //println("new weight for docId: "+ docId + " is: " + newWeight)
                }
              }
             }
            i+=1
          }
        }
        //now get the centroid - Update: looks as if we need to sort and keep only top N values, otherwise mem reqs are too high
        val noTokens = tokens.size
        docIndex.foreach {
          case (docId: Int, weight: Double) => {
            val avgWeight = weight/noTokens
            docIndex.put(docId, avgWeight)
          }
        }
        //TESTING - threshold hard-coded for now
        val topN = 100
        val topList = docIndex.toList.sortBy(_._2).drop(docIndex.size-topN)
        val topMap = new HashMap[Int, Double]()
        topList.foreach {
          case (docId: Int, weight: Double) => {
            topMap.put(docId, weight)
          }
        }

        esaMemoryIndexer.addDocOccurrence(res, topMap)


      }
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
      val sfStore = MemoryStore.loadSurfaceFormStore(new FileInputStream("data/sf.mem"))
      val cm = MemoryStore.loadCandidateMapStore(new FileInputStream("data/candmap.mem"), resStore)

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
