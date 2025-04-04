/*
 * Copyright (c) 2024 cdp-scala authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.windymelt.cdpscala

import cats.effect.IO
import cats.effect.Resource
import org.http4s.Uri

object ChromeProcess {
  type ChromeProcessIO = Resource[IO, ChromeProcess]

  val CHROME_SHELL = "chromium"

  def spawn(): ChromeProcessIO =
    Resource.make {
      IO.delay(rawSpawnChrome()) <* IO.sleep(
        concurrent.duration.Duration(100, "milliseconds")
      )
    } { proc =>
      IO.delay(proc.destroy())
    }

  // TODO: toggle headless switch
  private def rawSpawnChrome(): ChromeProcess = ChromeProcess(
    os
      .proc(CHROME_SHELL, "--headless", "--remote-debugging-port=9222")
      .spawn(stdout = os.Inherit, stderr = os.Inherit),
    "localhost",
    9222
  )
}

case class ChromeProcess(proc: os.SubProcess, host: String, port: Int) {
  def destroy(): Unit = proc.destroy()
  def httpUrl: Uri = Uri.unsafeFromString(s"http://${host}:${port}")
}
