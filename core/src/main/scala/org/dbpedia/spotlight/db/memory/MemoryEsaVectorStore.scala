package org.dbpedia.spotlight.db.memory

import org.dbpedia.spotlight.db.model.{ContextStore, ResourceStore, EsaVectorStore}
import com.esotericsoftware.kryo.KryoSerializable
import scala.Array
import org.dbpedia.spotlight.model.{Token, DBpediaResource}
import java.util.{Map, HashMap}
import scala.collection.JavaConversions._

/**
 * @author Chris Hokamp
 */

@SerialVersionUID(1008001)
//TODO: remove abstract
abstract class MemoryEsaVectorStore
  extends MemoryStore
  with EsaVectorStore
  with KryoSerializable {

  //The resource set
  var resources: Array[Array[Int]]
  //the tfidf weight for this term in each resource that it occurs in
  var weights: Array[Array[Double]]

  def size = resources.length

  //used to go from id to resource name
  //may not be necessary
  @transient
  var resourceStore: ResourceStore = null

  def getResourceWeights(token: Token): Map[DBpediaResource, Double] = {

    val resourceWeights = new HashMap[DBpediaResource, Double]
    val id  = token.id

    if (resources(id) != null) {
      val r = resources(id)
      val w = weights(id)

      (0 to r.length-1) foreach { j =>
        resourceWeights.put(resourceStore.getResource(r(j)), w(j))
      }
    }
    resourceWeights
  }

}
