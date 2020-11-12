package urlshortener.web;

 import org.springframework.beans.factory.annotation.Autowired;
 import org.springframework.http.HttpStatus;
 import org.springframework.http.ResponseEntity;
 import org.springframework.web.bind.annotation.GetMapping;
 import org.springframework.web.bind.annotation.RequestMapping;
 import org.springframework.web.bind.annotation.RequestMethod;
 import org.springframework.web.bind.annotation.RestController;
 import urlshortener.domain.SystemInfo;
 import urlshortener.repository.ClickRepository;
 import urlshortener.repository.SystemInfoRepository;

 @RestController
 public class SystemInfoController {

     protected SystemInfoRepository systemInfoRepository;

     @RequestMapping(value = "/link", method = RequestMethod.GET)
     public ResponseEntity<SystemInfo> getSystemInfo(){
         SystemInfo serverInfo = systemInfoRepository.getSystemInfo();
         return new ResponseEntity<>(serverInfo, HttpStatus.OK);
     }

 }