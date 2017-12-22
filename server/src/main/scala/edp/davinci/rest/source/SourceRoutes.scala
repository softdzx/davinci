

package edp.davinci.rest.source

import java.sql.Connection
import javax.ws.rs.Path

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.{Directives, Route}
import edp.davinci.module.{BusinessModule, ConfigurationModule, PersistenceModule, RoutesModuleImpl}
import edp.davinci.persistence.entities._
import edp.davinci.rest._
import edp.davinci.util.{AuthorizationProvider, JsonUtils, SqlUtils}
import edp.davinci.util.JsonProtocol._
import edp.davinci.util.ResponseUtils.getHeader
import io.swagger.annotations._
import org.apache.log4j.Logger
import edp.davinci.util.ResponseUtils._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}


@Api(value = "/sources", produces = "application/json")
@Path("/sources")
class SourceRoutes(modules: ConfigurationModule with PersistenceModule with BusinessModule with RoutesModuleImpl) extends Directives {

  val routes: Route = getSourceByAllRoute ~ postSourceRoute ~ putSourceRoute ~ deleteSourceByIdRoute ~ testConnection
  private lazy val logger = Logger.getLogger(this.getClass)
  private lazy val routeName = "sources"

  @ApiOperation(value = "get all source with the same domain", notes = "", nickname = "", httpMethod = "GET")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "active", value = "true or false", required = false, dataType = "boolean", paramType = "query")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "OK"),
    new ApiResponse(code = 403, message = "user is not admin"),
    new ApiResponse(code = 404, message = "sources not found"),
    new ApiResponse(code = 401, message = "authorization error"),
    new ApiResponse(code = 400, message = "bad request")
  ))
  def getSourceByAllRoute: Route = path(routeName) {
    get {
      parameter('active.as[Boolean].?) { active =>
        authenticateOAuth2Async[SessionClass](AuthorizationProvider.realm, AuthorizationProvider.authorize) {
          session => getAllSourcesComplete(session, active.getOrElse(true))
        }
      }
    }
  }

  private def getAllSourcesComplete(session: SessionClass, active: Boolean): Route = {
    if (session.admin) {
      onComplete(SourceService.getAll(session)) {
        case Success(sourceSeq) =>
          complete(OK, ResponseSeqJson[PutSourceInfo](getHeader(200, session), sourceSeq))
        case Failure(ex) => logger.error("getAllSourcesComplete error", ex)
          complete(BadRequest, ResponseJson[String](getHeader(400, ex.getMessage, session), ""))
      }
    } else complete(Forbidden, ResponseJson[String](getHeader(403, session), ""))
  }


  @ApiOperation(value = "Add new sources to the system", notes = "", nickname = "", httpMethod = "POST")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "sources", value = "Source objects to be added", required = true, dataType = "edp.davinci.persistence.entities.PostSourceInfoSeq", paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "post success"),
    new ApiResponse(code = 403, message = "user is not admin"),
    new ApiResponse(code = 401, message = "authorization error"),
    new ApiResponse(code = 400, message = "bad request")
  ))
  def postSourceRoute: Route = path(routeName) {
    post {
      entity(as[PostSourceInfoSeq]) {
        sourceSeq =>
          authenticateOAuth2Async[SessionClass](AuthorizationProvider.realm, AuthorizationProvider.authorize) {
            session => postSource(session, sourceSeq.payload)
          }
      }
    }
  }


  private def postSource(session: SessionClass, postSourceSeq: Seq[PostSourceInfo]): Route = {
    if (session.admin) {
      val sourceSeq = postSourceSeq.map(post => Source(0, post.name, post.connection_url, post.desc, post.`type`, post.config, active = true, currentTime, session.userId, currentTime, session.userId))
      onComplete(modules.sourceDal.insert(sourceSeq)) {
        case Success(sourceWithIdSeq) =>
          val responseSourceSeq = sourceWithIdSeq.map(source => PutSourceInfo(source.id, source.name, source.connection_url, source.desc, source.`type`, source.config))
          complete(OK, ResponseSeqJson[PutSourceInfo](getHeader(200, session), responseSourceSeq))
        case Failure(ex) => logger.error("getAllSourcesComplete error", ex)
          complete(BadRequest, ResponseJson[String](getHeader(400, ex.getMessage, session), ""))
      }
    } else complete(Forbidden, ResponseJson[String](getHeader(403, session), ""))
  }


  @ApiOperation(value = "update sources in the system", notes = "", nickname = "", httpMethod = "PUT")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "sources", value = "Source objects to be updated", required = true, dataType = "edp.davinci.persistence.entities.PutSourceInfoSeq", paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "put success"),
    new ApiResponse(code = 403, message = "user is not admin"),
    new ApiResponse(code = 404, message = "sources not found"),
    new ApiResponse(code = 401, message = "authorization error"),
    new ApiResponse(code = 400, message = "bad request")
  ))
  def putSourceRoute: Route = path(routeName) {
    put {
      entity(as[PutSourceInfoSeq]) {
        sourceSeq =>
          authenticateOAuth2Async[SessionClass](AuthorizationProvider.realm, AuthorizationProvider.authorize) {
            session => putSourceComplete(session, sourceSeq.payload)
          }
      }
    }
  }

  private def putSourceComplete(session: SessionClass, sourceSeq: Seq[PutSourceInfo]): Route = {
    if (session.admin) {
      val future = SourceService.update(sourceSeq, session)
      onComplete(future) {
        case Success(_) => complete(OK, ResponseJson[String](getHeader(200, session), ""))
        case Failure(ex) => logger.error("putSourceComplete error", ex)
          complete(BadRequest, ResponseJson[String](getHeader(400, ex.getMessage, session), ""))
      }
    } else complete(Forbidden, ResponseJson[String](getHeader(403, session), ""))
  }


  @Path("/{id}")
  @ApiOperation(value = "delete source by id", notes = "", nickname = "", httpMethod = "DELETE")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "id", value = "source id ", required = true, dataType = "integer", paramType = "path")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "delete success"),
    new ApiResponse(code = 403, message = "user is not admin"),
    new ApiResponse(code = 401, message = "authorization error"),
    new ApiResponse(code = 400, message = "bad request")
  ))
  def deleteSourceByIdRoute: Route = path(routeName / LongNumber) { sourceId =>
    delete {
      authenticateOAuth2Async[SessionClass]("davinci", AuthorizationProvider.authorize) {
        session =>
          if (session.admin) {
            onComplete(SourceService.deleteSource(sourceId, session)) {
              case Success(_) => complete(OK, ResponseJson[String](getHeader(200, session), ""))
              case Failure(ex) => logger.error("deleteSourceByIdRoute error", ex)
                complete(BadRequest, ResponseJson[String](getHeader(400, ex.getMessage, session), ""))
            }
          } else complete(Forbidden, ResponseJson[String](getHeader(403, session), ""))
      }
    }
  }

  @Path("/test_connection")
  @ApiOperation(value = "test connection", notes = "", nickname = "", httpMethod = "POST")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "url", value = "connection url ", required = true, dataType = "string", paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "delete success"),
    new ApiResponse(code = 403, message = "user is not admin"),
    new ApiResponse(code = 401, message = "authorization error"),
    new ApiResponse(code = 400, message = "bad request")
  ))
  def testConnection: Route = path(routeName / "test_connection") {
    post {
      entity(as[String]) { jsonConfig =>
        authenticateOAuth2Async[SessionClass]("davinci", AuthorizationProvider.authorize) { session =>
          val connectionUrl = JsonUtils.json2caseClass[SourceConfig](jsonConfig)
          val testQuery = if (connectionUrl.url.toLowerCase.indexOf("oracle") > -1) "select 1 from dual" else "select 1"
          var connection: Connection = null
          try {
            connection = SqlUtils.getConnection(connectionUrl.url, connectionUrl.user, connectionUrl.password)
            if (!connection.isClosed)
              complete(OK, ResponseJson[String](getHeader(200, session), ""))
            //            val statement = connection.createStatement()
            //            if (statement.execute(testQuery))

            else {
              logger.error("testConnection error<<<<<<<<<<<<<<<<<<<<<<<<<<<")
              complete(BadRequest, ResponseJson[String](getHeader(400, session), "execute test query error"))
            }
          } catch {
            case e: Throwable =>
              logger.error("test connection exception", e)
              complete(BadRequest, ResponseJson[String](getHeader(400, e.getMessage, session), ""))
          } finally {
            if (null != connection)
              connection.close()
          }
        }
      }
    }
  }

}
