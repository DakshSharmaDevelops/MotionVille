package org.example.motionville.account.web;

import jakarta.validation.Valid;
import org.example.motionville.account.AccountConflictException;
import org.example.motionville.account.AccountService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AccountController {
    private final AccountService accounts;

    public AccountController(AccountService accounts) {
        this.accounts = accounts;
    }

    @GetMapping("/login")
    public String login() {
        return "account/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("form", new RegisterForm("", "", "", ""));
        return "account/register";
    }

    @PostMapping("/register")
    public String register(
            @Valid @ModelAttribute("form") RegisterForm form,
            BindingResult bindingResult,
            Model model) {
        if (bindingResult.hasErrors()) {
            return "account/register";
        }
        try {
            accounts.register(form);
        } catch (AccountConflictException | AccountService.PasswordLengthException exception) {
            model.addAttribute("registrationError", exception.getMessage());
            return "account/register";
        }
        return "redirect:/login?registered";
    }
}
