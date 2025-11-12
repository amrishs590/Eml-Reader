package com.emlreader;

import jakarta.mail.*;
import jakarta.mail.Part;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.util.logging.Logger;

@WebServlet("/read-eml")
public class ReadEMLServlet extends HttpServlet {

    private static final Logger logger = Logger.getLogger(ReadEMLServlet.class.getName());
    private static final String[] HEADER_KEYS = {
            "To", "From", "Return-Path", "Delivered-To", "Received",
            "DKIM-Signature", "Date", "Reply-To", "MIME-Version",
            "ARC-Seal", "ARC-Authentication-Results"
    };

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        String sessionToken = (String) req.getSession().getAttribute("csrfToken");
        String formToken = req.getParameter("csrfToken");
        if (sessionToken == null || formToken == null || !sessionToken.equals(formToken)) {
            req.setAttribute("error", "Invalid CSRF Token. Request blocked for security reasons.");
            req.getRequestDispatcher("result.jsp").forward(req, resp);
            return;
        }

        String emlPath = req.getParameter("emlPath");
        if (emlPath == null || !emlPath.matches("^[a-zA-Z0-9_./: \\\\-]+$")) {
            req.setAttribute("error", "Invalid file path.");
            req.getRequestDispatcher("result.jsp").forward(req, resp);
            return;
        }

        File emlFile = new File(emlPath);
        if (!emlFile.exists()) {
            req.setAttribute("error", "File not found: " + emlPath);
            req.getRequestDispatcher("result.jsp").forward(req, resp);
            return;
        }

        if (!emlPath.endsWith(".eml")) {
            req.setAttribute("error", "Invalid file extension: " + emlPath);
            req.getRequestDispatcher("result.jsp").forward(req, resp);
            return;
        }

