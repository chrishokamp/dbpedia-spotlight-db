package org.dbpedia.spotlight.db

import org.dbpedia.spotlight.db.io._
import java.io.{PrintWriter, FileInputStream, File}
import disk.{DiskInvertedIndexStore, JDBMStore}
import org.dbpedia.spotlight.db.memory.{MemoryInvertedIndexStore, MemoryEsaVectorStore, MemoryStore}

import java.lang.{Short, String}
import org.dbpedia.spotlight.eval.filter.occurrences.RedirectResolveFilter

import org.dbpedia.spotlight.eval.corpus.{CSAWCorpus, MilneWittenCorpus}

import org.dbpedia.spotlight.model._
import scala.{Array, Int}
import collection.mutable.HashMap
import org.apache.lucene.analysis.en.EnglishAnalyzer
import org.apache.lucene.util.Version
import org.dbpedia.spotlight.disambiguate.mixtures.LinearRegressionMixture
import org.apache.commons.logging.LogFactory
import org.dbpedia.spotlight.eval.{EvalUtils, TSVOutputGenerator, EvaluateParagraphDisambiguator}

/**
 * @author Chris Hokamp
 *
 * This class runs ESA indexing and disambiguation evaluation from beginning to end
 *
 *
 */

object IndexEsa {
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
      TokenOccurrenceSource.fromJsonFile(new File("raw_data/json/token_counts-20120601-top150.json"),
        tokenStore,
        wikipediaToDBpediaClosure,
        resStore
      ) //.toList

    resourceMap.filter(t => t != null && t._1 != null).foreach {
      t: Triple[DBpediaResource, Array[Token], Array[Double]] => {
        val Triple(res, tokens, weights) = t
        val resId = res.id
        var i = 0

        tokens.foreach {
          (t: Token) => {
            val tokenId = t.id
            //Map[tokenId[resourceId, weight]]
            //val index = new HashMap[Int, Double]

            //TODO: fix hard-coded tf-idf threshold below (changed to test indexing efficiency)
            if (weights(i) > 5){
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
              }
          }
          i += 1
        }
      }
    }
    //sort every list in the Inverted index and retain only topN elements
    //TODO: testing here - make sure that the sort is correct
    esaMemoryIndexer.invertedIndex.topN(10)

    /*
    //TODO: testing Kryo persistence of inverted index
    esaMemoryIndexer.writeInvertedIndex()

    Update: getting heap space error here - ask Jo about this??
    val testii = MemoryStore.loadInvertedIndexStore(new FileInputStream("data/invertedIndex.mem"))
    //TEST
    val indexSize = testii.resources.size
    println("the size of testii is: " + indexSize)
    */

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


    /*Now the invertedIndex is finished - create the ESAVectorIndex
      - (1) load the inverted index (created in another step)
      - (2) read vectors directly from disk
      - (3) iterate over the resource map (iteration code copied from above)
    */
    //println ("now for the vector index...")
    LOG.info("now for the vector index...")
    val dataMap: Iterator[(DBpediaResource, Array[Token], Array[Double])] =
    //TokenOccurrenceSource.fromJsonFile(new File("raw_data/json/top150-50000docs.json"),
      TokenOccurrenceSource.fromJsonFile(new File("raw_data/json/token_counts-20120601-top150.json"),
        tokenStore,
        wikipediaToDBpediaClosure,
        resStore
      )
    var docCount: Int = 0
    dataMap.filter(t => t != null && t._1 != null).foreach {
      t: Triple[DBpediaResource, Array[Token], Array[Double]] => {
        val Triple(res, tokens, weights) = t
        docCount += 1
        if (docCount % 10000 == 0) {
          LOG.info("Made ESA vectors for %d resources".format(docCount))
        }

        var docIndex = new HashMap[Int, Double]()
        var i = 0

        //get the vector of docs from the inverted index
        tokens.foreach {
          (tok: Token) => {
            val tokenWeight = weights(i)

            //TODO: get the vector from the disk-backed inverted index
            //TODO: handle nulls properly

            //TODO: Decide whether to use in-memory or disk-backed inverted indexes

            //TESTING MEMORY BACKED - this is throwing a garbage collection error when read from disk
            val tokensDocVector = esaMemoryIndexer.invertedIndex.getResources(tok)

            //TESTING DISK BACKED...
            //val tokensDocVector = testInvertedIndex.getResources(tok)

            if (tokensDocVector != null) {
              val s = tokensDocVector.size
              //println("The token vector for " + tok.name +" contains " + s + " docs" )
              tokensDocVector.foreach {
                case (docId: Int, weight: Double) => {

                  val currentScore: Double = docIndex.getOrElse(docId, 0.00)
                  //sum the weights multiplied by the token's tfidf score in this doc
                  docIndex.put(docId, currentScore + (weight * tokenWeight))
                }
              }
            }
            i += 1
          }
        }
        //now get the centroid - Update: looks as if we need to sort and keep only top N values, otherwise mem reqs are too high
        val noTokens = tokens.size
        docIndex.foreach {
          case (docId: Int, weight: Double) => {
            val avgWeight = weight / noTokens
            docIndex.put(docId, avgWeight)
          }
        }
        //TESTING - threshold hard-coded for now
        val topN = 50
        val topList = docIndex.toList.sortBy(_._2).drop(docIndex.size - topN)
        val topMap = new HashMap[Int, Double]()
        topList.foreach {
          case (docId: Int, weight: Double) => {
            topMap.put(docId, weight)
          }
        }

        esaMemoryIndexer.addDocOccurrence(res, topMap)
      }
    }

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

    //Milne-Witten
    //val mw = MilneWittenCorpus.fromDirectory(new File("raw_data/MilneWitten-wikifiedStories"))
    //val testSourceName = mw.name

    val dName = disambiguator.name.replaceAll( """[.*[?/<>|*:\"{\\}].*]""", "_")

    //val tsvOut = new TSVOutputGenerator(new PrintWriter("%s-%s-%s.milne-witten.log".format(testSourceName,dName,EvalUtils.now())))
    //val arffOut = new TrainingDataOutputGenerator()
    //val outputs = List(tsvOut)
    //val pw = new PrintWriter("%s-%s-%s.milne-witten.log".format(testSourceName,dName,EvalUtils.now()))

    //CSAW
    val csaw = CSAWCorpus.fromDirectory(new File("raw_data/csaw"))
    val testSourceName2 = csaw.name
    //val cs = new PrintWriter("%s-%s-%s.csaw.log".format(testSourceName2,dName,EvalUtils.now()))

    //Make the occ filters
    val redirectTCFileName = if (args.size > 1) args(1) else "data/redirects_tc.tsv" //produced by ExtractCandidateMap

    val occFilters = List(RedirectResolveFilter.fromFile(new File(redirectTCFileName)))
    val tsvOut = new TSVOutputGenerator(new PrintWriter("%s-%s-%s.milne-witten.log".format(testSourceName2, dName, EvalUtils.now())))
    val outputs = List(tsvOut)


    //(2) create the EvaluateParagraphDisambiguator
    //EvaluateParagraphDisambiguator.evaluate(mw, disambiguator, pw)
    EvaluateParagraphDisambiguator.evaluate(csaw, disambiguator, outputs, occFilters)
  }
}
