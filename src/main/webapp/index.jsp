<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.UUID" %>
<%
    String csrfToken = UUID.randomUUID().toString();
    session.setAttribute("csrfToken", csrfToken);
%>
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<title>EML Reader</title>
<style>
body {
    font-family: 'Segoe UI', Roboto, sans-serif;
    background: linear-gradient(135deg, #e3f2fd, #e8eaf6);
    margin: 0;
    height: 100vh;
    display: flex;
    justify-content: center;
    align-items: center;
}

.container {
    background: #fff;
    width: 520px;
    padding: 40px;
    border-radius: 16px;
    box-shadow: 0 8px 24px rgba(0,0,0,0.1);
    text-align: center;
    transition: transform 0.3s ease;
}
.container:hover {
    transform: translateY(-4px);
}

h2 {
    color: #1a237e;
    font-size: 26px;
    margin-bottom: 25px;
    font-weight: 600;
}

label {
    font-size: 15px;
    color: #555;
    display: block;
    margin-bottom: 10px;
    text-align: left;
}

input[type="text"] {
    width: 100%;
    padding: 12px 15px;
    border: 1px solid #ccc;
    border-radius: 8px;
    font-size: 15px;
    transition: 0.3s ease;
}
input[type="text"]:focus {
    border-color: #1e88e5;
    box-shadow: 0 0 8px rgba(30,136,229,0.3);
    outline: none;
}

input[type="submit"] {
    background-color: #1e88e5;
    color: #fff;
    border: none;
    border-radius: 8px;
    padding: 12px 30px;
    font-size: 16px;
    cursor: pointer;
    margin-top: 25px;
    transition: background-color 0.3s ease, transform 0.2s;
}
input[type="submit"]:hover {
    background-color: #1565c0;
    transform: scale(1.03);
}

.footer {
    text-align: center;
    color: #888;
    font-size: 13px;
    margin-top: 30px;
}
</style>
</head>
<body>
    <div class="container">
        <h2>📧 EML File Reader</h2>
        <form action="ReadEMLServlet" method="post" onsubmit="return sanitizeInput()">
            <label>Enter full path of .eml file:</label>
            <input type="text" id="emlPath" name="emlPath" placeholder="/path/to/email.eml"
                   required pattern="^[a-zA-Z0-9_./:\\\\-]+$"
                   title="Only letters, numbers, slashes, dots, and dashes allowed.">
            <input type="hidden" name="csrfToken" value="<%= csrfToken %>">
            <input type="submit" value="Read EML">
        </form>
        <div class="footer">🔒 Secure EML Reader</div>
    </div>

<script>
function sanitizeInput() {
    const input = document.getElementById("emlPath").value.trim();
    if (/[<>]/.test(input)) {
        alert("Invalid characters in file path. '<' and '>' are not allowed.");
        return false;
    }
    return true;
}
</script>
</body>
</html>
