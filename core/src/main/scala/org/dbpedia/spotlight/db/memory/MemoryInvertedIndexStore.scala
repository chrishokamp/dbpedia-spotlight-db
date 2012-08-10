package org.dbpedia.spotlight.db.memory

import org.dbpedia.spotlight.db.model.{DocFrequencyStore, InvertedIndexStore}
import org.dbpedia.spotlight.model.Token
import collection.mutable
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Output

/**
 * @author Chris Hokamp
 */
//TODO: remove abstract
class MemoryInvertedIndexStore
  extends MemoryStore
  with InvertedIndexStore
  with DocFrequencyStore {

  //creating new instances could be problematic with serialization
  var docFreq = new mutable.HashMap[Int, Int]
  var index = new mutable.HashMap[Int, mutable.HashMap[Int, Double]]


  def size =  docFreq.size

  def getDocFreq (token: Token):  Int = {
    val i = token.id
    docFreq.getOrElse(i, 100000) //to avoid div by zero
  }

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



}
