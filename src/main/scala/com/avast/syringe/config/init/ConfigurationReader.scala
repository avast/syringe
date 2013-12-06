package com.avast.syringe.config.init

import java.io.Reader

/**
 * User: vacata
 * Date: 12/5/13
 * Time: 9:20 AM
 *
 * Simple abstract trait giving the possibility to read static configuration from provided `Reader` (typically
 * wrapping some initialization file like `.properties`, `.ini` or `.xml`.
 */
trait ConfigurationReader {
  def configurationRead: Option[Reader]
}
