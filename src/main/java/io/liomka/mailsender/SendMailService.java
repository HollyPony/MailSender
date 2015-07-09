package io.liomka.mailsender;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Created by aagombart on 03/03/2015.
 *
 * Main class of the mail sender
 */
public class SendMailService {

    private final static Logger LOG = LoggerFactory.getLogger(SendMailService.class);

    // Args const
    private final static String MAIL_TO = "MAIL_TO";
    private final static String MAIL_CC = "MAIL_CC";
    private final static String MAIL_FROM = "MAIL_FROM";
    private final static String MAIL_SMTP_PORT = "MAIL_SMTP_PORT";
    private final static String MAIL_SMTP_PORT_DEFAULT = "25";
    private final static String MAIL_ADRESSE_HOST = "MAIL_ADRESSE_HOST";
    private final static String MAIL_ADRESSE_HOST_DEFAULT = "localhost";

    // Default properties consts
    private final static String mailPath = "./Mails/";

    // Properties file path
    private final static String propertiesPath = "mail.properties";

    class Contexte {
        // Properties
        public Date date;
        public String from;
        public String to;
        public String cc;
        public String port;
        public String host;

        public Transport transport = null;
        public Session session = null;
    }

    private void execute() {

        LOG.info("Read properties on ".concat(propertiesPath));
        final Properties properties = loadProperties();

        LOG.info("Read files on \"".concat(mailPath).concat("\" !"));
        final Map<File, Message> mails = loadMail(mailPath);


        final Contexte ctx = new Contexte();
        ctx.date = new Date();
        ctx.to = properties.getProperty(MAIL_TO, null);
        ctx.cc = properties.getProperty(MAIL_CC, StringUtils.EMPTY);
        ctx.port = properties.getProperty(MAIL_SMTP_PORT, MAIL_SMTP_PORT_DEFAULT);
        ctx.host = properties.getProperty(MAIL_ADRESSE_HOST, MAIL_ADRESSE_HOST_DEFAULT);
        ctx.from = properties.getProperty(MAIL_FROM, StringUtils.EMPTY);

        try {
            // Il faut au moins des smtp host et port
            if (StringUtils.isEmpty(ctx.host))
                throw new Exception("Cannot send mail, server address is missing");

            if (StringUtils.isEmpty(ctx.port))
                throw new Exception("Cannot send mail: server port is missing");

            // Properties
            properties.setProperty("mail.transport.protocol", "smtp");
            properties.setProperty("mail.smtp.port", ctx.port.trim());
            properties.setProperty("mail.smtp.host", ctx.host.trim());
            properties.setProperty("mail.smtp.connectiontimeout", "5000");
            properties.setProperty("mail.smtp.timeout", "25000");
            properties.setProperty("mail.smtp.auth", "false");

            ctx.session = javax.mail.Session.getInstance(properties, null);

            LOG.info("Get transport ...");
            ctx.transport = ctx.session.getTransport();
            LOG.info("Connect on transport");
            ctx.transport.connect();
            LOG.info("Connected");

            LOG.info("Sending " + mails.values().size() + " mails");
            for (final Map.Entry<File, Message> mailEntry : mails.entrySet()) {
                sendMail(ctx, mailEntry);
            }
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        } catch (MessagingException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (ctx.transport != null) {
                try {
                    ctx.transport.close();
                } catch (MessagingException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void sendMail(final Contexte ctx, final Map.Entry<File, Message> mailEntry) {
        InputStream mailStream = null;

        try {
            LOG.info("Sending mail : "+ mailEntry.getKey().getCanonicalPath());

            mailStream = new FileInputStream(mailEntry.getKey());

            final String fromEml = (mailEntry.getValue() != null
                    && mailEntry.getValue().getFrom() != null
                    && mailEntry.getValue().getFrom().length > 0)
                    ? mailEntry.getValue().getFrom()[0].toString()
                    : StringUtils.EMPTY;

            final String fromDefault = System.getProperty("user.name").concat("@")
                    .concat(InetAddress.getLocalHost().getHostName());

            final String from = StringUtils.defaultIfEmpty(ctx.from, StringUtils.defaultIfEmpty(fromEml, fromDefault));

            // Le Message
            MimeMessage message = new MimeMessage(ctx.session, mailStream);
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(ctx.to.trim()));
            message.setRecipients(Message.RecipientType.CC, InternetAddress.parse(ctx.cc.trim()));
            message.setFrom(new InternetAddress(from.trim()));
            message.setSentDate(ctx.date);


            // envoi du mail
            ctx.transport.sendMessage(message, message.getRecipients(Message.RecipientType.TO));
            LOG.info("Mail sent");
        } catch (Exception e) {
            LOG.info("fail");
            e.printStackTrace();
        } finally {
            if (mailStream != null) {
                try {
                    mailStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private Map<File,Message> loadMail(String mailPath) {
        final Map<File, Message> result = new HashMap<File, Message>();
        final File mailDirectory = new File(mailPath);
        if (mailDirectory.isDirectory()) {
            final Collection<File> emlFiles = FileUtils.listFiles(mailDirectory, new String[]{"eml"}, true);

            final Properties props = System.getProperties();
            props.put("mail.host", "smtp.dummydomain.com");
            props.put("mail.transport.protocol", "smtp");

            final javax.mail.Session mailSession = javax.mail.Session.getDefaultInstance(props, null);
            for (final File emlFile : emlFiles) {
                try {
                    InputStream source = new FileInputStream(emlFile);
                    MimeMessage message = new MimeMessage(mailSession, source);
                    result.put(emlFile, message);
                } catch (MessagingException e) {
                    e.printStackTrace();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }

        return result;
    }

    private Properties loadProperties() {
        InputStream propertiesFile = null;
        final Properties properties = new Properties();
        try {
            propertiesFile = new FileInputStream(propertiesPath);

            LOG.info("Load properties");
            properties.load(propertiesFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (propertiesFile != null) {
                try {
                    propertiesFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return properties;
    }

    public static void main (String[] args) {
        try {
            new SendMailService().execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
