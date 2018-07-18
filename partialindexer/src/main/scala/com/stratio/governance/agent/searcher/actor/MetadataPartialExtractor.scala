package com.stratio.governance.agent.searcher.actor

import java.sql.{Connection, ResultSet, Statement}

import akka.actor.{Actor, ActorRef}
import akka.pattern.ask
import akka.util.Timeout
import com.stratio.governance.agent.searcher.actor.MetadataPartialExtractor.Chunks
import com.stratio.governance.agent.searcher.model.es.EntityRowES
import com.stratio.governance.agent.searcher.model.utils.KeyValuePairMapping
import com.stratio.governance.agent.searcher.model.{DatabaseSchema, FileTable, KeyValuePair}
import com.typesafe.config.Config
import org.apache.commons.dbcp.DelegatingConnection
import org.json4s.DefaultFormats
import org.postgresql.{PGConnection, PGNotification}
import org.slf4j.{Logger, LoggerFactory}
import scalikejdbc._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object MetadataPartialExtractor {
  case class Chunks(l: List[Array[PGNotification]])
}


class MetadataPartialExtractor(indexer: ActorRef, override val circuitBreakerConfig: Config)
  extends Actor with CircuitBreakerConfig{

  private lazy val LOG: Logger = LoggerFactory.getLogger(getClass.getName)
  implicit val timeout =
    Timeout(2000, MILLISECONDS)
  implicit val formats = DefaultFormats

  // execution context for the notifications
  context.dispatcher

  val connection: Connection = ConnectionPool.borrow()
  val db: DB = DB(connection)
  val postgresNotification = context.system.scheduler.schedule(1000 millis, 5000 millis, self, "postgresNotification")

  //implicit val formats = DefaultFormats

  circuitBreaker.onOpen(setEOnErrorState())
    .onClose(backToClosedState)
    .onHalfOpen(backToNormalState())

  def setEOnErrorState(): Unit = {
    LOG.error("Circuit breaker open")
    context.become(error)
  }

  def backToNormalState(): Unit = {
    LOG.debug("Circuit breaker half Open")
    context.unbecome()
  }

  def backToClosedState(): Unit = {
    LOG.debug("Circuit breaker Closed")
    context.unbecome()
  }


  override def preStart() = {
    // make sure connection isn't closed when executing queries
    // we setup the
    db.autoClose(false)
    db.localTx { implicit session =>
      session.connection.setAutoCommit(false)
      //session.connection.createStatement().setFetchSize(1000)
      val stmt: Statement = session.connection.createStatement(ResultSet.CONCUR_READ_ONLY,
        ResultSet.FETCH_FORWARD,
        ResultSet.TYPE_FORWARD_ONLY)//.setFetchSize(1000)//.execute("LISTEN events")
      stmt.setFetchSize(1000)
      stmt.execute("LISTEN events")
    }
  }

  override def postStop() = {
    postgresNotification.cancel()
    db.close()
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

  def receive = {
    case "postgresNotification" => {
      db.readOnly { implicit session =>
        val pgConnection = connection.asInstanceOf[DelegatingConnection].getInnermostDelegate.asInstanceOf[PGConnection]
        //TODO revisar:

        pgConnection.setDefaultFetchSize(1000)

        println(s"FETCH SIZE :::::::::::::: ${pgConnection.getDefaultFetchSize}")

        pgConnection.setPrepareThreshold(1000)
        val notifications: Array[PGNotification] = Option(pgConnection.getNotifications).getOrElse(Array[PGNotification]())

        if (notifications.nonEmpty) {
          self ! Chunks(notifications.grouped(1000).toList)
        }else{
          self ! "postgresNotification"
        }


      }

    }

    case Chunks(list) =>

      if(list.nonEmpty){
        indexer ? PartialIndexer.IndexerEvent(list.head)
        self ! Chunks(list.tail)
      } else {
        self ! "postgresNotification"
      }
  }


  def error: Receive = {
    case msg: AnyRef => LOG.debug(s"Actor in error state no messages processed: ${msg.getClass.getCanonicalName}")
  }

  object MetadataPartialExtractor
}