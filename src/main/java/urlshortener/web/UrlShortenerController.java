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
import java.util.Base64;

//CSV
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import java.io.StringWriter;

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
  private final ShortURLService shortUrlService;

  private final ClickService clickService;

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



  // CSV function ,
  @RequestMapping(value = "/csv", method = RequestMethod.POST)
  public ResponseEntity shortenerWithCSV(@RequestParam("file") MultipartFile file,
                                            @RequestParam(value = "sponsor", required = false)
                                                String sponsor,
                                            HttpServletRequest request, HttpServletResponse response) {
                                              
    System.out.println("En funcion csv");
    // validate file
    //TODO: check if format is CSV
    if (file.isEmpty()) {
      System.out.println("Fichero vacio");
      return new ResponseEntity<>("Empty file", HttpStatus.BAD_REQUEST);
    } else {
      try{
        //Fichero lectura
        Reader reader = new InputStreamReader(file.getInputStream());
        CSVReader csvReader = new CSVReaderBuilder(reader).withSkipLines(1).build();
        String[] fila = null;

        //Date to name the file
        LocalDateTime date = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_hh_mm_ss_SS");
        String text = date.format(formatter);

        //V2 escalabilidad
        //String en el que se escribe el contenido "URL, shortenedURL \n"
        StringWriter content = new StringWriter();
        content.write("url,shortened URL\n");
        
        //Mostrar contenido CSV
        while((fila = csvReader.readNext()) != null) {
          String url = fila[0];
          System.out.println(url);
          
          //Procesar linea, recortar url
          UrlValidator urlValidator = new UrlValidator(new String[] {"http", "https"});
          if (urlValidator.isValid(url)) {
            ShortURL su = shortUrlService.save(url, sponsor, request.getRemoteAddr());
            String shortenedUri = su.getUri().toString();
            System.out.println("URL " + url + " ---> " + shortenedUri);

            //Escribir url
            content.write(url + ',' + shortenedUri + "\n");

          } else {
            System.out.println("URL " + url + " invalid");
          }
        }
        csvReader.close();
        System.out.println("String a enviar: " + content);

        // Should give stringWriter as response and not an attachment
        ResponseEntity res = new ResponseEntity<>(content, HttpStatus.OK);
        return res;

      } catch (Exception ex) {
          System.out.println("Error processing the CSV file.");
          ex.printStackTrace();
          return new ResponseEntity<>("Error Processing the CSV file", HttpStatus.BAD_REQUEST);
      }
    }
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
