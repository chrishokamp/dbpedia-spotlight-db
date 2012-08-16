package org.dbpedia.spotlight.db.memory

import org.dbpedia.spotlight.db.model.{ResourceStore, DocFrequencyStore, InvertedIndexStore}
import org.dbpedia.spotlight.model.Token
import collection.mutable
import com.esotericsoftware.kryo.{KryoSerializable, KryoException, Kryo}
import com.esotericsoftware.kryo.io.{Input, Output}
import collection.mutable.ListBuffer
import org.dbpedia.spotlight.db.disk.JDBMStore
import org.apache.commons.lang.NotImplementedException
import org.apache.commons.logging.LogFactory

/**
 * @author Chris Hokamp
 */

@SerialVersionUID(1001001)
class MemoryInvertedIndexStore
  extends MemoryStore
  with InvertedIndexStore
  with DocFrequencyStore
  with KryoSerializable {

  //TODO: remove this ASAP - get size directly from index
  //var docFreq = new mutable.HashMap[Int, Int]

  //TODO: testing here - vals in array index with limited size - i.e. 25
  @transient
  var docs: Array[ListBuffer[(Int,Double)]] = null //tokenId[docIds]
  //var weights: Array[ListBuffer[Double]] = null //tokenId[weightsForEachDoc]
  //TODO: add the docFreq index
  def size =  docs.size
  //TODO: Serialize this using Kryo with custom reader/writer
  //(1) make a map [token, Map[docId:Int, weight:Double]]
  //(2) serialize into arrays by doing map.unzip --> (a, b) a.toArray, b.toArray
  //Working...
  //***Note: this is currently only useful as a way to persist the index
  @transient
  var resStore: ResourceStore = null

  var resources: Array[Array[Int]] = null
  var weights: Array[Array[Double]] = null


  //*Note - see MemoryStoreIndexer.addTokenOccurrences for example
  /*
  // this is for testing with the disk-backed store...
  def getResources (token: Token): Map[Int, Double] = {
    throw new NotImplementedException()
  }
  */


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

  //TODO: the vectors are truncated, so each token has the same df right now (i.e. 25)
  def getDocFreq (token: Token):  Int = {
    val i = token.id
    docs(i).size
  }

  //Sort each list by weight, and keep only top N tokens
  def topN (noToKeep: Int) {
    var i = 0
    docs.foreach {

      (map: ListBuffer[(Int,Double)]) => {
        if (map != null) {
          //TODO: temporary! add doc freq here
          //docFreq.put(i, map.length)
          if(map.length >= noToKeep) {
            val sorted = map.sortBy(_._2)
            val truncated = sorted.drop(sorted.length-noToKeep)
            docs(i) = truncated
          } else {
            docs(i) = null //Test - set this doc to null if it is too short
          }
        }
      }
      i += 1
    }
  }

  //TODO: test this
  def docsToNestedArrays {
    //initialize the array indexes
    resources = new Array[Array[Int]](docs.size)
    weights = new Array[Array[Double]](docs.size)

    var i = 0
    docs.foreach { case (listVector: ListBuffer[(Int, Double)]) => {
      if (listVector != null) {

        val subLen = listVector.length
        val resArray = new Array[Int](subLen)
        val weightArray = new Array[Double](subLen)

        var j = 0
        listVector.foreach { case (resId: Int, resWeight: Double) => {
          resArray(j) = resId
          weightArray(j) = resWeight
          j += 1
        }
        }
        resources(i) = resArray
        weights(i) = weightArray
      }

    }
      i += 1
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
      if (tokenId % 10000 == 0) {
        LOG.info("Persisted %d tokens...".format(tokenId))
        diskMap.commit()
      }
    }
    diskMap.commit()

  }

  /*
  def getResources (token: Token): mutable.HashMap[Int, Double] = {

     val id = token.id
     val resSet= index.get(id)
     resSet match {
       case None => null
       case Some(x) => val vect = x
       vect
     }
  }
  */

  /*
  def add (tokenId: Int, docs: mutable.HashMap[Int, Double]) {
    index.put(tokenId, docs)
  }
  */

  //TODO: finish serialization!
  def write(kryo: Kryo, output: Output) {
    output.writeInt(resources.length)

    (0 to resources.length-1).foreach { i =>
      if (resources(i) == null) {
        output.writeInt(0)
      } else {
        output.writeInt(resources(i).length)

        (0 to resources(i).length-1).foreach { j =>
          output.writeInt(resources(i)(j))
        }
        (0 to resources(i).length-1).foreach { j =>
          output.writeDouble(weights(i)(j))
        }
      }
    }
    output.writeChar('#')
  }
  //TODO: implement 'read'
  def read(kryo: Kryo, input: Input) {
    val size = input.readInt()

    resources = new Array[Array[Int]](size)
    weights = new Array[Array[Double]](size)

    var i = 0
    var j = 0

    while (i < size) {
      val subsize = input.readInt()

      if (subsize > 0) {
        resources(i) = new Array[Int](subsize)
        weights(i) = new Array[Double](subsize)

        j = 0
        while (j < subsize) {
          resources(i)(j) = subsize
          j += 1
        }

       j = 0
       while (j < subsize) {
         weights(i)(j) = input.readDouble()
         j += 1
       }
      }

      i += 1
     }
   if(input.readChar() != '#')
      throw new KryoException("Error deserializing InvertedIndex store")

  }

}
