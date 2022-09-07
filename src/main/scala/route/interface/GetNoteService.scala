package route.interface

import db._
import model.Note
import zio.*

trait GetNoteService:
  
  def getNote(noteId: Long, userId: Long): Task[Either[String, Note]]

