package db

import db.Repository.*
import db.mongo.{DatabaseContext, DataSource, MongoDatabaseInitializer}
import db.*
import model.Note.*
import model.{Note, User}
import org.mongodb.scala.*
import org.mongodb.scala.model.Filters.*
import org.mongodb.scala.result.{DeleteResult, InsertOneResult}
import zio.*
import zio.json.*

import java.time.Instant
import java.util.Date
import scala.collection.mutable.ListBuffer
import scala.util.Random

final case class NotesRepositoryLive(databaseContext: DataSource) extends NotesRepository {
  
  override def getById(id: Long): Task[Option[Note]] = for {
    db        <- databaseContext.getCtx.map(_.mongoDatabase.get)
    document  <- ZIO.fromFuture { implicit ec =>
      db.getCollection("notes")
        .find(equal("id", id))
        .first()
        .toFuture()
    }
    maybeNote <- ZIO.attempt(parseDocumentToNote(document))
  } yield maybeNote

  override def getAll: Task[List[Note]] = for {
    db        <- databaseContext.getCtx.map(_.mongoDatabase.get)
    documents <- ZIO.fromFuture { implicit ec =>
      db.getCollection("notes")
        .find()
        .toFuture()
    }
    notes     <- ZIO.attempt(parseDocumentsToNoteList(documents))
  } yield notes

  override def update(id: Long, newNote: Note): Task[UpdateStatus] = for {
    db        <- databaseContext.getCtx.map(_.mongoDatabase.get)
    queryResult  <- ZIO.fromFuture { implicit ec =>
      db.getCollection("notes")
        .replaceOne(equal("id", id), Document(newNote.toJson))
        .toFuture()
    }
    updateStatus <- queryResult.fold(queryResult.wasAcknowledged, s"Note with id '$id' has been updated successfully", s"Note with id '$id' has not been updated")
  } yield updateStatus

  override def delete(noteId: Long): Task[DeletionStatus] = for {
    db        <- databaseContext.getCtx.map(_.mongoDatabase.get)

    queryResult    <- ZIO.fromFuture { implicit ec =>
      db.getCollection("notes")
        .deleteOne(equal("id", noteId))
        .toFuture()
    }
    deletionStatus <- queryResult.fold(queryResult.wasAcknowledged, s"Note with id '$noteId' has been deleted", s"Note with id '$noteId' has not been deleted")
  } yield deletionStatus

  override def add(note: Note): Task[CreationStatus] = for {
    db        <- databaseContext.getCtx.map(_.mongoDatabase.get)
    noteWithId     <- ZIO.succeed(note.copy(id = Some(scala.util.Random.nextLong(Long.MaxValue))))
    queryResult    <- ZIO.fromFuture { implicit ec =>
      db.getCollection("notes")
        .insertOne(Document(noteWithId.toJson))
        .toFuture()
    }
    creationStatus <- queryResult.fold(queryResult.wasAcknowledged, "Note has been added successfully", "Note has not been added")
  } yield creationStatus

  override def getNotesByUserId(userId: Long): Task[List[Note]] = for {
    db        <- databaseContext.getCtx.map(_.mongoDatabase.get)
    documents <- ZIO.fromFuture { implicit ec =>
      db.getCollection("notes")
        .find(equal("userId", userId))
        .toFuture()
    }
    notes     <- ZIO.succeed(parseDocumentsToNoteList(documents))
  } yield notes

  override def getNoteByIdAndUserId(id: Long, userId: Long): Task[Option[Note]] = for {
    db        <- databaseContext.getCtx.map(_.mongoDatabase.get)
    document <- ZIO.fromFuture { implicit ec =>
      db.getCollection("notes")
        .find(and(equal("id", id), equal("userId", userId)))
        .first()
        .toFuture()
    }
    note     <- ZIO.attempt(parseDocumentToNote(document))
  } yield note

  override def deleteNoteByIdAndUserId(noteId: Long, userId: Long): Task[DeletionStatus] = for {
    db        <- databaseContext.getCtx.map(_.mongoDatabase.get)
    queryResult    <- ZIO.fromFuture { implicit ec =>
      db.getCollection("notes")
        .deleteOne(and(equal("id", noteId), equal("userId", userId)))
        .toFuture()
    }
    deletionStatus <- queryResult.fold(queryResult.wasAcknowledged, s"Note with id '$noteId' has been deleted", s"Note with id '$noteId' has not been deleted")
  } yield deletionStatus

  private def parseDocumentToNote(document: Document): Option[Note] = {
    Option(document).fold(None) { doc =>
      Some(
        Note(
          id        = doc("id").asInt64.getValue,
          title     = doc("title").asString.getValue,
          body      = doc("body").asString.getValue,
          createdAt = doc("createdAt").asString.getValue,
          userId    = doc("userId").asInt64.getValue
        )
      )
    }
  }

  private def parseDocumentsToNoteList(documents: Seq[Document]) = documents.map(parseDocumentToNote).toList.flatten
  
}

object NotesRepositoryLive {
  
  lazy val layer: URLayer[DataSource, NotesRepository] = ZLayer.fromFunction(NotesRepositoryLive.apply _)

}