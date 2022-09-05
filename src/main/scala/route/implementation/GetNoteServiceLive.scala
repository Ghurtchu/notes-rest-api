package route.implementation

import db._
import model.Note
import route.interface.GetNoteService
import zhttp.http.Response
import zio.*
import zio.json.*

final case class GetNoteServiceLive(private final val notesRepository: NotesRepository) extends GetNoteService {

  override def getNote(noteId: Long, userId: Long): Task[Either[DbError.NotFound, Note]] = for {
    maybeNote <- notesRepository.getNoteByIdAndUserId(noteId, userId)
    response  <- ZIO.succeed(maybeNote.fold(Left(DbError.NotFound(s"Could not find the note with id ${noteId.withQuotes}")))(Right(_)))
  } yield response
}

object GetNoteServiceLive {
  
  lazy val layer: URLayer[NotesRepository, GetNoteService] = 
    ZLayer.fromFunction(GetNoteServiceLive.apply)
  
}
