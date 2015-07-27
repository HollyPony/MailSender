package io.liomka.mailsender.utils;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;

import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Transport;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Properties;

/**
 * Created by aagombart on 09/07/2015.
 */
public class UserProperties {

    // Properties file path
    private final static String propertiesPath = "mail.properties";

    // Args const
    private final static String MAIL_TO = "MAIL_TO";
    private final static String MAIL_CC = "MAIL_CC";
    private final static String MAIL_FROM = "MAIL_FROM";
    private final static String MAIL_SMTP_PORT = "MAIL_SMTP_PORT";
    private final static String MAIL_SMTP_PORT_DEFAULT = "25";
    private final static String MAIL_ADRESSE_HOST = "MAIL_ADRESSE_HOST";
    private final static String UPDATE_MESSAGE_ID = "UPDATE_MESSAGE_ID";
    private final static String MAIL_ADRESSE_HOST_DEFAULT = "localhost";
    private final static String MAIL_PATH_DEFAULT = "./Mails/";

    // Properties
    private String mailPath = null;
    private Date date = null;
    private String from = null;
    private String to = null;
    private String cc = null;
    private String port = null;
    private String host = null;
    private Boolean updateMessageId = null;

    private Transport transport = null;
    private Session session = null;

    private Properties properties = null;

    public UserProperties() {
        // Load properties file
        properties = new Properties();
        InputStream propertiesFile = null;
        try {
            propertiesFile = new FileInputStream(propertiesPath);
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

        // Initialize mail parameters
        properties.setProperty("mail.transport.protocol", "smtp");
        properties.setProperty("mail.smtp.port", getPort().trim());
        properties.setProperty("mail.smtp.host", getHost().trim());
        properties.setProperty("mail.smtp.connectiontimeout", "5000");
        properties.setProperty("mail.smtp.timeout", "25000");
        properties.setProperty("mail.smtp.auth", "false");
    }

    public String getMailPath() {
        if (mailPath == null) {
            mailPath = properties.getProperty("MAIL_PATH", MAIL_PATH_DEFAULT);
        }
        return mailPath;
    }

    public Date getDate() {
        if (date == null) {
            date = new Date();
        }
        return date;
    }

    public String getTo() {
        if (to == null) {
            to = properties.getProperty(MAIL_TO, null);
        }
        return to;
    }

    public String getCc() {
        if (cc == null) {
            cc = properties.getProperty(MAIL_CC, StringUtils.EMPTY);
        }
        return cc;
    }

    public String getPort() {
        if (port == null) {
            port = properties.getProperty(MAIL_SMTP_PORT, MAIL_SMTP_PORT_DEFAULT);
        }
        return port;
    }

    public String getHost() {
        if (host == null) {
            host = properties.getProperty(MAIL_ADRESSE_HOST, MAIL_ADRESSE_HOST_DEFAULT);
        }
        return host;
    }

    public String getFrom() {
        if (from == null) {
            from = properties.getProperty(MAIL_FROM, StringUtils.EMPTY);
        }
        return from;
    }

    public boolean isUpdateMessageId() {
        if (updateMessageId == null) {
            updateMessageId = BooleanUtils.toBoolean(properties.getProperty(UPDATE_MESSAGE_ID, "false"));
        }
        return BooleanUtils.isTrue(updateMessageId);
    }

    public Session getSession() {
        if (session == null) {
            session = javax.mail.Session.getInstance(properties, null);
        }
        return session;
    }

    public Transport getTransport() throws NoSuchProviderException {
        if (transport == null) {
            transport = getSession().getTransport();
        }
        return transport;
    }
}
