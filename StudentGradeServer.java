import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class StudentGradeServer {

    // ArrayList to store students
    static ArrayList<Student> students = new ArrayList<>();

    static int nextId = 1;

    public static void main(String[] args) throws Exception {

        // Create server on port 8080
        int port = Integer.parseInt(
    System.getenv().getOrDefault("PORT", "8080")
);

HttpServer server = HttpServer.create(
    new InetSocketAddress("0.0.0.0", port), 0);

        // Serve HTML, CSS and JavaScript
        server.createContext("/", StudentGradeServer::serveFiles);

        // API to get students
        server.createContext("/api/students",
                StudentGradeServer::handleStudents);

        // API to add student
        server.createContext("/api/add",
                StudentGradeServer::addStudent);

        // API to delete student
        server.createContext("/api/delete",
                StudentGradeServer::deleteStudent);

        // API to clear all students
        server.createContext("/api/clear",
                StudentGradeServer::clearStudents);

        server.setExecutor(null);

        System.out.println("======================================");
        System.out.println("   STUDENT GRADE TRACKER SERVER");
        System.out.println("======================================");
        System.out.println("Server started successfully!");
        System.out.println("Open: http://localhost:8080");
        System.out.println("======================================");

        server.start();
    }

    // -----------------------------------------
    // SERVE HTML, CSS, JAVASCRIPT FILES
    // -----------------------------------------

    static void serveFiles(HttpExchange exchange) throws IOException {

        String path = exchange.getRequestURI().getPath();

        if (path.equals("/")) {
            path = "/index.html";
        }

        File file = new File("public" + path);

        if (!file.exists() || file.isDirectory()) {
            sendResponse(exchange, "404 - File Not Found", 404);
            return;
        }

        String contentType = "text/html";

        if (path.endsWith(".css")) {
            contentType = "text/css";
        } else if (path.endsWith(".js")) {
            contentType = "application/javascript";
        }

        byte[] data = Files.readAllBytes(file.toPath());

        exchange.getResponseHeaders().set(
                "Content-Type",
                contentType + "; charset=UTF-8"
        );

        exchange.sendResponseHeaders(200, data.length);

        OutputStream output = exchange.getResponseBody();
        output.write(data);
        output.close();
    }

    // -----------------------------------------
    // GET STUDENTS
    // -----------------------------------------

    static void handleStudents(HttpExchange exchange)
            throws IOException {

        if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            sendResponse(exchange, "Method Not Allowed", 405);
            return;
        }

        String json = createStudentJSON();

        exchange.getResponseHeaders().set(
                "Content-Type",
                "application/json; charset=UTF-8"
        );

        sendResponse(exchange, json, 200);
    }

    // -----------------------------------------
    // ADD STUDENT
    // -----------------------------------------

    static void addStudent(HttpExchange exchange)
            throws IOException {

        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            sendResponse(exchange, "Method Not Allowed", 405);
            return;
        }

        String body = readRequestBody(exchange);

        Map<String, String> data = parseFormData(body);

        String name = data.get("name");
        String marksText = data.get("marks");

        if (name == null || marksText == null ||
                name.trim().isEmpty()) {

            sendResponse(exchange,
                    "{\"success\":false,\"message\":\"Invalid data\"}",
                    400);
            return;
        }

        try {

            int marks = Integer.parseInt(marksText);

            if (marks < 0 || marks > 100) {

                sendResponse(exchange,
                        "{\"success\":false,\"message\":\"Marks must be between 0 and 100\"}",
                        400);

                return;
            }

            Student student =
                    new Student(nextId++, name.trim(), marks);

            students.add(student);

            sendResponse(exchange,
                    "{\"success\":true,\"message\":\"Student added successfully\"}",
                    200);

        } catch (NumberFormatException e) {

            sendResponse(exchange,
                    "{\"success\":false,\"message\":\"Marks must be a number\"}",
                    400);
        }
    }

    // -----------------------------------------
    // DELETE STUDENT
    // -----------------------------------------

    static void deleteStudent(HttpExchange exchange)
            throws IOException {

        if (!exchange.getRequestMethod().equalsIgnoreCase("DELETE")) {
            sendResponse(exchange, "Method Not Allowed", 405);
            return;
        }

        String query =
                exchange.getRequestURI().getQuery();

        if (query == null) {
            sendResponse(exchange,
                    "{\"success\":false}",
                    400);
            return;
        }

        String[] parts = query.split("=");

        if (parts.length < 2) {
            sendResponse(exchange,
                    "{\"success\":false}",
                    400);
            return;
        }

        int id = Integer.parseInt(parts[1]);

        boolean removed = false;

        Iterator<Student> iterator =
                students.iterator();

        while (iterator.hasNext()) {

            Student student = iterator.next();

            if (student.getId() == id) {

                iterator.remove();
                removed = true;
                break;
            }
        }

        if (removed) {

            sendResponse(exchange,
                    "{\"success\":true}",
                    200);

        } else {

            sendResponse(exchange,
                    "{\"success\":false}",
                    404);
        }
    }

    // -----------------------------------------
    // CLEAR ALL STUDENTS
    // -----------------------------------------

    static void clearStudents(HttpExchange exchange)
            throws IOException {

        if (!exchange.getRequestMethod().equalsIgnoreCase("DELETE")) {
            sendResponse(exchange,
                    "Method Not Allowed",
                    405);
            return;
        }

        students.clear();

        sendResponse(exchange,
                "{\"success\":true}",
                200);
    }

    // -----------------------------------------
    // CREATE JSON
    // -----------------------------------------

    static String createStudentJSON() {

        StringBuilder json = new StringBuilder();

        json.append("[");

        for (int i = 0; i < students.size(); i++) {

            Student student = students.get(i);

            json.append("{");

            json.append("\"id\":")
                    .append(student.getId())
                    .append(",");

            json.append("\"name\":\"")
                    .append(escapeJSON(student.getName()))
                    .append("\",");

            json.append("\"marks\":")
                    .append(student.getMarks())
                    .append(",");

            json.append("\"grade\":\"")
                    .append(student.getGrade())
                    .append("\"");

            json.append("}");

            if (i < students.size() - 1) {
                json.append(",");
            }
        }

        json.append("]");

        return json.toString();
    }

    // -----------------------------------------
    // READ REQUEST BODY
    // -----------------------------------------

    static String readRequestBody(HttpExchange exchange)
            throws IOException {

        InputStream input =
                exchange.getRequestBody();

        return new String(
                input.readAllBytes(),
                StandardCharsets.UTF_8
        );
    }

    // -----------------------------------------
    // PARSE FORM DATA
    // -----------------------------------------

    static Map<String, String> parseFormData(String body)
            throws UnsupportedEncodingException {

        Map<String, String> map =
                new HashMap<>();

        String[] pairs = body.split("&");

        for (String pair : pairs) {

            String[] keyValue = pair.split("=");

            if (keyValue.length == 2) {

                String key =
                        URLDecoder.decode(
                                keyValue[0],
                                "UTF-8"
                        );

                String value =
                        URLDecoder.decode(
                                keyValue[1],
                                "UTF-8"
                        );

                map.put(key, value);
            }
        }

        return map;
    }

    // -----------------------------------------
    // ESCAPE JSON
    // -----------------------------------------

    static String escapeJSON(String text) {

        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }

    // -----------------------------------------
    // SEND RESPONSE
    // -----------------------------------------

    static void sendResponse(
            HttpExchange exchange,
            String response,
            int statusCode) throws IOException {

        byte[] data =
                response.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set(
                "Content-Type",
                "text/plain; charset=UTF-8"
        );

        exchange.sendResponseHeaders(
                statusCode,
                data.length
        );

        OutputStream output =
                exchange.getResponseBody();

        output.write(data);
        output.close();
    }
}
