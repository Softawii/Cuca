package dev.softawii.util;

import jakarta.inject.Singleton;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Singleton
public class EmailUtil {

    private final Pattern ruralPattern;
    private final Pattern gmailPattern;
    private final String  gmailRegex;

    public EmailUtil() {
        this.gmailRegex = "\\+(.*?)@";
        this.ruralPattern = Pattern.compile("^[a-zA-Z0-9._%+-]+@ufrrj\\.br$");
        this.gmailPattern = Pattern.compile(this.gmailRegex);
    }

    /**
     * Check if the email is from the specified domain
     *
     * @param email the email to check
     * @return result of the check
     */
    public boolean isValidEmail(String email) {
        Matcher matcher = this.ruralPattern.matcher(email);
        return matcher.matches();
    }

    /**
     * Process the email to remove the + and everything after it
     *
     * @param email the email to process
     * @return the processed email
     */
    public String processEmail(String email) {
        Matcher matcher = gmailPattern.matcher(email);

        if (matcher.find())
            return email.replaceAll(this.gmailRegex, "");
        return email;
    }
}
