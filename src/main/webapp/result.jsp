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
    %>
        <div class="error"><h2><%= error %></h2></div>
    <% } else {
        Map<String, Object> data = (Map<String, Object>) request.getAttribute("emailData");
        if (data != null) {
    %>
        <h1>📧 Extracted Email Details</h1>

        <div class="meta">
        	<h3>📝 Header</h3>
            <p><b>Subject:</b> <%= data.get("subject") %></p>
            <p><b>From:</b> <%= data.get("from") %></p>
            <p><b>To:</b> <%= data.get("to") %></p>
        </div>

        <div class="content">
            <h3>📝 Body</h3>
            <%= data.get("body") %>
        </div>

        <div class="meta">
            <h3>🔍 Extracted Info from Body</h3>
            <%
                Map<String, String> headers = (Map<String, String>) data.get("extractedHeaders");
                for (Map.Entry<String, String> entry : headers.entrySet()) {
            %>
                <p><b><%= entry.getKey() %>:</b> <%= entry.getValue() %></p>
            <% } %>
        </div>

        <div class="meta">
            <h3>🌐 Extracted Domains</h3>
            <ul>
            <%
                List<String> domains = (List<String>) data.get("domains");
                for (String domain : domains) {
            %>
                <li><%= domain %></li>
            <% } %>
            </ul>
        </div>

        <div class="content">
            <h3>🧾 All Key:Value Pairs</h3>
            <table>
                <tr><th>#</th><th>Key</th><th>Value</th></tr>
                <%
                    List<String[]> pairs = (List<String[]>) data.get("keyValues");
                    int i = 1;
                    for (String[] kv : pairs) {
                %>
                    <tr>
                        <td><%= i++ %></td>
                        <td><%= kv[0] %></td>
                        <td><%= kv[1] %></td>
                    </tr>
                <% } %>
            </table>
        </div>

    <% } } %>

    <a href="index.jsp" class="back-btn">⬅ Back to Upload Page</a>
</div>
</body>
</html>
