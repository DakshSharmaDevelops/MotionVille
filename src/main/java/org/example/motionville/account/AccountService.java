package org.example.motionville.account;

import org.example.motionville.account.web.RegisterForm;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService implements UserDetailsService {
    private final UserAccountRepository accounts;
    private final PasswordEncoder passwordEncoder;

    public AccountService(UserAccountRepository accounts, PasswordEncoder passwordEncoder) {
        this.accounts = accounts;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserAccount register(RegisterForm form) {
        if (form.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new PasswordLengthException("Password must be no longer than 72 UTF-8 bytes.");
        }
        String email = form.email().trim().toLowerCase(Locale.ROOT);
        String username = form.username().trim();
        if (accounts.existsByEmailIgnoreCase(email)) {
            throw new AccountConflictException("An account with that email already exists.");
        }
        if (accounts.existsByUsernameIgnoreCase(username)) {
            throw new AccountConflictException("That username is already taken.");
        }
        return accounts.save(new UserAccount(
                email,
                username,
                form.displayName().trim(),
                passwordEncoder.encode(form.password())));
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        UserAccount account = accounts.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("Account not found."));
        return new MotionVilleUserDetails(account);
    }

    public static class PasswordLengthException extends RuntimeException {
        public PasswordLengthException(String message) {
            super(message);
        }
    }
}
