package org.dbpedia.spotlight.db

import org.dbpedia.spotlight.db.MemoryStoreIndexer
import java.io.File
import org.dbpedia.spotlight.model._
import org.dbpedia.spotlight.db.model.EsaVectorStore
import org.dbpedia.spotlight.db.memory.{MemoryStore, MemoryDocFreqStore, MemoryEsaVectorStore}

import org.apache.commons.lang.NotImplementedException
import collection.mutable

/**
 * @author Chris Hokamp
 */


//TODO: remove abstract
class EsaStoreIndexer(override val baseDir: File)
  extends MemoryStoreIndexer(baseDir)
  with InvertedIndexIndexer
  with DocOccurrenceIndexer {
  /*This class adds indexing of tfidf vectors for each doc
  * Building tfidf vectors can only occur AFTER the TokenOccurences have been indexed (in a ContextStore)
  * (1) for a doc, calculate tfidf for each token
  * (2) after tfidf has been calculated, add [token, [docId, tfIdfValue]] to the index for that token
  *    - interface is org.spotlight.db.model.EsaVectorStore
  *    - implementing class is org.spotlight.db.memory.MemoryEsaVectorStore
  * Load context store
  * Create EsaVectorStore
  * Dump EsaVectorStore
  * The Disambiguator will load it
  * i.e.  val contextStore = MemoryStore.loadContextStore(new FileInputStream("data/context.mem"), tokenStore)
  */

  //doc (resource) occurrences
  def addDocOccurrence(resource: DBpediaResource, token: Token, weight: Double) {
    throw new NotImplementedException()
  }

  //TODO: remove abstract before uncommenting
  lazy val vectorStore = new MemoryEsaVectorStore()

  def addDocOccurrence(resource: DBpediaResource, resourceWeights: mutable.Map[Int, Double]) {
    val id = resource.id
    vectorStore.resources.put(id, resourceWeights)
  }

  def createEsaVectorStore(n: Int) {
    throw new NotImplementedException()
    //vectorStore.resources = new Array[Array[Int]](n)
    //vectorStore.weights = new Array[Array[Double]](n)
  }

  def addDocOccurrences(occs: Map[Token, Map[Int, Double]]) {
    throw new NotImplementedException()
    /*occs.foreach {case (tok, docWeights) => {
      val (j, w) = docWeights.unzip
      vectorStore.resources(tok.id) = j.toArray
      vectorStore.weights(tok.id) = w.toArray
    }
    } */

    def writeDocOccurrences() {
      throw new NotImplementedException()
    }

  }


}
