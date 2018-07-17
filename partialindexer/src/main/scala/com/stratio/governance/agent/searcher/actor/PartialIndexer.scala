package com.stratio.governance.agent.searcher.actor

import java.sql.{Connection, ResultSet}

import akka.actor.Actor
import akka.pattern._
import com.stratio.governance.agent.searcher.actor.PartialIndexer.IndexerEvent
import com.stratio.governance.agent.searcher.http.HttpRequester
import com.stratio.governance.agent.searcher.model.es.{DatastoreEngineES, EntityRowES}
import com.stratio.governance.agent.searcher.model.utils.KeyValuePairMapping
import com.stratio.governance.agent.searcher.model.{DatabaseSchema, DatastoreEngine, FileTable, KeyValuePair}
import org.json4s.native.JsonMethods.parse
import org.json4s.{DefaultFormats, JValue}
import org.postgresql.PGNotification
import scalikejdbc.ConnectionPool

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PartialIndexer extends Actor {

  val connection: Connection = ConnectionPool.borrow()
  implicit val formats = DefaultFormats

  override def receive: Receive = {
    case IndexerEvent(chunk) => {

      val lista: Array[EntityRowES] = chunk.map(not => {

        // convert to object
        implicit val formats = DefaultFormats
        val json: JValue = parse(not.getParameter) \\ "data"
        val table: JValue = parse(not.getParameter) \\ "table"
        val requestToIndexer: EntityRowES = table.values match {
          case ("datastore_engine") => {
            val datastoreEngine: DatastoreEngine = json.extract[DatastoreEngine]
            val datastoreEngineES = Seq(DatastoreEngineES.fromDatastoreEngine(datastoreEngine))
            datastoreEngineES.head
          }
          case ("database_schema") => {
            val databaseSchema: DatabaseSchema = json.extract[DatabaseSchema]
            databaseSchemaProcess(databaseSchema)
          }
          /*
                      case ("file_table") => {
                        val fileTable: FileTable = json.extract[FileTable]
                        fileTableProcess(fileTable)
                      }
          */
          case ("key_value_pair") => {
            val keyValuePair = json.extract[KeyValuePair]
            keyValuePairProcess(keyValuePair)
          }
          case _ => null  //TODO errors management
        }
        requestToIndexer

      }
      )

      val documentsBulk: String = org.json4s.native.Serialization.write(lista)
      Future(HttpRequester.postRequest(documentsBulk))

    } pipeTo sender

  }


  def keyValuePairProcess(keyValuePair: KeyValuePair) = {

    val parentId = keyValuePair.parent_id
    val parentType = keyValuePair.parent_type
    val parentTable = KeyValuePairMapping.parentTable(parentType)

    val statement = connection.createStatement
    val resultSet: ResultSet = statement.executeQuery(s"select * from dg_metadata.$parentTable as parent " +
      s"join dg_metadata.key_value_pair as kvp on parent.id = kvp.parent_id and parent_type = $parentType " +
      s"where parent.id = $parentId")
    val entity: Seq[EntityRowES] = KeyValuePairMapping.entityFromResultSet(parentType, resultSet)
    resultSet.close
    statement.close

    entity.head

  }

  def databaseSchemaProcess(databaseSchema: DatabaseSchema) = {

    val parentId = databaseSchema.id
    val parentType = KeyValuePairMapping.parentType(DatabaseSchema.entity)

    val statement = connection.createStatement
    val resultSet: ResultSet = statement.executeQuery(s"select * from dg_metadata.key_value_pair " +
      s"where parent_id = $parentId and parent_type = $parentType")

    val entity: Seq[EntityRowES] = DatabaseSchema.entityFromResultSet(databaseSchema, resultSet)
    /*
        resultSet.close
        statement.close
    */

    entity.head

  }

  def fileTableProcess(fileTable: FileTable) = {

    val parentId = fileTable.id
    val parentType = KeyValuePairMapping.parentType(FileTable.entity)

    val statement = connection.createStatement
    val resultSet: ResultSet = statement.executeQuery(s"select * from dg_metadata.key_value_pair " +
      s"where parent_id = $parentId and parent_type = $parentType")

    val entity: Seq[EntityRowES] = FileTable.entityFromResultSet(fileTable, resultSet)
    resultSet.close
    statement.close

    org.json4s.native.Serialization.write(entity)

  }


}





object PartialIndexer {

  /**
    * Default Actor name
    */
  lazy val NAME = "Partial Indexer"

  /**
    * Actor messages
    */
  case class IndexerEvent(notificationChunks: Array[PGNotification])

}
