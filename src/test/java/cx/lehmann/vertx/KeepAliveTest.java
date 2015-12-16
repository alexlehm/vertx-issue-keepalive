package cx.lehmann.vertx;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;
import io.vertx.core.parsetools.RecordParser;
import io.vertx.test.core.VertxTestBase;

/**
 * 
 */

/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 *
 */
public class KeepAliveTest extends VertxTestBase {

  private static final int SERVER_PORT = 8000;
  private static final int REQUESTS = 15;
  private static final Logger log = LoggerFactory.getLogger(KeepAliveTest.class);
  private NetServer netServer;

  private AtomicInteger finished = new AtomicInteger(0);

  @Test
  public void test() {
    log.info("starting test");

    HttpClientOptions clientOptions = new HttpClientOptions().setDefaultHost("localhost").setDefaultPort(SERVER_PORT)
        // if setKeepAlive(false), the test passes
        .setKeepAlive(true);

    HttpClient client = vertx.createHttpClient(clientOptions);

    for (int i = 0; i < REQUESTS; i++) {
      startRequest(client);
    }
    waitFor(REQUESTS);
    await();
  }

  /**
   * @param testContext
   * @param async
   * @param client
   */
  private void startRequest(HttpClient client) {
    client.get("/", resp -> {
      log.info("request handler");
      resp.bodyHandler(buffer -> log.info("recieved body \"" + buffer + "\""));
      resp.endHandler(v -> {
        log.info("finished " + finished.addAndGet(1) + " of " + REQUESTS);
        complete();
      });
      resp.exceptionHandler(th -> {
        // if(th.getMessage().contains("Connection was closed")) {
        //// log.info("ignoring Connection was closed exception", th);
        // } else {
        // fail(th.getMessage());
        // }
      });
    }).exceptionHandler(th -> {
      // if(th.getMessage().contains("Connection was closed")) {
      //// log.info("ignoring Connection was closed exception", th);
      // } else {
      // fail(th.getMessage());
      // }
    }).end();
  }

  @Before
  public void before() throws Exception {
    setUp();
    NetServerOptions serverOptions = new NetServerOptions();

    netServer = vertx.createNetServer(serverOptions).connectHandler(socket -> {
      log.info("accepted new connection");
      AtomicBoolean firstRequest = new AtomicBoolean(true);
      socket.handler(RecordParser.newDelimited("\r\n\r\n", buffer -> {
        if (firstRequest.getAndSet(false)) {
          socket.write("HTTP/1.1 200 OK\n" + "Content-Type: text/plain\n" + "Content-Length: 4\n"
              + "Connection: keep-alive\n" + "\n" + "xxx\n");
        } else {
          socket.write("HTTP/1.1 200 OK\n" + "Content-Type: text/plain\n" + "Content-Length: 1\n"
              + "Connection: close\n" + "\n" + "\n");
          socket.close();
        }
      }));
    }).listen(SERVER_PORT, result -> {
      if (result.succeeded()) {
        log.info("server started");
      } else {
        log.error("server failed", result.cause());
      }
    });
    log.info("started netserver");
  }

  @After
  public void after() throws Exception {
    netServer.close();
    log.info("stopped netserver");
    tearDown();
  }

}
