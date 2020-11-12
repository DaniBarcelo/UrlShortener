package urlshortener.repository;

 import org.springframework.stereotype.Repository;
 import urlshortener.domain.SystemInfo;

 @Repository
 public interface SystemInfoRepository {

     SystemInfo getSystemInfo();
 }