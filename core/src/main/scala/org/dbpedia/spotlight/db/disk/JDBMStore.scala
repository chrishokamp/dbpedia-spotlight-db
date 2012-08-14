package org.dbpedia.spotlight.db.disk

import net.kotek.jdbm.DBMaker
import java.lang.NoSuchFieldError

/**
 * @author Joachim Daiber
 */

class JDBMStore[A, B](databaseFile: String) {

  val db = DBMaker.openFile(databaseFile).enableHardCache().make()
  var data = Option(db.getHashMap[A, B]("data")) match {
    case None => db.createHashMap[A, B]("data")
    case Some(map) => map
  }

  def add(a: A, b: B) {
    data.put(a, b)
  }

  def commit() {
    db.commit()
  }

  //TODO - why is this returning NoSuchFieldError?
  def get(key: A): B = data.get(key)


  //Written for testing only - currently returns incorrect booleans (always true)
  def contains(key: A): Boolean = {
    val B = data.get(key)
    if (B == null) {
      false
    } else {
      true
    }
  }


}


