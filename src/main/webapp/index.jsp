<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>EML Reader</title>
</head>
<body>
<h2>Read .eml File</h2>
<form action="ReadEMLServlet" method="post">
    <label>Enter full path of .eml file:</label><br>
    <input type="text" name="emlPath" size="80"/><br><br>
    <input type="submit" value="Read EML"/>
</form>
</body>
</html>