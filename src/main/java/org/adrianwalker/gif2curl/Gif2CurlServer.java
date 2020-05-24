package org.adrianwalker.gif2curl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import static java.util.Arrays.asList;
import static java.util.Arrays.copyOfRange;
import java.util.List;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;

public final class Gif2CurlServer {

  private static final String PATH = "/gif2curl";
  private String[] args;

  public Gif2CurlServer(final String[] args) {

    this.args = args;
  }

  public void start() throws Exception {

    int port = Integer.valueOf(args[0]);
    args = copyOfRange(args, 1, args.length);

    ServletHandler handler = createHandler();
    Server server = createServer(port, handler);
    server.start();
    server.join();
  }

  private Server createServer(final int port, final ServletHandler handler) {

    Server server = new Server(port);
    server.setHandler(handler);

    return server;
  }

  private ServletHandler createHandler() {

    ServletHandler handler = new ServletHandler();
    handler.addServletWithMapping(new ServletHolder(new Gif2CurlServlet(args)), PATH);

    return handler;
  }

  public static void main(final String[] args) throws Exception {

    new Gif2CurlServer(args).start();
  }

  private final class Gif2CurlServlet extends HttpServlet {

    private static final String REDIRECT_URL = "https://github.com/adrianwalker/gif2curl";
    private static final String USER_AGENT_HEADER = "User-Agent";
    private static final String CURL_USER_AGENT = "curl";
    private static final String TEXT_HTML_CONTENT_TYPE = "text/html";
    private static final String EXECUTABLE = "gif-for-cli";
    private static final int BUFFER_SIZE = 80 * 24 * 2;
    private final List<String> command;

    public Gif2CurlServlet(final String[] args) {

      super();

      command = new ArrayList<>();
      command.add(EXECUTABLE);
      command.addAll(asList(args));
    }

    @Override
    protected void doGet(
      final HttpServletRequest request,
      final HttpServletResponse response) throws IOException {

      checkUserAgent(request, response);
      setHeaders(response);

      Process process = startProcess(command);

      bufferedTransferTo(
        process.getInputStream(),
        response.getOutputStream(),
        BUFFER_SIZE);
    }

    private void setHeaders(final HttpServletResponse response) {

      response.setContentType(TEXT_HTML_CONTENT_TYPE);
      response.setStatus(HttpServletResponse.SC_OK);
    }

    private Process startProcess(final List<String> command) throws IOException {

      return new ProcessBuilder(command).start();
    }

    private void checkUserAgent(
      final HttpServletRequest request,
      final HttpServletResponse response) throws IOException {

      String userAgent = request.getHeader(USER_AGENT_HEADER);

      if (!userAgent.startsWith(CURL_USER_AGENT)) {
        response.sendRedirect(REDIRECT_URL);
      }
    }

    private void bufferedTransferTo(
      final InputStream from, final OutputStream to,
      final int bufferSize) throws IOException {

      byte[] b = new byte[bufferSize];
      int n;
      try (to; from) {
        while ((n = from.read(b)) > 0) {
          to.write(b, 0, n);
          to.flush();
        }
      }
    }
  }
}
