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
<<<<<<< HEAD
import org.springframework.http.MediaType;

=======
//QR
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
>>>>>>> 5e49f36cf00021fffd164745dc92832d1911a50d


import org.springframework.web.multipart.MultipartFile;

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

  @RequestMapping(value = "/link", method = RequestMethod.POST)
  public ResponseEntity<ShortURL> shortener(@RequestParam("url") String url, @RequestParam(value = "sponsor", required = false) String sponsor, HttpServletRequest request) throws IOException, WriterException {
    UrlValidator urlValidator = new UrlValidator(new String[] {"http", "https"});
    if (urlValidator.isValid(url)) {
      ShortURL su = shortUrlService.save(url, sponsor, request.getRemoteAddr());
      QRCodeWriter qrCodeWriter = new QRCodeWriter();
      BitMatrix bitMatrix = qrCodeWriter.encode(su.getUri().toString(), BarcodeFormat.QR_CODE, 200, 200);
      BufferedImage BI = MatrixToImageWriter.toBufferedImage(bitMatrix);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ImageIO.write(BI, "jpg", baos);
      su.setQR(baos.toByteArray());
      HttpHeaders h = new HttpHeaders();
      h.setLocation(su.getUri());
      return new ResponseEntity<>(su, h, HttpStatus.CREATED);
    } else {
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }
  }



  // CSV function ,
  @RequestMapping(value = "/csv", method = RequestMethod.POST, produces="application/csv")
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
        String filename = "shortened-URLs-" + text + ".csv";
        String csvFolder = "./files/";

        //Fichero escritura
        String archCSV = csvFolder + filename;
        String [] headersWrite = {"url", "shortened URL"};

        Writer writer = new FileWriter(archCSV);
        CSVWriter csvWriter = new CSVWriter(writer, ',' , CSVWriter.NO_QUOTE_CHARACTER);
        csvWriter.writeNext(headersWrite);
        
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
            String [] dataWrite = {url, shortenedUri};
            csvWriter.writeNext(dataWrite);
          } else {
            System.out.println("URL " + url + " invalid");
          }
        }
        csvWriter.close();
        csvReader.close();

        Path path = Paths.get(csvFolder + filename);
        InputStreamResource resource = null;
        try {
          resource = new InputStreamResource(new FileInputStream(new File(path.toUri())));
        } catch (Exception e) {
          e.printStackTrace();
        }

       ResponseEntity res = ResponseEntity.ok()
          .contentType(MediaType.parseMediaType("text/csv"))
          .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
          .body(resource);

        //Borro file


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
