package org.dbpedia.spotlight.db.disk

import org.dbpedia.spotlight.db.model.InvertedIndexStore
import org.dbpedia.spotlight.model.Token
import org.dbpedia.spotlight.exceptions.DBpediaResourceNotFoundException


/**
 * @author Chris Hokamp
 */

class DiskInvertedIndexStore(file: String) /*extends InvertedIndexStore*/ {

  val jdbm = new JDBMStore[Int, Map[Int, Double]](file)

  def getResources (token: Token): Map[Int,Double] = {
      val resources = jdbm.get(token.id)
      if (resources == null)
        throw new DBpediaResourceNotFoundException("Token not found: " + token.name)

      resources
  }

  def getDocFreq (token: Token): Int = {
    val resources = jdbm.get(token.id)
    if (resources == null) {
      0
    } else {
      resources.size
    }
  }

    //TEST
    def printAll(i: Int) {
      val testMap = jdbm.get(i)
      testMap.foreach {
        case (k: Int, v: Double) => {
          println ("k is : " +k + " v is: " + v)
        }
      }
    }

}
