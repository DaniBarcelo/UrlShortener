package urlshortener.domain;

import com.sun.tools.javac.util.Pair;

import java.net.URI;
import java.util.List;

public class SystemInfo {
    private Long numUsers;
    private Long numClicks;
    private Long numUris;
 

    public SystemInfo(Long numUris, Long numClicks, Long numUsers) {
        this.numUris = numUris;
        this.numClicks = numClicks;
        this.numUsers = numUsers;
        
        
    }

     public Long getNumUris() {
            return numUris;
        }
   
    public Long getNumClicks() {
        return numClicks;
    }

    public Long getNumUsers() {
        return numUsers;
    }




}
