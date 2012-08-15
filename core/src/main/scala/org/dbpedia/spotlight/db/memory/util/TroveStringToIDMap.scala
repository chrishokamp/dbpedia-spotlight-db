package org.dbpedia.spotlight.db.memory.util

import gnu.trove.TObjectIntHashMap
import java.lang.Integer

class TroveStringToIDMap(expectedSize: Int) extends java.util.AbstractMap[String, Integer] {
  val impl = new TObjectIntHashMap(expectedSize)

  override def isEmpty = impl.isEmpty

  override def size = impl.size()

  override def containsKey(key: Object) = impl.containsKey(key)

  override def containsValue(value: Object) = impl.containsValue(value.asInstanceOf[Int])

  override def get(key: Object): Integer = {
    impl.get(key) match {
      case value: Int if value > 0 => value
      case _ => null
    }
  }

  override def put(key: String, value: Integer) = impl.put(key, value)

  override def remove(key: Object) = impl.remove(key)

  override def clear() {
    impl.clear()
  }

  def entrySet = null

  override def keySet: java.util.Set[String] = null

  override def values: java.util.Collection[Integer] = null
}
