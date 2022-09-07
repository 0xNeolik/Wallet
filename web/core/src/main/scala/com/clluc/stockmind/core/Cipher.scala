package com.clluc.stockmind.core

import java.net.InetAddress
import java.nio.file.{Files, Paths}
import java.security.spec.{PKCS8EncodedKeySpec, X509EncodedKeySpec}
import java.security.{KeyFactory, PrivateKey, PublicKey}
import java.text.SimpleDateFormat
import java.util.{Base64, Calendar, Date}
import com.typesafe.scalalogging.LazyLogging

//import play.api.inject.Injector
//import play.api.{Application, Play}
import org.apache.commons.net.ntp.NTPUDPClient
//import javax.inject.Inject

import com.clluc.stockmind.port.primary.LicensePort

private[core] class Cipher(license: String) extends LicensePort with LazyLogging {
  private val dateFmt   = "dd-MM-yyyy"
  val sdf               = new SimpleDateFormat(dateFmt)
  private val algorithm = "RSA"
  private val kf        = KeyFactory.getInstance(algorithm)

  //Call method to get the correct encoded private key to use for Encrypt
  def getPrivate: PrivateKey = {
    val file = Files.readAllBytes(Paths.get("private_key.der"))
    return kf.generatePrivate(new PKCS8EncodedKeySpec(file))
  }

  //Call method to get the correct encoded public key to use for Decrypt
  def getPublic: PublicKey = {
    val file = Files.readAllBytes(Paths.get("./conf/public_key.der"))
    return kf.generatePublic(new X509EncodedKeySpec(file))
  }

  //Returns the input param decrypted
  def decrypt(publicKey: PublicKey): Date = {
    val input   = license
    val decrypt = javax.crypto.Cipher.getInstance(algorithm)
    decrypt.init(javax.crypto.Cipher.DECRYPT_MODE, publicKey)
    new Date(new String(decrypt.doFinal(Base64.getDecoder.decode(input))).toLong)
  }

  //Returns the date encrypted and encoded on Base64
  def encrypt(privateKey: PrivateKey, day: Int, month: Int, year: Int): String = {
    //Create Date
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.DAY_OF_MONTH, day)
    calendar.set(Calendar.MONTH, month - 1)
    calendar.set(Calendar.YEAR, year)
    //Encrypt the date
    val encrypt = javax.crypto.Cipher.getInstance(algorithm)
    encrypt.init(javax.crypto.Cipher.ENCRYPT_MODE, privateKey)
    Base64.getEncoder.encodeToString(encrypt.doFinal(calendar.getTimeInMillis.toString.getBytes))
  }

  def getTimeNTP: Date = {
    (new NTPUDPClient)
      .getTime(InetAddress.getByName("1.es.pool.ntp.org"))
      .getMessage
      .getTransmitTimeStamp
      .getDate

  }

  override def checkLicense {
    logger.info("Checking your License ...")
    if (this.getTimeNTP.getTime >= this.decrypt(this.getPublic).getTime) {
      //Play.stop(Play.current.injector.instanceOf[Application])
      logger.error("Your Licensed has been expired")
      System.exit(0)
    }
  }
}

object Cipher {

  def apply(license: String): Cipher =
    new Cipher(license)
}
