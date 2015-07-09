package io.liomka.mailsender;

import io.liomka.mailsender.utils.UserProperties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by aagombart on 03/03/2015.
 *
 * Main class of the mail sender
 */
public class SendMailService {

    private final static Logger LOG = LoggerFactory.getLogger(SendMailService.class);

    private void execute() {
        final UserProperties usr = new UserProperties();

        usr.getDate();
        final String mailPath = usr.getMailPath();

        LOG.info("Read files on \"".concat(mailPath).concat("\" !"));
        final List<File> mails = loadMails(mailPath);

        Transport transport = null;
        try {
            // At least a host and port smtp are required
            if (StringUtils.isEmpty(usr.getHost()))
                throw new Exception("Cannot send mail, server address is missing");

            if (StringUtils.isEmpty(usr.getPort()))
                throw new Exception("Cannot send mail: server port is missing");

            LOG.info("Get session ...");
            usr.getSession();
            LOG.info("Get transport ...");
            transport = usr.getTransport();
            LOG.info("Connect on transport");
            transport.connect();
            LOG.info("Connected");

            LOG.info("Sending " + mails.size() + " mails");
            for (final File mailFile : mails) {
                sendMail(usr, mailFile);
            }
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        } catch (MessagingException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (transport != null) {
                try {
                    transport.close();
                } catch (MessagingException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void sendMail(final UserProperties usr, final File mailFile) {
        InputStream mailStream = null;

        try {
            LOG.info("Sending mail : " + mailFile.getCanonicalPath());

            mailStream = new FileInputStream(mailFile);

            final MimeMessage message = new MimeMessage(usr.getSession(), mailStream);

            final String fromEml = (message.getFrom() != null && message.getFrom().length > 0)
                    ? message.getFrom()[0].toString() : StringUtils.EMPTY;

            final String fromDefault = System.getProperty("user.name").concat("@")
                    .concat(InetAddress.getLocalHost().getHostName());

            final String from = StringUtils.defaultIfEmpty(usr.getFrom(), StringUtils.defaultIfEmpty(fromEml, fromDefault));

            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(usr.getTo().trim()));
            message.setRecipients(Message.RecipientType.CC, InternetAddress.parse(usr.getCc().trim()));
            message.setFrom(new InternetAddress(from.trim()));
            message.setSentDate(usr.getDate());

            usr.getTransport().sendMessage(message, message.getRecipients(Message.RecipientType.TO));
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

    /**
     * Load all mails into HashMap Java compliant.
     * @param mailPath
     * @return
     */
    private List<File> loadMails(String mailPath) {
        final List<File> result = new ArrayList<File>();
        final File mailDirectory = new File(mailPath);
        if (mailDirectory.isDirectory()) {
            final Collection<File> emlFiles = FileUtils.listFiles(mailDirectory, new String[]{"eml"}, true);

            for (final File emlFile : emlFiles) {
                result.add(emlFile);
            }
        }

        return result;
    }

    public static void main (String[] args) {
        try {
            new SendMailService().execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
