package urlshortener.web;

import java.net.MalformedURLException;
import java.net.URI;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.validator.routines.UrlValidator;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import urlshortener.domain.ShortURL;
import urlshortener.service.ClickService;
import urlshortener.service.ShortURLService;
//QR
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.Base64;

//CSV
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import java.io.StringWriter;
//WebSocket
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import java.io.Reader;
import java.nio.file.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.springframework.util.FileCopyUtils;
import java.io.*;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.time.*;
import java.time.format.*;

import com.opencsv.*;




@RestController
public class UrlShortenerController {
  @Autowired
  private SimpMessagingTemplate simpMessagingTemplate;

  private final ShortURLService shortUrlService;

  private final ClickService clickService;
  private final String URI_NOT_VALID_MSG = "INVALID URI";

  public UrlShortenerController(ShortURLService shortUrlService, ClickService clickService) {
    this.shortUrlService = shortUrlService;
    this.clickService = clickService;
  }

  private static String generateQRCodeImage(String uri,int width, int height)
          throws WriterException, IOException {
      QRCodeWriter qrCodeWriter = new QRCodeWriter();
      BitMatrix bitMatrix = qrCodeWriter.encode(uri, BarcodeFormat.QR_CODE, width, height);
      BufferedImage new_qr = MatrixToImageWriter.toBufferedImage(bitMatrix);
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      ImageIO.write(new_qr,"png",bos);
      byte[] qr_b = bos.toByteArray();
      qr_b = Base64.getEncoder().encode(qr_b);
      String qr = new String(qr_b);
      return qr;
  }

  @Async  //Generar QR de forma asincrona
  public void generarQR(ShortURL su){  
    System.out.println(Thread.currentThread().getName());
    try {
      String qr = generateQRCodeImage(su.getUri().toString(),250,250);
      shortUrlService.setQr(su, qr);
    } catch (WriterException e) {
        System.out.println(e.getMessage());
    } catch (IOException e) {
      System.out.println(e.getMessage());
    }
  }

  @RequestMapping(value = "/{id:(?!link|index).*}", method = RequestMethod.GET)
  public ResponseEntity<?> redirectTo(@PathVariable String id, HttpServletRequest request) {
    ShortURL l = shortUrlService.findByKey(id);
    if (l != null) {
      clickService.saveClick(id, extractIP(request));
      return createSuccessfulRedirectToResponse(l);
    } else {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }

  @RequestMapping(value = "/qr/{id}", method = RequestMethod.GET, produces = MediaType.IMAGE_JPEG_VALUE)
  public ResponseEntity<byte[]> takeQR (@PathVariable String id) throws IOException {
      //Generamos el array de Byte a partir del string del qr
      ShortURL su = shortUrlService.findByKey(id);
      String aux = su.getQr();
      byte[] bytes = Base64.getDecoder().decode(aux);
  
      return ResponseEntity
      .ok()
      .contentType(MediaType.IMAGE_JPEG)
      .body(bytes);
  } 

  @RequestMapping(value = "/link", method = RequestMethod.POST)
  public ResponseEntity<ShortURL> shortener(@RequestParam("url") String url, @RequestParam(value = "sponsor", required = false) String sponsor, @RequestParam(value = "qr", required = false) String checkboxValue, HttpServletRequest request) throws IOException, WriterException {
    UrlValidator urlValidator = new UrlValidator(new String[] {"http", "https"});
    if (urlValidator.isValid(url)) {
      ShortURL su = shortUrlService.save(url, sponsor, request.getRemoteAddr());
      HttpHeaders h = new HttpHeaders();
      h.setLocation(su.getUri());
      if (checkboxValue != null ){     
        su.setQrUrl("http://localhost:8080/qr/"+su.getHash());
        if (!shortUrlService.existShortURLByUri(su.getHash())){ //Generar QR si no existe
          generarQR(su);
        }
      }
      else{
        su.setQrUrl(null);
      }
      return new ResponseEntity<>(su, h, HttpStatus.CREATED);
    } else {
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }
  }



  // CSV function , escalability version.
  // Idea: Using webSockets full-duplex connection, client reads each URI and sends it, server responses with shortened URI.
  // Is not necessary to read or write any file.
  @MessageMapping("/websocket-csv-server")
  @SendTo("/topic/websocket-csv-client")
  public void shortenerWithCSV(String url,
                                            @RequestParam(value = "sponsor", required = false)
                                                String sponsor,
                                            // HttpServletRequest request,
                                            SimpMessageHeaderAccessor ha, 
                                            @Header("simpSessionId") String sessionId
                                            ) {                   
    System.out.println("En funcion csv con URL: ");
    System.out.println(url);
    try{
      //V2 escalabilidad 15 puntos
      String ip = (String) ha.getSessionAttributes().get("ip");        
      String message = "NO_MESSAGE";
      UrlValidator urlValidator = new UrlValidator(new String[] {"http", "https"});

      url = url.substring(0,url.length()-2);
      
      System.out.println("URL to validate: " + url);

      //Solucion para evitar el caracter fantasma \r, no aparece en los logs, pero se inyecta en las variables 
      System.out.println("URL without character \r: " + url);
      if (urlValidator.isValid(url)) {

        ShortURL su = shortUrlService.save(url, sponsor, ip);
        System.out.println("URL " + url + "is valid");
        String shortenedUri = su.getUri().toString();
        System.out.println("URL " + url + " ---> " + shortenedUri);
        //Escribir url
        message = url + ',' + shortenedUri + '\n';

      } else {
        System.out.println("URL " + url + "is invalid");
        message = url + ',' + URI_NOT_VALID_MSG + '\n';
      }
      System.out.println("String a enviar: " + message);
      sendMessage(message, sessionId);

    } catch (Exception ex) {
        System.out.println("Error processing the CSV file.");
        ex.printStackTrace();
        String message = "Error Processing the CSV file";
        sendMessage(message, sessionId);
    }
  }
  
  //Send message to simpleMessagingTemplate
  private void sendMessage(String message, String sessionId) {
      SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create();
      accessor.setHeader(SimpMessageHeaderAccessor.SESSION_ID_HEADER, sessionId);
      simpMessagingTemplate.convertAndSendToUser(sessionId, "/topic/websocket-csv-client", message,
              accessor.getMessageHeaders());
  }

  private String extractIP(HttpServletRequest request) {
    return request.getRemoteAddr();
  }

  private ResponseEntity<?> createSuccessfulRedirectToResponse(ShortURL l) {
    HttpHeaders h = new HttpHeaders();
    h.setLocation(URI.create(l.getTarget()));
    return new ResponseEntity<>(h, HttpStatus.valueOf(l.getMode()));
  }
}
