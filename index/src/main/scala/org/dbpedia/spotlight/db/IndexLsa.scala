package org.dbpedia.spotlight.db

import io.TokenOccurrenceSource
import memory.MemoryStore
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.FileSystem
import org.apache.hadoop.fs.Path
import org.apache.hadoop.io.IntWritable
import org.apache.hadoop.io.LongWritable
import org.apache.hadoop.io.SequenceFile
import org.apache.hadoop.io.Text

import java.io._
import org.apache.mahout.math.VectorWritable
import org.apache.mahout.math.Vector
import org.apache.lucene.analysis.en.EnglishAnalyzer
import org.apache.lucene.util.Version


import collection.mutable
import org.tartarus.snowball.ext.PorterStemmer
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute
import com.aliasi.matrix.DenseVector
import org.dbpedia.spotlight.model.{Token, DBpediaResource}
import collection.mutable.HashMap
import org.dbpedia.spotlight.disambiguate.mixtures.OnlySimScoreMixture
import org.dbpedia.spotlight.eval.corpus.{CSAWCorpus, MilneWittenCorpus}
import org.dbpedia.spotlight.eval.filter.occurrences.{RedirectResolveFilter, UriWhitelistFilter}
import org.dbpedia.spotlight.eval.{EvaluateParagraphDisambiguator, EvalUtils, TSVOutputGenerator}
import org.apache.commons.logging.LogFactory


/**
 * @author Chris Hokamp
 * -Working notes:
 * (1) read term vectors from sequence file and add to inverted index for each term - we need a term --> docId index
 * (2) iterate over docs and create index - for now, just take the centroid of the term vectors
 *
 * - This class is for LSA demonstration only!
 */
object IndexLsa {

  private val LOG = LogFactory.getLog(this.getClass)

