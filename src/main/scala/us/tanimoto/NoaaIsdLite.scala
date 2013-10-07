package us.tanimoto

import scala.util.{Try, Success, Failure}
import scala.collection.JavaConversions._

import resource._

import org.apache.commons.net.ftp._
import org.apache.commons.net.io.Util

class NoaaIsdLite {
  private val ftp = new FTPClient()
  private val host = "ftp.ncdc.noaa.gov"
  private val isdPath = "/pub/data/noaa/isd-lite"
  private val user = "anonymous"
  private val pass = "anonymous"

  def open(): Unit = {
    ftp.connect(host)
    val login = ftp.login(user, pass)
    val reply = ftp.getReplyCode()
    if (login && FTPReply.isPositiveCompletion(reply)) {
      // Disable buffering
      ftp.setBufferSize(0)
      // Transfer as binary
      ftp.setFileType(FTP.BINARY_FILE_TYPE)
      // Set timeout to 5 minutes
      ftp.setControlKeepAliveTimeout(300)
      // Enable passive mode
      ftp.enterLocalPassiveMode()
    } else {
      close()
    }
  }

  def close(): Unit = {
    ftp.disconnect()
  }

  def requireConnection(): Unit = {
    if (!ftp.isConnected())
      open()
  }

  /**
    * Make Paths
    */
  private def makeFileName(usaf: String, wban: String, year: Int): String = {
    s"${usaf}-${wban}-${year}.gz"
  }

  private def makeFilePath(usaf: String, wban: String, year: Int): String = {
    val name = makeFileName(usaf, wban, year)
    s"${isdPath}/${year}/${name}"
  }

  /**
    * Downloading
    */
  private def downloadWith[A](filepath: String, proc: java.io.InputStream => Try[A]): Try[A] = {
    for {
      // Connect if needed
      opened <- Try(requireConnection)
      // Open input stream
      input <- Try(ftp.retrieveFileStream(filepath))
      if input != null
      // Run action
      result <- proc(input)
      // Complete transfer
      completed <- Try(ftp.completePendingCommand())
      if completed
    } yield {
      result
    }
  }

  private def downloadBytes(filepath: String): Try[Array[Byte]] = {
    val output = new java.io.ByteArrayOutputStream()
    def proc(input: java.io.InputStream): Try[Array[Byte]] = {
      for {
        copied <- Try(Util.copyStream(input, output))
        if copied >= 0
      } yield {
        output.toByteArray()
      }
    }
    downloadWith(filepath, proc)
  }

  /**
    * Listing
    */
  def listFiles(path: String): Seq[String] = {
    requireConnection()
    ftp.listNames(path).to[Seq]
  }

  def listByName(usaf: String, wban: String, year: Int): Seq[String] = {
    val path = makeFilePath(usaf, wban, year)
    listFiles(path)
  }

  def listByYear(year: Int): Seq[String] = {
    val path = s"${isdPath}/${year}"
    listFiles(path)
  }

  /**
   * Weather
   */
  def downloadWeather(usaf: String, wban: String, year: Int): Option[Seq[Map[String, String]]] = {
    val filepath = makeFilePath(usaf, wban, year)

    def proc(input: java.io.InputStream): Try[Seq[Map[String, String]]] = {
      for {
        gzip <- Try(new java.util.zip.GZIPInputStream(input))
      } yield {
        val buff = new java.io.BufferedInputStream(gzip)
        val lines = scala.io.Source.fromInputStream(buff).getLines.to[Seq]
        lines.map(parseLine)
      }
    }
    downloadWith(filepath, proc) match {
      case Success(s) => Some(s)
      case Failure(e) => None
    }
  }

  def downloadToFile(file: String, usaf: String, wban: String, year: Int): Unit = {
    val filepath = makeFilePath(usaf, wban, year)
    val output = new java.io.FileOutputStream(new java.io.File(file))

    def proc(input: java.io.InputStream): Try[Unit] = {
      val res = Try(Util.copyStream(input, output))
      input.close()
      output.close()
      res.map(x => ())
    }
    downloadWith(filepath, proc)
  }

  /**
    * Parsing
    */
  def parseLine(line: String): Map[String, String] = {

    case class Field(
      val start: Int,
      val end: Int,
      val length: Int,
      val name: String,
      val description: String
    )

    val fields = Seq(
      Field( 1,  4, 4, "year",       "Year"),
      Field( 6,  7, 2, "month",      "Month"),
      Field( 9, 11, 2, "day",        "Day"),
      Field(12, 13, 2, "hour",       "Hour"),
      Field(14, 19, 6, "air temp",   "Air Temperature (C)"),
      Field(20, 25, 6, "dew temp",   "Dew Point Temperature (C)"),
      Field(26, 31, 6, "pressure",   "Sea Level Pressure (hPa)"),
      Field(32, 37, 6, "wind dir",   "Wind Direction (Degrees)"),
      Field(38, 43, 6, "wind speed", "Wind Speed Rate (m/s)"),
      Field(44, 49, 6, "sky cond",   "Sky Condition"),
      Field(50, 55, 6, "precip 1h",  "Precipitation 1-Hour (mm)"),
      Field(56, 61, 6, "precip 6h",  "Precipitation 6-Hour (mm)")
    )

    val empty = Map[String, String]().withDefaultValue("")

    if (line.length >= 61) {
      fields.foldLeft(empty){ (m,f) =>
        val value = line.substring(f.start-1, f.end).trim
        val recoded = if (value == "-9999") "" else value
        m.updated(f.name, recoded)
      }
    } else {
      empty
    }
  }
}
