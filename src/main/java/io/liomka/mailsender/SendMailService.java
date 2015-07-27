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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

/**
 * Created by aagombart on 03/03/2015.
 *
 * Main class of the mail sender
 */
public class SendMailService {

    private final static Logger LOG = LoggerFactory.getLogger(SendMailService.class);

    // TODO Make a singleton
    final UserProperties usr = new UserProperties();

    private void execute() {

        usr.getDate();
        final String mailPath = usr.getMailPath();

        LOG.info("Read files on \"".concat(mailPath).concat("\" !"));
        final Map<String, Collection<File>> mailsMap = loadMails(mailPath);

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

            for (final Map.Entry<String, Collection<File>> mailsEntry : mailsMap.entrySet()) {
                final String address = mailsEntry.getKey();
                final Collection<File> mails = mailsEntry.getValue();
                if (mails != null && mails.size() > 0) {
                    LOG.info("Sending " + mails.size() + " mails to <" + address + ">");
                    for (final File file : mails) {
                        sendMail(address, file);
                    }
                }
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

    private void sendMail(final String recipient, final File mailFile) {
        InputStream mailStream = null;

        try {
            LOG.info("\tSending mail");
            LOG.info("\t\tPath: " + mailFile.getCanonicalPath());
            LOG.info("\t\tSize: " + FileUtils.byteCountToDisplaySize(mailFile.length()));

            mailStream = new FileInputStream(mailFile);

            final MimeMessage message = new MimeMessage(usr.getSession(), mailStream);

            final String fromEml = (message.getFrom() != null && message.getFrom().length > 0)
                    ? message.getFrom()[0].toString() : StringUtils.EMPTY;

            final String fromDefault = System.getProperty("user.name").concat("@")
                    .concat(InetAddress.getLocalHost().getHostName());

            final String from = StringUtils.defaultIfEmpty(usr.getFrom(), StringUtils.defaultIfEmpty(fromEml, fromDefault));

            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient.trim()));
            message.setRecipients(Message.RecipientType.CC, InternetAddress.parse(usr.getCc().trim()));
            message.setFrom(new InternetAddress(from.trim()));
            message.setSentDate(usr.getDate());

            if (usr.isUpdateMessageId()) {
                message.setHeader("Message-id", UUID.randomUUID().toString());
            }

            usr.getTransport().sendMessage(message, message.getRecipients(Message.RecipientType.TO));
            LOG.info("\tMail sent");
        } catch (Exception e) {
            LOG.error("\tMail fail");
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
     */
    private Map<String, Collection<File>> loadMails(String mailPath) {
        final Map<String, Collection<File>> result = new HashMap<String, Collection<File>>();
        final File mailDirectory = new File(mailPath);
        if (mailDirectory.isDirectory()) {

            for (final File file : mailDirectory.listFiles()) {
                if (file.isDirectory()) {
                    final Collection<File> folderContent = FileUtils.listFiles(file, new String[]{"eml"}, true);
                    result.put(file.getName(), folderContent);
                }
            }

            final Collection<File> rootList = FileUtils.listFiles(mailDirectory, new String[]{"eml"}, false);

            final String recipient = usr.getTo();
            if (!result.containsKey(recipient)) {
                result.put(recipient, new HashSet<File>());
            }
            result.get(usr.getTo()).addAll(rootList);
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