  def main(args: Array[String]) {
    val baseDir = args(0)
    //TODO: initialize these valuse in main()
    val conf = new Configuration
    val fs = FileSystem.get(conf)

    val field = "term"
    val analyzer = new EnglishAnalyzer(Version.LUCENE_36)

    val tokenStore = MemoryStore.loadTokenStore(new FileInputStream(baseDir+"data/tokens.mem"))
    val esaMemoryIndexer = new EsaStoreIndexer(new File(baseDir+"data/"))
    esaMemoryIndexer.createInvertedIndexStore(tokenStore.size)

    //val matrixDir = new File("/home/chris/data/sequence-files/milne-V_t-10/V/")
    //val dictFile = new File("/home/chris/data/sequence-files/milne-V_t-10/dictionary.file-0")
    val matrixDir = new File(baseDir+"raw_data/LSA/milne-V_t-10/V/")
    val dictFile = new File(baseDir+"raw_data/LSA/milne-V_t-10/dictionary.file-0")

  //def buildTermIndex (dictFile: File): mutable.HashMap[Int, Int] = {
    //indexes lsaId to tokenId
    val termIndex = new mutable.HashMap[Int, Int]
    val p = new Path(dictFile.getAbsolutePath)

    //create the term index from the mahout dictionary file
    val reader = new SequenceFile.Reader(fs, p, conf)

    var term = new org.apache.hadoop.io.Text()
    var value = new IntWritable()
    while (reader.next(term, value)) {
      val keyString = term.toString
      var stemmedTerm = new String
      //this is done to ensure that tokens are the same as those in the corpus
      val stream = analyzer.reusableTokenStream(field, new StringReader(keyString))
      while (stream.incrementToken()) {
        stemmedTerm = stream.getAttribute(classOf[CharTermAttribute]).toString
      }
      val token = tokenStore.getToken(stemmedTerm)
      if (token.id != 0) {
        termIndex.put(value.get(), token.id)
        //println("the tokenStore contains the token: " + stemmedTerm)
      }
    }
  //create the index --> Map[tokenId, Vector]
  val matrixFiles = matrixDir.listFiles
  matrixFiles.foreach {
    case (f: File) => {
      val filename = f.getAbsolutePath();
      val p = new Path(filename)
      val reader = new SequenceFile.Reader(fs, p, conf)

      var key = new IntWritable()
      var value = new VectorWritable()

      //read the term vectors
      while (reader.next(key, value)) {
         val lsaId: Int = key.get()
         val tokenId = termIndex.getOrElse(lsaId, -1)
         if (tokenId != -1) {

          val mahoutVec: Vector = value.get()
           //all vectors actually have the same number of values
          val s = mahoutVec.size()
          //TODO: only for testing!!!!!!
          for (i <- 0 to s-1) {
            val i = 0
            val weight = mahoutVec.get(i)
            esaMemoryIndexer.addResource(tokenId, (i, weight))
            //println ("added token: " + tokenStore.getTokenByID(tokenId) + " to the index...")
           }
         }
      }
    }
  }
    //esaMemoryIndexer.invertedIndex.topN(50)

    val resStore = MemoryStore.loadResourceStore(new FileInputStream(baseDir+"data/res.mem"))
    //Create wikipedia to DBpedia closure
    val wikipediaToDBpediaClosure = new WikipediaToDBpediaClosure(
      new FileInputStream(new File(baseDir+"raw_data/pig/redirects_en.nt")),
      new FileInputStream(new File(baseDir+"raw_data/pig/disambiguations_en.nt"))
    )
    //Now create the ESA index
    LOG.info("now for the vector index...")
    val dataMap: Iterator[(DBpediaResource, Array[Token], Array[Double])] =
    //TokenOccurrenceSource.fromJsonFile(new File("raw_data/json/top150-50000docs.json"),
    //TokenOccurrenceSource.fromJsonFile(new File("raw_data/json/token_counts-20120601-top150.json"),
      TokenOccurrenceSource.fromJsonFile(new File(baseDir+"raw_data/json/token_counts-top150-nofilter.json"),
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
          LOG.info("Made LSA vectors for %d resources".format(docCount))
        }

        var docIndex = new HashMap[Int, Double]()
        var i = 0

        //get the vector from the inverted index
        tokens.foreach {
          (tok: Token) => {
            val tokenWeight = weights(i)
            val tokensDocVector = esaMemoryIndexer.invertedIndex.getResources(tok)

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
        //now get the centroid - Update: looks as if we need to sort and keep only top N values, otherwise mem requirements are too high
        val noTokens = tokens.size
        docIndex.foreach {
          case (docId: Int, weight: Double) => {
            val avgWeight = weight / noTokens
            docIndex.put(docId, avgWeight)
          }
        }
        //TODO: TESTING - threshold hard-coded for now
        val topN = 150
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

    val sfStore = MemoryStore.loadSurfaceFormStore(new FileInputStream(baseDir+"data/sf.mem"))
    val cm = MemoryStore.loadCandidateMapStore(new FileInputStream(baseDir+"data/candmap.mem"), resStore)

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
      //new LinearRegressionMixture()
      new OnlySimScoreMixture()
    )

    //Milne-Witten
    val mw = MilneWittenCorpus.fromDirectory(new File(baseDir+"raw_data/MilneWitten-wikifiedStories"))
    val testSourceName = mw.name
    //val pw = new PrintWriter("%s-%s-%s.milne-witten.log".format(testSourceName,dName,EvalUtils.now()))

    val dName = disambiguator.name.replaceAll( """[.*[?/<>|*:\"{\\}].*]""", "_")

    //CSAW
    val csaw = CSAWCorpus.fromDirectory(new File(baseDir+"raw_data/csaw"))
    val testSourceName2 = csaw.name
    //val cs = new PrintWriter("%s-%s-%s.csaw.log".format(testSourceName2,dName,EvalUtils.now()))

    //Make the occ filters
    val redirectTCFileName = if (args.size > 1) args(1) else baseDir+"data/redirects_tc.tsv" //produced by ExtractCandidateMap
    val conceptURIsFileName  = if (args.size>2) args(2) else baseDir+"data/conceptURIs.list" //produced by ExtractCandidateMap
    val occFilters = List(UriWhitelistFilter.fromFile(new File(conceptURIsFileName)),RedirectResolveFilter.fromFile(new File(redirectTCFileName)))

    val tsvOut = new TSVOutputGenerator(new PrintWriter("%s-%s-%s.disambiguator.log".format(testSourceName2, dName, EvalUtils.now())))
    val outputs = List(tsvOut)

    //(2) create the EvaluateParagraphDisambiguator
    EvaluateParagraphDisambiguator.evaluate(csaw, disambiguator, outputs, occFilters)
    EvaluateParagraphDisambiguator.evaluate(mw, disambiguator, outputs, occFilters)
  }

}
