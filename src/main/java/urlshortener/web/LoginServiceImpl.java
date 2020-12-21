package urlshortener.web;


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
//import org.springframework.boot.actuate.metrics.CounterService;
//import org.springframework.boot.actuate.metrics.Counter;
import urlshortener.domain.SystemInfo;
import urlshortener.repository.SystemInfoRepository;

import urlshortener.domain.ShortURL;
import urlshortener.service.ClickService;
import urlshortener.service.ShortURLService;

import io.micrometer.core.instrument.MeterRegistry;
//import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Counter;
import org.springframework.stereotype.Service;

import java.net.*;
import java.io.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;


@Service
public class LoginServiceImpl {

    private final Counter success;
    private final Counter failure;
    
    public LoginServiceImpl(MeterRegistry registry) {
        success = Counter.builder("counter.login.success").register(registry);
		failure = Counter.builder("counter.login.failure").register(registry);
    }
	
    public boolean login(String userName, char[] password) {
        boolean isok;
        if (userName.equals("admin") && "secret".toCharArray().equals(password)) {
            //counterService.increment("counter.login.success");
            success.increment();
            isok = true;
        }
        else {
            //counterService.increment("counter.login.failure");
            failure.increment();
            isok = false;
        }
        return isok;
    }
}