package sample

import scala.util.Success
import scala.util.Failure
import scala.util.control.NonFatal

import dispatch._

import twitter4j.Twitter
import twitter4j.TwitterFactory
import twitter4j.TwitterException
import twitter4j.auth.RequestToken
import twitter4j.conf.ConfigurationBuilder

import xitrum.{Action, Server, SkipCsrfCheck}
import xitrum.annotation.{GET, POST}
import xitrum.util.Json

object Boot {
  def main(args: Array[String]) {
    Server.start()
  }
}

@POST("/")
class Empty extends Action with SkipCsrfCheck{
  def execute() {
    respondView()
  }
}

@GET("/ghcb")
class GHCallback extends Action with SkipCsrfCheck{
  def execute() {
  // Start github auth request
    val ghRequestHeadersAcceptJson = Map("Accept" -> "application/json")
    val ghRequestHeadersAcceptV3   = Map("Accept" -> "application/vnd.github.v3+json")

    val ghAuthRequestBody    = Map("client_id" -> Config.github.clientID,
                               "client_secret" -> Config.github.clientSecret,
                               "code" -> param("code").toString
                              )
    def ghAuthRequest = dispatch.url(Config.github.accsessTokenURL) << ghAuthRequestBody <:< ghRequestHeadersAcceptJson
    val ghAuthResponse = dispatch.Http(ghAuthRequest OK as.String)

    ghAuthResponse.onComplete {
      case Success(string) =>
        val json = Json.parse[Map[String, String]](string)
        if (!json.contains("access_token")){
          at("error") = json
          respondView[Empty]()
        } else {
          val accessToken = json("access_token")
          def ghUserRequest = dispatch.url(Config.github.userAPIURL) <<? Map("access_token" -> accessToken) <:< ghRequestHeadersAcceptV3
          val ghUserResponse = dispatch.Http(ghUserRequest OK as.String)
          ghUserResponse.onComplete {
            case Success(userInfo) =>
              at("userInfo") = userInfo
              respondView[Empty]()
            case Failure(error) =>
              at("error") = Map("error"->error)
              respondView[Empty]()
            }
        }
      case Failure(error) =>
        println("Error occured"+error.toString())
        at("error") = Map("error"->error)
        respondView[Empty]()
    }
  }
}

@GET("/twlogin")
class _TWLogin extends Action with SkipCsrfCheck{
  def execute() {
    val cb = new ConfigurationBuilder()
        cb.setUseSSL(true)
    val twitter = new TwitterFactory(cb.build()).getInstance()
        twitter.setOAuthConsumer(Config.twitter.consumerKey, Config.twitter.consumerSecret)
    val requestToken = twitter.getOAuthRequestToken(Config.twitter.callbackURL)

    session("twitter") = twitter
    session("requestToken") = requestToken
    redirectTo(requestToken.getAuthenticationURL().replace("http","https"));
  }
}

@GET("/twcb")
class TWCallback extends Action with SkipCsrfCheck{
  def execute(){
    val twitter      = session("twitter").asInstanceOf[Twitter]
    val requestToken = session("requestToken").asInstanceOf[RequestToken]
    val verifier     = param("oauth_verifier")
    try {
      val accessToken = twitter.getOAuthAccessToken(requestToken, verifier)
      val token       = accessToken.getToken()
      val tokenSecret = accessToken.getTokenSecret()
      val userId      = accessToken.getUserId()
      try {
        val userInfo = twitter.showUser(userId)
        at("userInfo") = Json.generate(
            Map(
                "login" -> userInfo.getName(),
                "avatar_url" -> userInfo.getProfileImageURL()
                )
            )
        respondView[Empty]()
      } catch {
        case NonFatal(e) =>
          println("Error occured"+e.toString())
          e.printStackTrace()
          at("error") = Map("error"->e)
          respondView[Empty]()
      }
    } catch {
      case NonFatal(e) =>
        println("Error occured"+e.toString())
        e.printStackTrace()
        at("error") = Map("error"->e)
        respondView[Empty]()
    }
  }
}

