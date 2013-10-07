package us.tanimoto

import scala.util.{Try, Success, Failure}

import org.scalatest.FunSpec
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.ShouldMatchers

class NoaaIsdLiteSpec extends FunSpec with ShouldMatchers with BeforeAndAfterAll {

  // Madison, WI
  val usaf = "726410"
  val wban = "14837"
  val year = 2012
  val noaa = new NoaaIsdLite()
  val file = "test.gz"

  override def beforeAll() = {
    noaa.open()
  }

  override def afterAll() = {
    noaa.close()
  }

  describe("NOAA ISD Lite") {
    it("should find a specific station") {
      val list = noaa.listByName(usaf, wban, year)
      list.size should be (1)
    }

    it("should list multiple stations in a year") {
      val list = noaa.listByYear(year)
      list.size should be > (13000)
    }

    it("should get weather data for a station") {
      val data = noaa.downloadWeather(usaf, wban, year)
      val list = data.getOrElse(Seq())
      list.size should be >= 8760
    }

    it("should download weather to a file") {
      noaa.downloadToFile(file, usaf, wban, year)
      val f = new java.io.File(file)
      f.delete()
    }
  }
}
