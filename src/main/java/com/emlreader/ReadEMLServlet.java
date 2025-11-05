package com.emlreader;

import jakarta.mail.Address;
import jakarta.mail.BodyPart;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;

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
//        req.getSession().removeAttribute("csrfToken");

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

				.content p {
				    margin: 10px 0;
				}

				.content br {
				    line-height: 1.2em;
				}


				     .attachments {
				        margin-top: 30px;
				        padding: 15px;
				        background: #f1f8e9;
				        border-left: 5px solid #7cb342;
				        border-radius: 10px;
				    }
				    .attachment {
				        margin-left: 25px;
				        font-size: 14px;
				        color: #555;
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
			Session session = Session.getDefaultInstance(System.getProperties());
			MimeMessage message = new MimeMessage(session, source);

			Address[] from = message.getFrom();
			Address[] to = message.getAllRecipients();
			String subject = message.getSubject();

			out.println("<h1>📧 Extracted Email Details</h1>");
			out.println("<div class='meta'>");
			out.println("<p><b>Subject:</b> " + escapeHtml(subject != null ? subject : "Not Provided") + "</p>");
			out.println("<p><b>From:</b> \"" + (from != null && from.length > 0 ? escapeHtml(from[0].toString()) : "")
					+ "\"</p>");
			out.println(
					"<p><b>To:</b> \"" + (to != null && to.length > 0 ? escapeHtml(to[0].toString()) : "") + "\"</p>");
			out.println("</div>");

			String body = getTextFromMessage(message);
			if (body != null && !body.isEmpty()) {
				boolean isHtml = containsHtmlPart(message);
				String safeBody = isHtml ? sanitizeHtml(body) : escapeHtml(body);
				out.println("<div class='content'>" + safeBody + "</div>");
			} else {
				out.println("<p><i>No readable text content found.</i></p>");
			}

			if (hasAttachments(message)) {
				out.println("<div class='attachments'><h3>📎 Attachments Detected:</h3>");
				listAttachments(message, out);
				out.println("</div>");
			}

		} catch (Exception e) {
			out.println(
					"<div class='error'><h3>Error Occurred:</h3><pre>" + escapeHtml(e.getMessage()) + "</pre></div>");
		}
		out.println("<a href='index.jsp' class='back-btn'>⬅ Back to Upload Page</a>");
		out.println("</div></body></html>");
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

	private boolean hasAttachments(Part part) throws Exception {
		if (part.isMimeType("multipart/*")) {
			Multipart multipart = (Multipart) part.getContent();
			for (int i = 0; i < multipart.getCount(); i++) {
				BodyPart bodyPart = multipart.getBodyPart(i);
				String disposition = bodyPart.getDisposition();
				if ("attachment".equalsIgnoreCase(disposition) || hasAttachments(bodyPart))
					return true;
			}
		}
		return false;
	}

	private void listAttachments(Part part, PrintWriter out) throws Exception {
		if (part.isMimeType("multipart/*")) {
			Multipart mp = (Multipart) part.getContent();
			for (int i = 0; i < mp.getCount(); i++) {
				BodyPart bp = mp.getBodyPart(i);
				String disposition = bp.getDisposition();
				if ("attachment".equalsIgnoreCase(disposition)) {
					out.println("<div class='attachment'>📄 <b>" + escapeHtml(bp.getFileName()) + "</b> ("
							+ escapeHtml(bp.getContentType()) + ")</div>");
				} else if (bp.getContent() instanceof Multipart) {
					listAttachments(bp, out);
				}
			}
		}
	}

	private String escapeHtml(String text) {
		return text == null ? ""
				: text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
						.replace("'", "&#39;");
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