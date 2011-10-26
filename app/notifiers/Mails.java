package notifiers;

import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;

import play.Logger;
import play.libs.Mail;
import play.mvc.Mailer;

public class Mails extends Mailer {
	public static void activation(String from, String to, String subject, String code) {
		setFrom("huhu");
		addRecipient(to);
		setSubject("Aktivierung");
		send(to, code);
	}
}
