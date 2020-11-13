package urlshortener.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import urlshortener.domain.SystemInfo;
import urlshortener.repository.SystemInfoRepository;

import java.net.*;
import java.io.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@RestController
public class SystemInfoController {


    private final SystemInfoRepository systemInfoRepository;


    public SystemInfoController(SystemInfoRepository systemInfoRepository) throws IOException {
        this.systemInfoRepository = systemInfoRepository;


    }

    @RequestMapping(value = "/info", method = RequestMethod.GET)
    public ResponseEntity<SystemInfo> getSystemInfo() throws IOException {
       
     SystemInfo serverInfo = systemInfoRepository.getSystemInfo();
        return new ResponseEntity<>(serverInfo, HttpStatus.OK);
    }

        private String getIP() throws IOException {

        URL myIP = new URL("http://checkip.amazonaws.com"); //this returns my IP
        BufferedReader in = new BufferedReader(new InputStreamReader(
                myIP.openStream()));

        String ip = in.readLine(); 
        System.out.println(ip);
        return ip;
    }



}
