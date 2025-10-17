package de.julianweinelt.caesar.auth;

import de.julianweinelt.caesar.util.StringUtil;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PasswordConditions {
    private int minPasswordLength = 4;
    private int maxPasswordLength = 32;
    private int passwordHistory = 0;
    private boolean enforceUppercase = true;
    private boolean enforceNumbers = true;
    private boolean enforceSymbols = false;

    public PasswordConditions(int minPasswordLength, int maxPasswordLength, int passwordHistory,
                              boolean enforceUppercase, boolean enforceNumbers, boolean enforceSymbols) {
        this.minPasswordLength = minPasswordLength;
        this.maxPasswordLength = maxPasswordLength;
        this.passwordHistory = passwordHistory;
        this.enforceUppercase = enforceUppercase;
        this.enforceNumbers = enforceNumbers;
        this.enforceSymbols = enforceSymbols;
    }

    public PasswordConditions() {}

    /**
     * Checks if the given password meets the conditions
     * @param password The password to check as a {@link String}
     * @return True if the password meets the conditions, false otherwise
     */
    public boolean checkPassword(String password) {
        String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
        String LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String DIGITS = "0123456789";
        String SYMBOLS = "!@#$%^&*()-_=+[]{}|;:,.<>?";
        return
                StringUtil.contains(password, LOWERCASE) &&
                enforceUppercase && StringUtil.contains(password, LETTERS) || !enforceUppercase
                && !StringUtil.contains(password, DIGITS) && enforceNumbers || !enforceNumbers
                && !StringUtil.contains(password, SYMBOLS) && enforceSymbols || !enforceSymbols;
    }
}