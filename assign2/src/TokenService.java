import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Service for managing authentication tokens. Tokens are used to maintain
 * user sessions across reconnections without requiring credentials.
 */
public class TokenService {
    
    private static final int TOKEN_LENGTH = 32;
    private static final int TOKEN_EXPIRY_HOURS = 24; // Tokens expire after 24 hours
    
    private final Map<String, TokenInfo> tokens; 
    private final Map<String, String> userTokens; 
    private final ReentrantReadWriteLock lock;
    private final SecureRandom random; // Secure random number generator for token generation
    
    /**
     * Create a new token service.
     */
    public TokenService() {
        this.tokens = new HashMap<>();
        this.userTokens = new HashMap<>();
        this.lock = new ReentrantReadWriteLock();
        this.random = new SecureRandom();
    }
    
    /**
     * Generate a new authentication token for a user.
     * 
     * @param username The username
     * @return The generated token
     */
    public String generateToken(String username) {
        lock.writeLock().lock();
        try {
            // Invalidate any existing token for this user
            String existingToken = userTokens.get(username);
            if (existingToken != null) {
                tokens.remove(existingToken);
            }
            
            // Generate new token
            byte[] tokenBytes = new byte[TOKEN_LENGTH];
            random.nextBytes(tokenBytes);
            String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes); // Generate a URL-safe token
            
            // Store token info
            LocalDateTime expiry = LocalDateTime.now().plus(TOKEN_EXPIRY_HOURS, ChronoUnit.HOURS);
            TokenInfo tokenInfo = new TokenInfo(username, expiry);
            
            tokens.put(token, tokenInfo);
            userTokens.put(username, token);
            
            return token;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Validate a token and return the associated username.
     * 
     * @param token The token to validate
     * @return The username if valid, null otherwise
     */
    public String validateToken(String token) {
        if (token == null) {
            return null;
        }
        
        lock.readLock().lock();
        try {
            TokenInfo tokenInfo = tokens.get(token);
            if (tokenInfo == null) {
                return null;
            }
            
            // Check if token has expired
            if (LocalDateTime.now().isAfter(tokenInfo.getExpiryTime())) {
                // Token expired, remove it
                lock.readLock().unlock();
                lock.writeLock().lock();
                try {
                    tokens.remove(token);
                    userTokens.remove(tokenInfo.getUsername());
                    return null;
                } finally {
                    lock.writeLock().unlock();
                    lock.readLock().lock();
                }
            }
            
            return tokenInfo.getUsername();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Refresh a token's expiry time.
     * 
     * @param token The token to refresh
     * @return true if refreshed, false if token doesn't exist
     */
    public boolean refreshToken(String token) {
        lock.writeLock().lock();
        try {
            TokenInfo tokenInfo = tokens.get(token);
            if (tokenInfo == null) {
                return false;
            }
            
            // Update expiry time
            LocalDateTime newExpiry = LocalDateTime.now().plus(TOKEN_EXPIRY_HOURS, ChronoUnit.HOURS);
            TokenInfo newTokenInfo = new TokenInfo(tokenInfo.getUsername(), newExpiry);
            tokens.put(token, newTokenInfo);
            
            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Invalidate a token.
     * 
     * @param token The token to invalidate
     */
    public void invalidateToken(String token) {
        if (token == null) {
            return;
        }
        
        lock.writeLock().lock();
        try {
            TokenInfo tokenInfo = tokens.remove(token);
            if (tokenInfo != null) {
                userTokens.remove(tokenInfo.getUsername());
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Clean up expired tokens.
     */
    public void cleanupExpiredTokens() {
        lock.writeLock().lock();
        try {
            LocalDateTime now = LocalDateTime.now();
            tokens.entrySet().removeIf(entry -> {
                if (now.isAfter(entry.getValue().getExpiryTime())) {
                    userTokens.remove(entry.getValue().getUsername());
                    return true;
                }
                return false;
            });
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Inner class to hold token information.
     */
    private static class TokenInfo {
        private final String username;
        private final LocalDateTime expiryTime;
        
        public TokenInfo(String username, LocalDateTime expiryTime) {
            this.username = username;
            this.expiryTime = expiryTime;
        }
        
        public String getUsername() {
            return username;
        }
        
        public LocalDateTime getExpiryTime() {
            return expiryTime;
        }
    }
}