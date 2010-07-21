package org.jclouds.rackspace.cloudfiles.handlers;

import static org.jclouds.http.HttpUtils.releasePayload;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Resource;

import org.jclouds.blobstore.ContainerNotFoundException;
import org.jclouds.blobstore.KeyNotFoundException;
import org.jclouds.http.HttpCommand;
import org.jclouds.http.HttpErrorHandler;
import org.jclouds.http.HttpResponse;
import org.jclouds.http.HttpResponseException;
import org.jclouds.logging.Logger;
import org.jclouds.rest.AuthorizationException;
import org.jclouds.util.Utils;

/**
 * This will parse and set an appropriate exception on the command object.
 * 
 * @author Adrian Cole
 * 
 */
public class ParseCloudFilesErrorFromHttpResponse implements HttpErrorHandler {
   @Resource
   protected Logger logger = Logger.NULL;
   public static final String MOSSO_PREFIX = "^/v1[^/]*/MossoCloudFS_[^/]+/";
   public static final Pattern CONTAINER_PATH = Pattern.compile(MOSSO_PREFIX + "([^/]+)$");
   public static final Pattern CONTAINER_KEY_PATH = Pattern.compile(MOSSO_PREFIX + "([^/]+)/(.*)");

   public void handleError(HttpCommand command, HttpResponse response) {
      Exception exception = new HttpResponseException(command, response);
      try {
         String content = parseErrorFromContentOrNull(command, response);
         exception = content != null ? new HttpResponseException(command, response, content) : exception;
         switch (response.getStatusCode()) {
         case 401:
            exception = new AuthorizationException(command.getRequest(), content, exception);
            break;
         case 404:
            if (!command.getRequest().getMethod().equals("DELETE")) {
               String path = command.getRequest().getEndpoint().getPath();
               Matcher matcher = CONTAINER_PATH.matcher(path);
               if (matcher.find()) {
                  exception = new ContainerNotFoundException(matcher.group(1), content);
               } else {
                  matcher = CONTAINER_KEY_PATH.matcher(path);
                  if (matcher.find()) {
                     exception = new KeyNotFoundException(matcher.group(1), matcher.group(2), content);
                  }
               }
            }
            break;
         }
      } finally {
         releasePayload(response);
         command.setException(exception);
      }
   }

   String parseErrorFromContentOrNull(HttpCommand command, HttpResponse response) {
      if (response.getPayload() != null) {
         try {
            return Utils.toStringAndClose(response.getPayload().getInput());
         } catch (IOException e) {
            logger.warn(e, "exception reading error from response", response);
         }
      }
      return null;
   }
}
