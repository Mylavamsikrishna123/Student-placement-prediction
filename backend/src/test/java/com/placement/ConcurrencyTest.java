package com.placement;

import org.junit.jupiter.api.*;

import java.sql.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 4: Concurrency Tests
 * Tests multi-threaded scenarios and thread safety.
 * Uses H2 in-memory database with MySQL compatibility mode.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Concurrency Tests")
public class ConcurrencyTest {
    
    private static final String H2_URL = "jdbc:h2:mem:testdb_concurrency;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
    private Connection testConn;
    private ExecutorService executor;
    
    @BeforeAll
    public void setupDatabase() throws SQLException {
        testConn = DriverManager.getConnection(H2_URL, "sa", "");
        executor = Executors.newFixedThreadPool(4);
        
        try (Statement stmt = testConn.createStatement()) {
            stmt.execute("CREATE TABLE students (" +
                "student_id INT AUTO_INCREMENT PRIMARY KEY, " +
                "student_name VARCHAR(100) NOT NULL, " +
                "email VARCHAR(100) NOT NULL UNIQUE, " +
                "password_hash VARCHAR(60), " +
                "cgpa DECIMAL(4,2), " +
                "backlogs INT DEFAULT 0)");
            
            stmt.execute("CREATE TABLE login_sessions (" +
                "session_id INT AUTO_INCREMENT PRIMARY KEY, " +
                "student_id INT, " +
                "token VARCHAR(255), " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        }
    }
    
    @BeforeEach
    public void clearData() throws SQLException {
        try (Statement stmt = testConn.createStatement()) {
            stmt.execute("DELETE FROM login_sessions");
            stmt.execute("DELETE FROM students");
        }
    }
    
    @AfterAll
    public void teardownDatabase() throws SQLException {
        executor.shutdown();
        if (testConn != null && !testConn.isClosed()) {
            testConn.close();
        }
    }
    
    @Test
    @DisplayName("Concurrent user registrations do not cause conflicts")
    public void testConcurrentUserRegistrations() throws InterruptedException {
        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);
        
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    createStudent("student" + index + "@example.com", "Student " + index, "hash" + index, 5.0 + (index * 0.1));
                } catch (SQLException e) {
                    if (!e.getMessage().contains("UNIQUE")) {
                        e.printStackTrace();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        try {
            int count = countStudents();
            assertEquals(threadCount, count, "All concurrent registrations should succeed");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    @Test
    @DisplayName("Concurrent login attempts work correctly")
    public void testConcurrentLoginAttempts() throws InterruptedException {
        try {
            // Pre-create students
            for (int i = 0; i < 5; i++) {
                createStudent("student" + i + "@example.com", "Student " + i, "hash" + i, 5.0);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        int loginCount = 50; // 50 concurrent logins
        CountDownLatch latch = new CountDownLatch(loginCount);
        AtomicInteger successCount = new AtomicInteger(0);
        
        for (int i = 0; i < loginCount; i++) {
            final int index = i % 5;
            executor.submit(() -> {
                try {
                    boolean success = recordLogin("student" + index + "@example.com");
                    if (success) {
                        successCount.incrementAndGet();
                    }
                } catch (SQLException e) {
                    // Handle exception
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        assertEquals(loginCount, successCount.get(), "All concurrent logins should succeed");
    }
    
    @Test
    @DisplayName("Concurrent profile updates maintain consistency")
    public void testConcurrentProfileUpdates() throws SQLException, InterruptedException {
        createStudent("student@example.com", "Student", "hash", 5.0);
        
        int updateCount = 20;
        CountDownLatch latch = new CountDownLatch(updateCount);
        
        for (int i = 0; i < updateCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    updateStudentCGPA("student@example.com", 5.0 + (index * 0.1));
                    latch.countDown();
                } catch (SQLException e) {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        
        // Final value should be one of the updated values
        double finalCGPA = getStudentCGPA("student@example.com");
        assertTrue(finalCGPA >= 5.0 && finalCGPA < 7.0, 
            "Final CGPA should be within expected range after concurrent updates");
    }
    
    @Test
    @DisplayName("Concurrent database writes maintain integrity")
    public void testConcurrentDatabaseWrites() throws SQLException, InterruptedException {
        int writerCount = 5;
        int operationsPerWriter = 10;
        CountDownLatch latch = new CountDownLatch(writerCount * operationsPerWriter);
        
        for (int i = 0; i < writerCount; i++) {
            for (int j = 0; j < operationsPerWriter; j++) {
                final int writer = i;
                final int operation = j;
                executor.submit(() -> {
                    try {
                        createStudent("thread" + writer + "_op" + operation + "@example.com", 
                                     "User " + writer + "-" + operation, 
                                     "hash", 5.0 + (operation * 0.1));
                        latch.countDown();
                    } catch (SQLException e) {
                        latch.countDown();
                    }
                });
            }
        }
        
        latch.await();
        int count = countStudents();
        assertEquals(writerCount * operationsPerWriter, count, 
            "All concurrent write operations should succeed");
    }
    
    @Test
    @DisplayName("Deadlock prevention with multiple operations")
    public void testDeadlockPrevention() throws SQLException, InterruptedException {
        // Create initial data
        for (int i = 0; i < 3; i++) {
            createStudent("student" + i + "@example.com", "Student " + i, "hash", 5.0);
        }
        
        int operationCount = 10;
        CountDownLatch latch = new CountDownLatch(operationCount);
        
        for (int i = 0; i < operationCount; i++) {
            executor.submit(() -> {
                try {
                    // Simulate multiple operations (read, update, write)
                    String email = "student" + (int)(Math.random() * 3) + "@example.com";
                    double newCGPA = 5.0 + Math.random() * 3;
                    updateStudentCGPA(email, newCGPA);
                    latch.countDown();
                } catch (SQLException e) {
                    latch.countDown();
                }
            });
        }
        
        // Wait with timeout
        boolean completed = latch.await(10, TimeUnit.SECONDS);
        assertTrue(completed, "Operations should complete without deadlock");
    }
    
    @Test
    @DisplayName("Thread safety of database operations")
    public void testThreadSafety() throws SQLException, InterruptedException {
        AtomicInteger successCount = new AtomicInteger(0);
        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);
        
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    createStudent("safe" + index + "@example.com", "Safe User " + index, "hash", 6.0);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // Count failures
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        assertEquals(threadCount, successCount.get(), "All thread-safe operations should succeed");
    }
    
    @Test
    @DisplayName("Connection pool management under load")
    public void testConnectionPoolManagement() throws SQLException, InterruptedException {
        int highLoadCount = 50;
        CountDownLatch latch = new CountDownLatch(highLoadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        
        for (int i = 0; i < highLoadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    createStudent("load" + index + "@example.com", "Load Test " + index, "hash", 5.0);
                    successCount.incrementAndGet();
                } catch (SQLException e) {
                    // Connection pool management should handle this
                } finally {
                    latch.countDown();
                }
            });
        }
        
        boolean completed = latch.await(30, TimeUnit.SECONDS);
        assertTrue(completed, "All operations should complete within timeout");
        
        int finalCount = countStudents();
        assertTrue(finalCount > 0, "Some operations should succeed under load");
    }
    
    @Test
    @DisplayName("Load simulation with mixed operations")
    public void testLoadSimulation() throws SQLException, InterruptedException {
        // Pre-populate data
        for (int i = 0; i < 10; i++) {
            createStudent("base" + i + "@example.com", "Base User " + i, "hash", 5.0);
        }
        
        int operationCount = 100;
        CountDownLatch latch = new CountDownLatch(operationCount);
        AtomicInteger successCount = new AtomicInteger(0);
        
        for (int i = 0; i < operationCount; i++) {
            final int opIndex = i;
            executor.submit(() -> {
                try {
                    int operation = (int) (Math.random() * 3);
                    switch (operation) {
                        case 0: // Read
                            int count = countStudents();
                            if (count >= 10) successCount.incrementAndGet();
                            break;
                        case 1: // Update
                            updateStudentCGPA("base" + (int)(Math.random() * 10) + "@example.com", 6.0);
                            successCount.incrementAndGet();
                            break;
                        case 2: // Create
                            createStudent("load" + opIndex + "@example.com", "Load " + opIndex, "hash", 5.0);
                            successCount.incrementAndGet();
                            break;
                    }
                } catch (SQLException e) {
                    // Expected for some operations
                } finally {
                    latch.countDown();
                }
            });
        }
        
        boolean completed = latch.await(30, TimeUnit.SECONDS);
        assertTrue(completed, "Load simulation should complete");
        assertTrue(successCount.get() > 50, "Majority of operations should succeed");
    }
    
    // ===== Helper Methods =====
    
    private void createStudent(String email, String name, String passwordHash, double cgpa) throws SQLException {
        String sql = "INSERT INTO students (email, student_name, password_hash, cgpa) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = testConn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, name);
            ps.setString(3, passwordHash);
            ps.setDouble(4, cgpa);
            ps.executeUpdate();
        }
    }
    
    private void updateStudentCGPA(String email, double newCGPA) throws SQLException {
        String sql = "UPDATE students SET cgpa = ? WHERE email = ?";
        try (PreparedStatement ps = testConn.prepareStatement(sql)) {
            ps.setDouble(1, newCGPA);
            ps.setString(2, email);
            ps.executeUpdate();
        }
    }
    
    private boolean recordLogin(String email) throws SQLException {
        String sql = "INSERT INTO login_sessions (student_id, token) " +
                     "SELECT student_id, ? FROM students WHERE email = ?";
        try (PreparedStatement ps = testConn.prepareStatement(sql)) {
            ps.setString(1, java.util.UUID.randomUUID().toString());
            ps.setString(2, email);
            int result = ps.executeUpdate();
            return result > 0;
        }
    }
    
    private int countStudents() throws SQLException {
        String sql = "SELECT COUNT(*) FROM students";
        try (Statement stmt = testConn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }
    
    private double getStudentCGPA(String email) throws SQLException {
        String sql = "SELECT cgpa FROM students WHERE email = ?";
        try (PreparedStatement ps = testConn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("cgpa");
                }
            }
        }
        return -1;
    }
    
    /**
     * AtomicInteger for thread-safe operations
     */
    private static class AtomicInteger {
        private int value = 0;
        
        public AtomicInteger(int initialValue) {
            this.value = initialValue;
        }
        
        public synchronized void incrementAndGet() {
            value++;
        }
        
        public synchronized int get() {
            return value;
        }
    }
}
