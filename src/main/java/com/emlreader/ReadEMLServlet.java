package com.emlreader;

import jakarta.mail.*;
import jakarta.mail.Part;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import java.io.*;
import java.util.*;

public class ReadEMLServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        resp.setContentType("text/html;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        String sessionToken = (String) req.getSession().getAttribute("csrfToken");
        String formToken = req.getParameter("csrfToken");

        if (sessionToken == null || formToken == null || !sessionToken.equals(formToken)) {
            out.println("<div class='error'><h2>Invalid CSRF Token. Request blocked for security reasons.</h2></div>");
            return;
        }

        String emlPath = req.getParameter("emlPath");
        if (emlPath == null || !emlPath.matches("^[a-zA-Z0-9_./: \\\\-]+$")) {
            out.println("<div class='error'><h2>Invalid file path.</h2></div>");
            return;
        }

        File emlFile = new File(emlPath);
        out.println("""
                <html>
                <head>
                    <meta charset='UTF-8'>
                    <title>EML Reader Result</title>
                    <style>
                        body {
                            font-family: 'Segoe UI', Roboto, sans-serif;
                            background: linear-gradient(135deg, #e3f2fd, #e8eaf6);
                            margin: 0;
                            padding: 40px;
                        }
                        .container {
                            max-width: 950px;
                            margin: auto;
                            background: #fff;
                            border-radius: 16px;
                            box-shadow: 0 8px 24px rgba(0,0,0,0.1);
                            padding: 40px 50px;
                            animation: fadeIn 0.6s ease-in;
                        }
                        @keyframes fadeIn {
                            from {opacity: 0; transform: translateY(10px);}
                            to {opacity: 1; transform: translateY(0);}
                        }
                        h1 {
                            color: #1a237e;
                            font-size: 28px;
                            border-bottom: 2px solid #e0e0e0;
                            padding-bottom: 10px;
                            margin-bottom: 25px;
                        }
                        h3 {
                            color: #303f9f;
                            margin-top: 25px;
                        }
                        .meta {
                            background: #f9f9fc;
                            border: 1px solid #e0e0e0;
                            border-radius: 10px;
                            padding: 20px;
                            margin-bottom: 20px;
                        }
                        .meta p {
                            margin: 8px 0;
                            font-size: 15px;
                            color: #333;
                        }
                        .content {
                            background: #fff;
                            padding: 20px;
                            border-radius: 8px;
                            border: 1px solid #ddd;
                            line-height: 1.6;
                            font-size: 15px;
                            color: #333;
                            white-space: pre-wrap;
                            word-wrap: break-word;
                        }
                        .error {
                            background: #ffebee;
                            color: #c62828;
                            padding: 18px;
                            border-radius: 10px;
                            border: 1px solid #ffcdd2;
                            margin-bottom: 20px;
                        }
                        .back-btn {
                            display: inline-block;
                            margin-top: 30px;
                            background: #1e88e5;
                            color: white;
                            text-decoration: none;
                            padding: 10px 25px;
                            border-radius: 8px;
                            transition: background 0.3s ease;
                        }
                        .back-btn:hover {
                            background: #1565c0;
                        }
                    </style>
                </head>
                <body>
                <div class='container'>
        """);

        if (!emlFile.exists()) {
            out.println("<div class='error'><h2>File not found:</h2><p>" + emlPath + "</p></div></div></body></html>");
            return;
        }

        if (!emlPath.endsWith(".eml")) {
            out.println("<div class='error'><h2>Invalid File Extension:</h2><p>" + emlPath
                    + "</p></div></div></body></html>");
            return;
        }

        try (FileInputStream source = new FileInputStream(emlFile)) {
            Session session = Session.getDefaultInstance(new Properties());
            MimeMessage message = new MimeMessage(session, source);

            Address[] from = message.getFrom();
            Address[] to = message.getAllRecipients();
            String subject = message.getSubject();

            out.println("<h1>📧 Extracted Email Details</h1>");
            out.println("<div class='meta'>");
            out.println("<p><b>Subject:</b> " + escapeHtml(subject != null ? subject : "Not Provided") + "</p>");
            out.println("<p><b>From:</b> " + (from != null && from.length > 0 ? escapeHtml(from[0].toString()) : "") + "</p>");
            out.println("<p><b>To:</b> " + (to != null && to.length > 0 ? escapeHtml(to[0].toString()) : "") + "</p>");
            out.println("</div>");

            System.out.println("===== 📧 Extracted Email Details =====");
            System.out.println();
            System.out.println("Subject: " + (subject != null ? subject : "Not Provided"));
            System.out.println("From: " + (from != null && from.length > 0 ? from[0].toString() : "\"\""));
            System.out.println("To: " + (to != null && to.length > 0 ? to[0].toString() : "\"\""));
            System.out.println();

            String body = getTextFromMessage(message);
            if (body != null && !body.isEmpty()) {
                boolean isHtml = containsHtmlPart(message);
                String safeBody = isHtml ? sanitizeHtml(body) : escapeHtml(body);
                out.println("<div class='content'><h3>📝 Body</h3>" + safeBody + "</div>");
                System.out.println("===== 📝 Body Content =====");
                System.out.println();
                System.out.println(body);
                System.out.println();
            } else {
                out.println("<p><i>No readable text content found.</i></p>");
                System.out.println("Body: No readable text content found.");
            }

            
            out.println("<div class='content'><h3>🔍 Extracted Info from Body</h3>");
            System.out.println("===== 🔍 Extracted Info from Body =====");
            System.out.println();
            extractInfoFromBody(body, out);
            out.println("</div>");
            System.out.println();
            System.out.println();


        } catch (Exception e) {
            e.printStackTrace(out);
        }

        out.println("<a href='index.jsp' class='back-btn'>⬅ Back to Upload Page</a>");
        out.println("</div></body></html>");
    }

    private void extractInfoFromBody(String body, PrintWriter out) {
        if (body == null || body.isEmpty()) return;

        String[] keys = {
        		"To","From","Return-Path",
                "Delivered-To", "Received", "DKIM-Signature",
                "Date", "Reply-To", "MIME-Version",
                "ARC-Seal", "ARC-Authentication-Results"
        };

        String[] lines = body.split("\\r?\\n");
        for (String key : keys) {
            boolean found = false;
            for (String line : lines) {
                if (line.toLowerCase().startsWith(key.toLowerCase() + ":")) {
                    String value = line.substring(line.indexOf(':') + 1).trim();
                    out.println("<p><b>" + key + ":</b> " + escapeHtml(value) + "</p>");
                    System.out.println(key + ": " + value);
                    found = true;
                    break;
                }
            }
            if (!found) {
                out.println("<p><b>" + key + ":</b> Not Found</p>");
                System.out.println(key + ": Not Found");
            }
        }
    }

    private String getTextFromMessage(Part part) throws Exception {
        if (part.isMimeType("text/plain") || part.isMimeType("text/html"))
            return (String) part.getContent();

        if (part.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) part.getContent();
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < multipart.getCount(); i++) {
                result.append(getTextFromMessage(multipart.getBodyPart(i)));
            }
            return result.toString();
        }
        return "";
    }

    private String escapeHtml(String text) {
        return text == null ? ""
                : text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                      .replace("\"", "&quot;").replace("'", "&#39;");
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

    private String sanitizeHtml(String html) {
        if (html == null)
            return "";
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
//Multipart - 
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