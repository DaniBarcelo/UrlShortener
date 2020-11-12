package urlshortener.domain;

 import java.net.URI;
 import java.util.List;

 public class SystemInfo {
     private Long numUsers;
     private Long numClicks;
     private Long numUris;
     private List<URI> topUris;

     public SystemInfo(Long numUsers, Long numClicks, Long numUris) {
         this.numUsers = numUsers;
         this.numClicks = numClicks;
         this.numUris = numUris;
     }


     public SystemInfo(Long numUsers, Long numClicks, Long numUris, List<URI> topUris) {
         this.numUsers = numUsers;
         this.numClicks = numClicks;
         this.numUris = numUris;
         this.topUris = topUris;
     }

     public Long getNumUsers() {
         return numUsers;
     }

     public Long getNumClicks() {
         return numClicks;
     }

     public Long getNumUris() {
         return numUris;
     }

     public List<URI> getTopUris() {
         return topUris;
     }
 }