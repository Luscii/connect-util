package nl.focuscura.connect.util

import java.io.File

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.util.ByteString
import org.asynchttpclient.AsyncHttpClientConfig
import play.api.libs.json.JsValue
import play.api.libs.ws._
import play.api.libs.ws.ahc.{AhcConfigBuilder, AhcWSClient, AhcWSClientConfig}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.xml.Elem
/**
  * Created by sande on 7/11/2016.
  */
case class BasicAuthCredentials(username: String, password: String)

//Currently only implements POST, extend when other calls are needed.
trait WebServiceClient {

  def get(url: String,
          basicAuthCredentials: Option[BasicAuthCredentials]= None,
          timeOut:Duration = 3 seconds,
          headers: Seq[(String, String)] = List[(String, String)]()
         )  : Future[WSResponse]

  def post(
            url: String,
            body: String,
            basicAuthCredentials: Option[BasicAuthCredentials] = None,
            timeOut:Duration = 3 seconds,
            headers: Seq[(String, String)] = List[(String, String)]()
          ): Future[WSResponse]

  def postAsJson[T](url: String,
                    body: T,
                    basicAuthCredentials: Option[BasicAuthCredentials] = None,
                    timeOut:Duration = 3 seconds,
                    headers: Seq[(String, String)] = List[(String, String)]()
                   ): Future[WSResponse] = {

    val headersMustIncludeContentType = if (!headers.exists(elem => elem._1 == "Content-Type")) {
      headers :+("Content-Type", "application/json")
    }
    else { headers }

    post(url, JacksonWrapper.serialize(body), basicAuthCredentials, timeOut, headersMustIncludeContentType)
  }

  def isSuccessStatusCode(response: Future[WSResponse]): Future[Boolean] = {

    response map (response =>
    {

      response.status >= 200 && response.status < 300
    })
  }
}

object DummyApiWebServiceClient {
  def createStatusResponse(statusCode:Int): Future[WSResponse] = {
    Future(new WSResponse {
      override def status: Int = statusCode

      override def statusText: String = ???

      override def underlying[T]: T = ???

      override def xml: Elem = ???

      override def body: String = ???

      override def header(key: String): Option[String] = ???

      override def cookie(name: String): Option[WSCookie] = ???

      override def bodyAsBytes: ByteString = ???

      override def cookies: Seq[WSCookie] = ???

      override def json: JsValue = ???

      override def allHeaders: Map[String, Seq[String]] = ???
    })
  }
  def exceptionResponse() : Future[WSResponse] = Future{ throw new Exception("Intentional exception") }
}

class DummyApiWebServiceClient(responseCreator: () => Future[WSResponse]) extends WebServiceClient {

  override def get(url: String,
                   basicAuthCredentials: Option[BasicAuthCredentials] = None,
                   timeOut:Duration = 3 seconds,
                   headers: Seq[(String, String)]
                  ): Future[WSResponse] = responseCreator()

  override def post(url: String,
                    body: String,
                    basicAuthCredentials: Option[BasicAuthCredentials] = None,
                    timeOut:Duration = 3 seconds,
                    headers: Seq[(String, String)]
                   ): Future[WSResponse] = responseCreator()
}

class WsApiWebServiceClient(implicit actorSystem: ActorSystem, actorMaterializer: ActorMaterializer) extends WebServiceClient {

  override def post(url: String,
                    body: String,
                    basicAuthCredentials: Option[BasicAuthCredentials] = None,
                    timeOut:Duration = 3 seconds,
                    headers: Seq[(String, String)] = List[(String, String)]()
                   ): Future[WSResponse] = {
    performCallSafely((request) => request.post(body), url, basicAuthCredentials, timeOut,headers)
  }

  override def get(url: String,
                   basicAuthCredentials: Option[BasicAuthCredentials] = None,
                   timeOut:Duration = 3 seconds,
                   headers: Seq[(String, String)] = List[(String, String)]()
                  ): Future[WSResponse] = {
    performCallSafely((request) => request.get(), url, basicAuthCredentials, timeOut,headers)
  }

  private def createAhcWSClient(): AhcWSClient = {
    import com.typesafe.config.ConfigFactory
    import play.api._
    import play.api.libs.ws._

    val configuration: Configuration = Configuration.reference ++ Configuration(ConfigFactory.parseString(
      """
        |play.ws.followRedirects = true
      """.stripMargin)) ++ Configuration(ConfigFactory.defaultApplication().resolve())

    // If running in Play, environment should be injected
    val environment = Environment(new File("."), this.getClass.getClassLoader, Mode.Prod)

    val parser = new WSConfigParser(configuration, environment)
    val config = new AhcWSClientConfig(wsClientConfig = parser.parse())

    val builder = new AhcConfigBuilder(config)
    val logging = new AsyncHttpClientConfig.AdditionalChannelInitializer() {
      override def initChannel(channel: io.netty.channel.Channel): Unit = {
        channel.pipeline.addFirst("log", new io.netty.handler.logging.LoggingHandler("debug"))
      }
    }
    val ahcBuilder = builder.configure()
    ahcBuilder.setHttpAdditionalChannelInitializer(logging)
    val ahcConfig = ahcBuilder.build()
    new AhcWSClient(ahcConfig)
  }

  // Closes the client onComplete
  private def performCallSafely(call: WSRequest => Future[WSResponse],
                                url: String,
                                basicAuthCredentials: Option[BasicAuthCredentials],
                                timeOut:Duration,
                                headers: Seq[(String, String)]
                               ): Future[WSResponse] = {

    val client = createAhcWSClient()

    var request = client.url(url).withRequestTimeout(timeOut)

    request = addHeaders(request, headers)
    request = addBasicAuth(request, basicAuthCredentials)

    val response = call(request)
    response.onComplete((response) => client.close())
    response
  }

  private def addBasicAuth(request: WSRequest, basicAuthCredentials: Option[BasicAuthCredentials]): WSRequest = {

    basicAuthCredentials match {
      case Some(credentials) => request.withAuth(credentials.username, credentials.password, WSAuthScheme.BASIC)
      case None => request
    }
  }

  private def addHeaders(request: WSRequest, headers: Seq[(String, String)]): WSRequest = {
    if (!headers.isEmpty){ request.withHeaders(headers: _ *)}
    else { request }
  }
}