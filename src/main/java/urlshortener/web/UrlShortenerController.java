package urlshortener.web;

import java.net.URI;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.validator.routines.UrlValidator;
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


import org.springframework.web.multipart.MultipartFile;

import java.io.Reader;
import java.nio.file.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.*;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
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
  public ResponseEntity<?> redirectTo(@PathVariable String id,
                                      HttpServletRequest request) {
    ShortURL l = shortUrlService.findByKey(id);
    if (l != null) {
      clickService.saveClick(id, extractIP(request));
      return createSuccessfulRedirectToResponse(l);
    } else {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }

  @RequestMapping(value = "/link", method = RequestMethod.POST)
  public ResponseEntity<ShortURL> shortener(@RequestParam("url") String url,
                                            @RequestParam(value = "sponsor", required = false)
                                                String sponsor,
                                            HttpServletRequest request) {
    UrlValidator urlValidator = new UrlValidator(new String[] {"http",
        "https"});
    if (urlValidator.isValid(url)) {
      ShortURL su = shortUrlService.save(url, sponsor, request.getRemoteAddr());
      HttpHeaders h = new HttpHeaders();
      h.setLocation(su.getUri());
      return new ResponseEntity<>(su, h, HttpStatus.CREATED);
    } else {
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }
  }



  // CSV function
  @RequestMapping(value = "/csv", method = RequestMethod.POST)
  public ResponseEntity<ShortURL> shortenerWithCSV(@RequestParam("file") MultipartFile file,
                                            @RequestParam(value = "sponsor", required = false)
                                                String sponsor,
                                            HttpServletRequest request) {
                                              
    System.out.println("En funcion csv");
    // validate file
    if (file.isEmpty()) {
      System.out.println("Fichero vacio");
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    } else {
      try{
        //Fichero lectura
        Reader reader = new InputStreamReader(file.getInputStream());
        CSVReader csvReader = new CSVReaderBuilder(reader).withSkipLines(1).build();
        String[] fila = null;

        //Fichero escritura
        String archCSV = "./shortened-URLs.csv";
        String [] headersWrite = {"url", "shortened URL"};

        Writer writer = new FileWriter(archCSV);
        CSVWriter csvWriter = new CSVWriter(writer, ',' , CSVWriter.NO_QUOTE_CHARACTER);
        csvWriter.writeNext(headersWrite);
        
        //Mostrar contenido CSV
        while((fila = csvReader.readNext()) != null) {
            String url = fila[0];
            System.out.println(url);

            //Procesar linea, recortar url
            UrlValidator urlValidator = new UrlValidator(new String[] {"http",
            "https"});
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
        return new ResponseEntity<>(HttpStatus.CREATED);

      } catch (Exception ex) {
          System.out.println("An error occurred while processing the CSV file.");
          return new ResponseEntity<>(HttpStatus.BAD_REQUEST);

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
