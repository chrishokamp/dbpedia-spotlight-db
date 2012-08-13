package org.dbpedia.spotlight.db.memory

import org.dbpedia.spotlight.db.model.{DocFrequencyStore, InvertedIndexStore}
import org.dbpedia.spotlight.model.Token
import collection.mutable
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Output
import collection.mutable.ListBuffer
import org.dbpedia.spotlight.db.disk.JDBMStore

/**
 * @author Chris Hokamp
 */

class MemoryInvertedIndexStore
  extends MemoryStore
  with InvertedIndexStore
  with DocFrequencyStore {

  //creating new instances could be problematic with serialization
  var docFreq = new mutable.HashMap[Int, Int]
  var index = new mutable.HashMap[Int, mutable.HashMap[Int, Double]]

  //TODO: testing here - vals in array index with limited size - i.e. 25
  var docs: Array[ListBuffer[(Int,Double)]] = null //tokenId[docIds]
  //var weights: Array[ListBuffer[Double]] = null //tokenId[weightsForEachDoc]
  //TODO: add the docFreq index
  def size =  docFreq.size


  //TODO: change to map (not mutable.HashMap)
  def getResources (token: Token): mutable.HashMap[Int, Double] = {

    val docWeights = new mutable.HashMap[Int, Double]()
    val id = token.id
    if (docs(id) != null) {
      val d = docs(id)
      d.foreach {
        case (doc: Int, weight: Double) => {
          docWeights.put(doc, weight)
        }
      }
    }
    docWeights
  }

  //TODO: this shouldn't be in this object - move to indexer
  def addAll (tokenId: Int, documents: mutable.HashMap[Int, Double]) {

    var pos = 0
    documents.foreach {
      case (i: Int, w: Double) => {
        docs(tokenId).append((i, w))

        pos += 1
      }
    }
  }

  //TODO: still uses the hash
  def getDocFreq (token: Token):  Int = {
    val i = token.id
    docFreq.getOrElse(i, 100000) //to avoid div by zero
  }

  //Sort each list by weight, and keep only top N tokens
  def topN (noToKeep: Int) {
    var i = 0
    docs.foreach {
      (map: ListBuffer[(Int,Double)]) => {
        if (map != null && map.length > noToKeep) {
          var sorted = map.sortWith(_._2 > _._2)
          sorted = sorted.dropRight(noToKeep)
          docs(i) = sorted
        }
        i += 1
      }

    }
  }

  //TODO: TESTING HERE
  def persistIndex (diskMapLocation: String) {

    val diskMap = new JDBMStore[Int, Map[Int, Double]](diskMapLocation)
    var tokenId = 0

      docs.foreach {
      (docVec: ListBuffer[(Int,Double)]) => {
        if (docVec != null) {
          //println("tokenId is: " + tokenId)
          val asMap = Map(docVec map {doc => (doc._1, doc._2)} : _*)

          diskMap.add(tokenId, asMap)

          /* //TEST
          asMap.foreach {
            case (docI: Int, v: Double) => {
              println("docId: " + docI + " value: " + v)
            }
          }
          */
        }
      }
      tokenId += 1
    }
    diskMap.commit()

  }

  /* COMMENTING WHILE TESTING ARRAY INDEX
  def getResources (token: Token): mutable.HashMap[Int, Double] = {

     val id = token.id
     val resSet= index.get(id)
     resSet match {
       case None => null
       case Some(x) => val vect = x
       vect
     }
  }

  def add (tokenId: Int, docs: mutable.HashMap[Int, Double]) {
    index.put(tokenId, docs)
  }
  */



}
