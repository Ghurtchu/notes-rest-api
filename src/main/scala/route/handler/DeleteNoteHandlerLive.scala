package route.handler

import route.implementation
import route.interface._
import route.implementation.DeleteNoteServiceLive
import zhttp.http.Response
import zio.*
import zhttp.http._
import db.NotesRepositoryLive


trait DeleteNoteHandler:
  def handle(noteId: Long, userId: Long): Task[Response]


final case class DeleteNoteHandlerLive(deleteNoteService: DeleteNoteService) extends DeleteNoteHandler:

  override def handle(noteId: Long, userId: Long): Task[Response] = 
    deleteNoteService.deleteRecord(noteId, userId)
      .map(_.fold(Response.text, Response.text))
    


object DeleteNoteHandlerLive:
  
  lazy val layer: URLayer[DeleteNoteService, DeleteNoteHandler] = ZLayer.fromFunction(DeleteNoteHandlerLive.apply)
