package software.pxel.learneasy.exception;

/**
 * Письмо не удалось доставить. Раньше {@code MessagingException} внутри
 * EmailService просто логировалась и гасилась, из-за чего вызывающий код не мог
 * отличить отправленное письмо от неотправленного: повторная отправка кода
 * молча отвечала успехом, ничего не отправив.
 */
public class EmailDeliveryException extends RuntimeException {

    public EmailDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
