package urlshortener.repository.impl;

 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 import org.springframework.jdbc.core.JdbcTemplate;
 import urlshortener.domain.SystemInfo;
 import urlshortener.repository.SystemInfoRepository;

 public class SystemInfoRepositoryImpl implements SystemInfoRepository {
     private static final Logger log = LoggerFactory
             .getLogger(SystemInfoRepositoryImpl.class);

     private final JdbcTemplate jdbc;

     public SystemInfoRepositoryImpl(JdbcTemplate jdbc) {
         this.jdbc = jdbc;
     }

     @Override
     public SystemInfo getSystemInfo() {
         long numUsers = 0;
         long numClicks = 0;
         long numUris = 0;

         try{
             numClicks = jdbc.queryForObject("select count(*) from click", Long.class);
             numUris = jdbc.queryForObject("select count(*) from shorturl", Long.class);
             numUsers = jdbc.queryForObject("select count(*) from (select ip from click group by ip)", Long.class);

         }catch (Exception e){
             log.debug("When select ", e);
         }
         return new SystemInfo(numUsers, numClicks, numUris);
     }
 }