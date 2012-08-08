package org.dbpedia.spotlight.db.memory

import org.dbpedia.spotlight.db.model.{ContextStore, ResourceStore, EsaVectorStore}
import com.esotericsoftware.kryo.KryoSerializable
import scala.Array
import org.dbpedia.spotlight.model.{Token, DBpediaResource}

import scala.collection.JavaConversions._
import collection.mutable.HashMap
import collection.mutable

/**
 * @author Chris Hokamp
 */

@SerialVersionUID(1008001)
//TODO: remove abstract
class MemoryEsaVectorStore
  extends MemoryStore
  with EsaVectorStore
  /*with KryoSerializable*/ {

  //the tfidf weight for this term in each resource that it occurs in
  //var weights: Array[Array[Double]]
  //TODO: change to Map[Int, Map[Int, Double]]
  //    - Map[resId, Map[tokenId, Sum(Double)]
  var resources = new  mutable.HashMap[Int, mutable.Map[Int, Double]]

  def size = resources.size

  //used to go from id to resource name
  //may not be necessary
  @transient
  var resourceStore: ResourceStore = null

  def getDocVector(resource: DBpediaResource): mutable.Map[Int, Double] = {
    val id  = resource.id

    val resourceWeights = resources.getOrElse(id, new mutable.HashMap[Int, Double]())
    resourceWeights
  }

  def getDocVector(i: Int): mutable.Map[Int, Double] = {
    val resourceWeights = resources.getOrElse(i, mutable.HashMap[Int, Double]())
    resourceWeights
  }

  def addVector (resource: DBpediaResource, resourceWeights: mutable.Map[Int, Double]) {
    val id = resource.id
    resources.put(id, resourceWeights)
  }


}
