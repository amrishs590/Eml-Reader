<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="java.util.*" %>
<html>
<head>
    <title>EML Reader Result</title>
    <link rel="stylesheet" href="style.css">
</head>
<body>
<div class="container">

<%
    String error = (String) request.getAttribute("error");
    if (error != null) {
        System.out.println("Error: " + error);
%>
    <div class="error"><h2><%= error %></h2></div>
<%
    } else {
        Map<String, Object> data = (Map<String, Object>) request.getAttribute("emailData");
        if (data != null) {

            System.out.println("\n===== 📧 Extracted Email Details =====");
            System.out.println("Subject: " + data.get("subject"));
            System.out.println("From: " + data.get("from"));
            System.out.println("To: " + data.get("to"));
%>

    <h1>📧 Extracted Email Details</h1>

    <div class="meta">
        <h3>📝 Header</h3>
        <p><b>Subject:</b> <%= com.emlreader.ReadEMLServlet.escapeHtml((String)data.get("subject")) %></p>
        <p><b>From:</b> <%= com.emlreader.ReadEMLServlet.escapeHtml((String)data.get("from")) %></p>
        <p><b>To:</b> <%= com.emlreader.ReadEMLServlet.escapeHtml((String)data.get("to")) %></p>
    </div>

    <div class="content">
        <h3>📝 Body</h3>
        <%= data.get("body") %>
    </div>

<%
    // ---------- Extracted Info ----------
    System.out.println("\n===== 🔍 Extracted Info from Body =====");
    Map<String, String> infoMap = (Map<String, String>) data.get("extractedHeaders");
    int infoIndex = 1;
    for (Map.Entry<String, String> entry : infoMap.entrySet()) {
        System.out.println(infoIndex++ + ". " + entry.getKey() + ": " + entry.getValue());
    }
%>

    <div class="meta">
        <h3>🔍 Extracted Info from Body</h3>
        <%
            for (Map.Entry<String, String> entry : infoMap.entrySet()) {
                String safeKey = com.emlreader.ReadEMLServlet.escapeHtml(entry.getKey());
                String safeValue = com.emlreader.ReadEMLServlet.escapeHtml(entry.getValue());
        %>
            <p><b><%= safeKey %>:</b> <%= safeValue %></p>
        <% } %>
    </div>

<%
    // ---------- Domains ----------
    System.out.println("\n===== 🌐 Extracted Domains =====");
    List<String> domainList = (List<String>) data.get("domains");
    if (domainList != null && !domainList.isEmpty()) {
        for (String domain : domainList) System.out.println("Domain: " + domain);
    } else {
        System.out.println("No domains found.");
    }
%>

    <div class="meta">
        <h3>🌐 Extracted Domains</h3>
        <ul>
        <%
            for (String domain : domainList) {
                String safeDomain = com.emlreader.ReadEMLServlet.escapeHtml(domain);
        %>
            <li><%= safeDomain %></li>
        <% } %>
        </ul>
    </div>

<%
    // ---------- All Key:Value Pairs ----------
    System.out.println("\n===== 🧾 All Key:Value Pairs =====");
    List<String[]> kvPairs = (List<String[]>) data.get("keyValues");
    int pairIndex = 1;
    for (String[] kv : kvPairs) {
        System.out.println(pairIndex++ + ". " + kv[0] + " : " + kv[1]);
    }
%>

    <div class="content">
        <h3>🧾 All Key:Value Pairs</h3>
        <table>
            <tr><th>#</th><th>Key</th><th>Value</th></tr>
            <%
                int rowCount = 1;
                for (String[] kv : kvPairs) {
                    String safeKey = com.emlreader.ReadEMLServlet.escapeHtml(kv[0]);
                    String safeValue = com.emlreader.ReadEMLServlet.escapeHtml(kv[1]);
            %>
            <tr>
                <td><%= rowCount++ %></td>
                <td><%= safeKey %></td>
                <td><%= safeValue %></td>
            </tr>
            <% } %>
        </table>
    </div>

<%
        } // data != null
    } // else (no error)
%>

    <a href="index.jsp" class="back-btn">⬅ Back to Upload Page</a>
</div>
</body>
</html>
