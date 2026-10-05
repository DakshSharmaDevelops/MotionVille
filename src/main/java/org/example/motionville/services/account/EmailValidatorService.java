package org.example.motionville.services.account;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.time.Instant;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class EmailValidatorService {

    private static final Logger log = LoggerFactory.getLogger(EmailValidatorService.class);

    private static final Set<String> RESERVED_TEST_DOMAINS = Set.of(
            "localhost",
            "example.com",
            "example.org",
            "example.net"
    );

    private static final Set<String> RESERVED_TEST_TLDS = Set.of(
            ".test",
            ".example",
            ".invalid",
            ".localhost"
    );

    private static final long CACHE_TTL_MILLIS = 3600_000L; // 1 hour

    private final boolean mxValidationEnabled;
    private final Map<String, CacheEntry> domainCache = new ConcurrentHashMap<>();

    public EmailValidatorService(
            @Value("${motionville.mail.mx-validation.enabled:true}") boolean mxValidationEnabled) {
        this.mxValidationEnabled = mxValidationEnabled;
    }

    public void validateEmailDomainMx(String email) {
        if (!mxValidationEnabled) {
            return;
        }

        if (email == null || !email.contains("@")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid email format");
        }

        String domain = extractDomain(email);
        if (domain.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid email domain");
        }

        if (isReservedTestDomain(domain)) {
            return;
        }

        if (!hasValidMxOrARecord(domain)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Email domain does not have valid mail exchange (MX) records"
            );
        }
    }

    public boolean hasValidMxOrARecord(String domain) {
        if (domain == null || domain.isBlank()) {
            return false;
        }

        String normalizedDomain = domain.toLowerCase(Locale.ROOT).trim();

        if (isReservedTestDomain(normalizedDomain)) {
            return true;
        }

        CacheEntry cached = domainCache.get(normalizedDomain);
        long now = System.currentTimeMillis();
        if (cached != null && (now - cached.timestamp) < CACHE_TTL_MILLIS) {
            return cached.valid;
        }

        boolean isValid = queryDnsForMailExchange(normalizedDomain);
        domainCache.put(normalizedDomain, new CacheEntry(isValid, now));
        return isValid;
    }

    private boolean queryDnsForMailExchange(String domain) {
        Hashtable<String, String> env = new Hashtable<>();
        env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
        env.put("com.sun.jndi.dns.timeout.initial", "2500");
        env.put("com.sun.jndi.dns.timeout.retries", "1");

        DirContext dirContext = null;
        try {
            dirContext = new InitialDirContext(env);

            // 1. Check for MX records
            Attributes mxAttributes = dirContext.getAttributes(domain, new String[]{"MX"});
            Attribute mxAttribute = mxAttributes.get("MX");
            if (mxAttribute != null && mxAttribute.size() > 0) {
                return true;
            }

            // 2. Fallback per RFC 5321 §5.1: check A / AAAA records
            Attributes addressAttributes = dirContext.getAttributes(domain, new String[]{"A", "AAAA"});
            Attribute aRecord = addressAttributes.get("A");
            Attribute aaaaRecord = addressAttributes.get("AAAA");
            return (aRecord != null && aRecord.size() > 0) || (aaaaRecord != null && aaaaRecord.size() > 0);

        } catch (NamingException exception) {
            log.debug("DNS lookup failed for domain '{}': {}", domain, exception.getMessage());
            return false;
        } finally {
            if (dirContext != null) {
                try {
                    dirContext.close();
                } catch (NamingException ignored) {
                }
            }
        }
    }

    private String extractDomain(String email) {
        int atIndex = email.lastIndexOf('@');
        if (atIndex == -1 || atIndex == email.length() - 1) {
            return "";
        }
        return email.substring(atIndex + 1).toLowerCase(Locale.ROOT).trim();
    }

    private boolean isReservedTestDomain(String domain) {
        if (RESERVED_TEST_DOMAINS.contains(domain)) {
            return true;
        }
        for (String tld : RESERVED_TEST_TLDS) {
            if (domain.endsWith(tld)) {
                return true;
            }
        }
        return false;
    }

    public void clearCache() {
        domainCache.clear();
    }

    private record CacheEntry(boolean valid, long timestamp) {}
}
