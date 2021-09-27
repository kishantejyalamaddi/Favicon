package com.wk.gbs.dc.service.document;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.activation.DataSource;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.mail.util.ByteArrayDataSource;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;
import com.wk.gbs.dc.model.EmaiLogs;

@Component
public class ExceptionEmailToServiceTeam {

	final Logger logger = LoggerFactory.getLogger(ExceptionEmailToServiceTeam.class);
	
	private static final String EXCEPTION_TEMPLATE = "customerSupport";
	
	private final String ERR_EMAIL_SENDING = "Email not sent Please contact the Support Center.";

	@Value("${email.customerSupport.to}")
	private String emailTo;
	
	@Value("${email.env}")
	private String emailEnv;
	
	@Value("${email.from}")
	private String emailfrom;
	@Autowired
	private JavaMailSender javaMailSender;

	@Autowired
	private SpringTemplateEngine templateEngine;

	
	public Boolean sendEmail(EmaiLogs emailLogs) {
		
		String subject = emailEnv+":Email Failed:- "+ emailLogs.getSubjectLine();
		Boolean inquiryStatusSent = Boolean.FALSE;
		String emailToValues = emailTo;// +","+ emailLogs.getToAddress();
			List<String>  emailToList = Arrays.asList(emailToValues.split(","));
				try {
					composeAndSend(EXCEPTION_TEMPLATE, subject, emailfrom ,emailToList ,
							emailLogs);
					inquiryStatusSent = Boolean.TRUE;
				} catch (MessagingException me) {
					logger.error(String.format(ERR_EMAIL_SENDING,
							me.getMessage()));
				}
			
		return Optional.ofNullable(inquiryStatusSent).get();
	
		
	}
	
	public void composeAndSend(String template, String subject, String fromEmail, List<String> toEmail, Object body)
			throws MessagingException {

		composeAndSendWithAttachment(template, subject, fromEmail, toEmail, body, Boolean.FALSE);
	}

	
	public void composeAndSendWithAttachment(String template, String subject, String fromEmail, List<String> toEmail,
			Object body, Boolean hasAttachment) throws MessagingException {

		MimeMessage mimeMessage = javaMailSender.createMimeMessage();
		MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, true);

		Map<String, Object> model = composeEmailBody(body);

		Context context = new Context();
		context.setVariables(model);

		String html = templateEngine.process(template, context);
		mimeMessageHelper.setFrom(fromEmail);

		mimeMessageHelper.setTo(toEmail.stream().toArray(String[]::new));
		mimeMessageHelper.setSubject(subject);

		mimeMessageHelper.setText(html, true);

		if (hasAttachment.compareTo(Boolean.TRUE) == 0) {
			DataSource fds = new ByteArrayDataSource(((ByteArrayOutputStream) model.get("attachment")).toByteArray(),
					model.get("contentType").toString());
			mimeMessageHelper.addAttachment(model.get("fileName").toString(), fds);
		}

		javaMailSender.send(mimeMessage);
	}
	
	private Map<String, Object> composeEmailBody(Object form) {
		Map<String, Object> model = new HashMap<>();

		Field[] fields = form.getClass().getDeclaredFields();

		Arrays.asList(fields).stream().forEach(field -> {
			Object value = readFieldValue(field, form);
			model.put(field.getName(), ObjectUtils.isEmpty(value) ? "" : value);
		});
		return model;
	}
	
	private Object readFieldValue(Field field, Object form) {
		Object obj = null;
		try {
			return FieldUtils.readField(form, field.getName(), true);
		} catch (IllegalAccessException ie) {
			logger.error(String.format("Exception occurred while reading the property value %s with the exception %s ",
					field.getName(), ie.getMessage()));
		}
		return obj;
	}



}
