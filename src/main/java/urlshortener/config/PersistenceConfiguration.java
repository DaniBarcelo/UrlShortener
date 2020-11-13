package urlshortener.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import urlshortener.domain.SystemInfo;
import urlshortener.repository.ClickRepository;
import urlshortener.repository.ShortURLRepository;
import urlshortener.repository.impl.ClickRepositoryImpl;
import urlshortener.repository.SystemInfoRepository;
import urlshortener.repository.impl.ShortURLRepositoryImpl;
import urlshortener.repository.impl.SystemInfoRepositoryImpl;


@Configuration
public class PersistenceConfiguration {

  private final JdbcTemplate jdbc;

  public PersistenceConfiguration(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Bean
  ShortURLRepository shortURLRepository() {
    return new ShortURLRepositoryImpl(jdbc);
  }

  @Bean
  ClickRepository clickRepository() {
    return new ClickRepositoryImpl(jdbc);
  }

  @Bean
  SystemInfoRepository systemInfoRepository(){
    return new SystemInfoRepositoryImpl(jdbc);
  }

}
