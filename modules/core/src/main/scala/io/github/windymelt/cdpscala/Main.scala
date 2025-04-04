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

import TabSession.*
import cats.effect.IO
import cats.effect.IOApp
import util.Base64
import cats.effect.std.Random
import com.github.tarao.record4s.%
import io.github.windymelt.cdpscala.cmd.Page.setLifecycleEventsEnabled
import io.github.windymelt.cdpscala.cmd.Page.enable
import io.github.windymelt.cdpscala.cmd.Emulation.setScrollbarsHidden
//import scala.concurrent.duration.FiniteDuration

object Main extends IOApp.Simple {
  def run: IO[Unit] = for
    _ <- IO.delay(println(msg))
    // We need random number generator(RNG) to provide command ID
    // We use created RNG for further operation
    given Random[IO] <- Random.scalaUtilRandom[IO]
    _ <- ChromeProcess
      .spawn()
      .use: cp =>
        cp
          .newTabAutoClose()
          .use: ts =>
            import EventHandling.withEventHandling
            for
              _ <- IO.println("new tab opened")
              browserId = TabSession.extractBrowserId(ts.webSocketDebuggerUrl)
              wsSession <- TabSession.openWsSession(ts)
              shotBase64 <- wsSession.withEventHandling.use: s =>
                import cmd.Page.{navigate, captureScreenshot}
                import cmd.Browser.{setWindowBounds, getWindowForTarget}
                for
                  _ <- s.setLifecycleEventsEnabled(
                    true
                  )
                  window <- s.getWindowForTarget( /*None*/ )
                  // expand window.
                  _ <- s.setWindowBounds(
                    window.windowId,
                    %(
                      left = Some(0),
                      top = Some(0),
                      width = Some(1920),
                      height = Some(1080),
                      windowState = None
                    )
                  )
                  _ <- s.enable()
                  _ <- s.navigate("https://example.com/")
                  _ <- s.waitForLifecycleEvent("networkAlmostIdle")
                  _ <- s.setScrollbarsHidden(true)
                  shot <- s.captureScreenshot(
                    "jpeg",
                    quality = Some(80),
                    captureBeyondViewport = Some(true),
                    viewport = Some(
                      %(
                        x = 0,
                        y = 0,
                        width = 1920,
                        height = 1080,
                        scale = 1.0
                      )
                    )
                  )
                yield shot
              _ <- IO.delay:
                val Base64(shot) = shotBase64: @unchecked
                os.write.over(os.pwd / "ss.jpeg", shot)
            yield ()
  yield ()
}

def msg = "Chrome DevTools Protocol wrapper for Scala test command"
