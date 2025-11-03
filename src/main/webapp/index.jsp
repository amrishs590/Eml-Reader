<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>EML Reader</title>
<style>
    body {
        font-family: 'Segoe UI', Roboto, sans-serif;
        background-color: #f4f6f8;
        margin: 0;
        padding: 0;
    }

    .container {
        max-width: 700px;
        margin: 80px auto;
        background: #fff;
        border-radius: 12px;
        box-shadow: 0 4px 15px rgba(0,0,0,0.1);
        padding: 40px 50px;
        text-align: center;
    }

    h2 {
        color: #333;
        font-size: 28px;
        margin-bottom: 25px;
    }

    label {
        font-size: 16px;
        color: #555;
        font-weight: 500;
        display: block;
        margin-bottom: 10px;
    }

    input[type="text"] {
        width: 90%;
        padding: 12px 15px;
        border: 1px solid #ccc;
        border-radius: 8px;
        font-size: 15px;
        transition: all 0.2s ease-in-out;
    }

    input[type="text"]:focus {
        border-color: #007bff;
        box-shadow: 0 0 6px rgba(0,123,255,0.3);
        outline: none;
    }

    input[type="submit"] {
        background-color: #007bff;
        color: #fff;
        border: none;
        border-radius: 8px;
        padding: 12px 30px;
        font-size: 16px;
        cursor: pointer;
        margin-top: 20px;
        transition: background-color 0.3s ease;
    }

    input[type="submit"]:hover {
        background-color: #0056b3;
    }

    .footer {
        text-align: center;
        color: #888;
        font-size: 14px;
        margin-top: 40px;
    }
</style>
</head>
<body>
    <div class="container">
        <h2>📧 EML File Reader</h2>
        <form action="ReadEMLServlet" method="post">
            <label>Enter full path of .eml file:</label>
            <input type="text" name="emlPath" placeholder="/path/to/email.eml" required />
            <br><br>
            <input type="submit" value="Read EML">
        </form>
    </div>
</body>
</html>
