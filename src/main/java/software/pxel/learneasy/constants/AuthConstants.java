package software.pxel.learneasy.constants;

public class AuthConstants {

    public static final String MESSAGE_EMAIL_VERIFICATION = "To complete the registration, enter the code sent to your email.";

    /**
     * Аккаунт создан, но код доставить не удалось. Сообщение ведёт клиента к
     * повторной отправке вместо бесполезного ожидания письма.
     */
    public static final String MESSAGE_EMAIL_VERIFICATION_NOT_SENT =
            "The account has been created, but the confirmation code could not be sent. "
                    + "Please request the code again.";
    public static final String INVALID_VERIFICATION_RESPONSE = "Invalid or expired verification code.";
    public static final String USER_EMAIL_NOT_FOUND_EX_RESPONSE = "The user with this email address was not found.";
    public static final String RATE_LIMIT_EX_RESPONSE = "You have exceeded the email sending limit. Try again later.";
    public static final String USER_EMAIL_NO_VERIFICATION_EX_RESPONSE = "An unverified user with this email address was not found.";
}