        try (FileInputStream source = new FileInputStream(emlFile)) {

            Session session = Session.getDefaultInstance(new Properties());
            MimeMessage message = new MimeMessage(session, source);

            Map<String, Object> emailData = new HashMap<>();
            emailData.put("subject", message.getSubject());
            emailData.put("from", getAddressString(message.getFrom()));
            emailData.put("to", getAddressString(message.getAllRecipients()));

            String body = getTextFromMessage(message);
            boolean isHtml = containsHtmlPart(message);
            emailData.put("body", isHtml ? sanitizeHtml(body) : escapeHtml(body));

            Map<String, String> extractedHeaders = extractInfoFromBody(body);
            List<String> domains = extractDomainsFromBody(body);
            List<String[]> keyValuePairs = extractAllKeyValuePairs(body);

            emailData.put("extractedHeaders", extractedHeaders);
            emailData.put("domains", domains);
            emailData.put("keyValues", keyValuePairs);

            req.setAttribute("emailData", emailData);
            req.getRequestDispatcher("result.jsp").forward(req, resp);

        } catch (Exception e) {
            logger.severe("Error reading EML file: " + e.getMessage());
            req.setAttribute("error", "Error processing file: " + e.getMessage());
            req.getRequestDispatcher("result.jsp").forward(req, resp);
        }
    }

    private String getAddressString(Address[] addresses) {
        if (addresses == null || addresses.length == 0) return "Not Provided";
        return escapeHtml(addresses[0].toString());
    }

    private String getTextFromMessage(Part part) throws Exception {
        if (part.isMimeType("text/plain") || part.isMimeType("text/html"))
            return (String) part.getContent();

        if (part.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) part.getContent();
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < multipart.getCount(); i++)
                result.append(getTextFromMessage(multipart.getBodyPart(i)));
            return result.toString();
        }
        return "";
    }

    private Map<String, String> extractInfoFromBody(String body) {
        Map<String, String> info = new LinkedHashMap<>();
        if (body == null || body.isEmpty()) return info;

        String[] lines = body.split("\\r?\\n");
        for (String key : HEADER_KEYS) {
            for (String line : lines) {
                if (line.matches("(?i)^\\s*" + key + ":.*")) {
                    info.put(key, line.substring(line.indexOf(':') + 1).trim());
                    break;
                }
            }
        }
        return info;
    }

    private List<String> extractDomainsFromBody(String body) {
        Set<String> domains = new LinkedHashSet<>();
        if (body == null || body.isEmpty()) return new ArrayList<>();

        String[] lines = body.split("\\r?\\n");
        Pattern emailPat = Pattern.compile("[a-zA-Z0-9._%+-]+@([a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})");
        Pattern urlPat = Pattern.compile("https?://([^/\\s:]+)", Pattern.CASE_INSENSITIVE);

        for (String line : lines) {
            Matcher m = emailPat.matcher(line);
            while (m.find()) domains.add(m.group(1).toLowerCase());

            m = urlPat.matcher(line);
            while (m.find()) {
                String host = m.group(1).toLowerCase();
                if (host.startsWith("www.")) host = host.substring(4);
                int colon = host.indexOf(':');
                if (colon > -1) host = host.substring(0, colon);
                domains.add(host);
            }
        }
        return new ArrayList<>(domains);
    }

    private List<String[]> extractAllKeyValuePairs(String body) {
        List<String[]> keyValues = new ArrayList<>();
        if (body == null || body.isEmpty()) return keyValues;

        String[] lines = body.split("\\r?\\n");
        String currentKey = null;
        StringBuilder currentValue = new StringBuilder();
        Pattern headerPattern = Pattern.compile("^[A-Za-z0-9\\-]+:\\s+.*");

        for (String line : lines) {
            if (line.trim().isEmpty()) continue;

            if (headerPattern.matcher(line).matches()) {
                if (currentKey != null)
                    keyValues.add(new String[]{currentKey.trim(), currentValue.toString().trim()});

                int colonIndex = line.indexOf(':');
                currentKey = line.substring(0, colonIndex);
                currentValue = new StringBuilder(line.substring(colonIndex + 1).trim());
            } else if (currentKey != null && (line.startsWith(" ") || line.startsWith("\t"))) {
                currentValue.append(" ").append(line.trim());
            }
        }

        if (currentKey != null)
            keyValues.add(new String[]{currentKey.trim(), currentValue.toString().trim()});

        // 🔹 Exclude unwanted keys
        Set<String> excludedKeys = new LinkedHashSet<>(Arrays.asList(
            "Subject", "Date", "From", "To", "Message-ID", "Creation-Time", "Received"
        ));

        List<String[]> filteredList = new ArrayList<>();
        for (String[] kv : keyValues) {
            boolean isExcluded = false;
            for (String exclude : excludedKeys) {
                if (exclude.equalsIgnoreCase(kv[0])) {
                    isExcluded = true;
                    break;
                }
            }
            if (!isExcluded)
                filteredList.add(kv);
        }

        return filteredList;
    }

    private boolean containsHtmlPart(Part part) throws Exception {
        if (part.isMimeType("text/html"))
            return true;

        if (part.isMimeType("multipart/*")) {
            Multipart mp = (Multipart) part.getContent();
            for (int i = 0; i < mp.getCount(); i++) {
                if (containsHtmlPart(mp.getBodyPart(i)))
                    return true;
            }
        }
        return false;
    }

    public static String escapeHtml(String text) {
        return text == null ? "" : text
                .replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String sanitizeHtml(String html) {
        if (html == null) return "";
        html = html.replaceAll("(?i)<script.*?>.*?</script>", "");
        html = html.replaceAll("(?i)<iframe.*?>.*?</iframe>", "");
        html = html.replaceAll("(?i)<object.*?>.*?</object>", "");
        html = html.replaceAll("(?i)<embed.*?>.*?</embed>", "");
        html = html.replaceAll("(?i)on\\w+\\s*=\\s*(['\"]).*?\\1", "");
        html = html.replaceAll("(?i)javascript:", "");
        return html;
    }
}

//Part
//javamail interface - represent piece of email message
//Single Part - simple text mail
//Multi Part - Html + attachment + images


//Bodypart
//It is one section of multipart message
//Multipart 
//	text/ Plain
//	text/ html
//	image png
//BodyPart is used only inside a multipart email


//Multipart 
//A Multipart object is a container that holds multiple BodyPart objects
//Think of Multipart as a list of parts inside one email.

//isMimeType
//This method checks what kind of content a Part holds
//if (part.isMimeType("text/plain")) → plain text email
//if (part.isMimeType("text/html")) → HTML email
//if (part.isMimeType("multipart/*")) → email with multiple parts (text + attachments)	
