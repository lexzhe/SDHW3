package ru.akirakozov.sd.refactoring.servlet;

import org.junit.jupiter.api.*;

import org.mockito.Mockito;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public class ServletTests {
    private static final String DB_PATH = "jdbc:sqlite:test.db";

    protected HttpServletRequest request;

    protected HttpServletResponse response;

    protected static void testDBCommand(String statement) {
        try (Connection connection = DriverManager.getConnection(DB_PATH)) {
            Statement stmt = connection.createStatement();
            stmt.executeUpdate(statement);
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
            Assertions.fail();
        }
    }

    @BeforeAll
    public static void setupTestDB() {
        testDBCommand("CREATE TABLE IF NOT EXISTS PRODUCT" +
                      "(ID INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                      " NAME           TEXT    NOT NULL, " +
                      " PRICE          INT     NOT NULL)");
    }

    @AfterAll
    public static void dropTable() {
        testDBCommand("DROP TABLE IF EXISTS PRODUCT");
    }

    @BeforeEach
    public void clearTestDB() {
        testDBCommand("DELETE FROM PRODUCT");
    }

    @BeforeEach
    public void setupMocks() {
        response = Mockito.mock(HttpServletResponse.class);
        request = Mockito.mock(HttpServletRequest.class);
    }

    @Test
    public void addProductServletTest() {
        List<String> list = new ArrayList<>();
        list.add("OK");
        Mockito.when(request.getParameter("name")).thenReturn("nameParameter");
        Mockito.when(request.getParameter("price")).thenReturn("100");
        checkResponseTest(list, () -> {
                    try {
                        new AddProductServlet().doGet(request, response);
                    } catch (IOException e) {
                        Assertions.fail("Exception: ", e);
                    }
                },
                (String str, StringWriter writer) -> writer.toString().contains(str));
    }

    public void checkResponseTest(List<String> assertions,
                                  Runnable servletDoGet,
                                  BiFunction<String, StringWriter, Boolean> checker) {
        try {
            StringWriter stringWriter = new StringWriter();
            PrintWriter writer = new PrintWriter(stringWriter);
            Mockito.when(response.getWriter()).thenReturn(writer);

            servletDoGet.run();

            System.out.println(stringWriter);

            assertions.forEach(str ->
                    Assertions.assertTrue(checker.apply(str, stringWriter))
            );

            Mockito.verify(response).setContentType("text/html");
            Mockito.verify(response).setStatus(HttpServletResponse.SC_OK);
        } catch (Exception e) {
            e.printStackTrace();
            Assertions.fail();
        }
    }

    @Test
    public void getProductsServletTest() {
        List<String> list = new ArrayList<>();
        list.add("<html><body>");
        list.add("</body></html>");
        Mockito.when(request.getParameter("name")).thenReturn("banana");
        Mockito.when(request.getParameter("price")).thenReturn("10");

        checkResponseTest(list, () -> {
                    try {
                        new GetProductsServlet().doGet(request, response);
                    } catch (IOException e) {
                        Assertions.fail("Exception: ", e);
                    }
                },
                (String str, StringWriter writer) -> writer.toString().contains(str));
    }

    public void testQuery(String command, String body) {
        List<String> list = new ArrayList<>();
        list.add("<html><body>" + System.lineSeparator()
                 + body + System.lineSeparator()
                 + "</body></html>" + System.lineSeparator());
        Mockito.when(request.getParameter("command")).thenReturn(command);
        checkResponseTest(list, () -> {
                    try {
                        new QueryServlet().doGet(request, response);
                    } catch (IOException e) {
                        Assertions.fail("Exception: ", e);
                    }
                },
                (String str, StringWriter writer) -> writer.toString().equals(str));
    }

    @Test
    public void testMin() {
        testQuery("min", "<h1>Product with min price: </h1>");
    }

    @Test
    public void testMax() {
        testQuery("max", "<h1>Product with max price: </h1>");
    }

    @Test
    public void testCount() {
        testQuery("count", "Number of products: " + System.lineSeparator() + "0");
    }

    @Test
    public void testSum() {
        testQuery("sum", "Summary price: " + System.lineSeparator() + "0");
    }


}
